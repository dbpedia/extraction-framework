package org.dbpedia.extraction.scripts

import java.io._
import java.net.{URI, URLEncoder}
import java.nio.charset.Charset
import java.security.InvalidParameterException
import java.text.SimpleDateFormat
import java.util.Date

import org.apache.http.client.utils.URLEncodedUtils
import org.apache.jena.atlas.json.{JSON, JsonObject}
import org.apache.jena.rdf.model._
import org.apache.jena.vocabulary.RDF
import org.apache.log4j.{Level, Logger}
import org.dbpedia.extraction.config.{Config, ExtractionLogger, ExtractionRecorder}
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, Dataset}
import org.dbpedia.extraction.util.{Language, OpenRdfUtils}
import org.dbpedia.iri.UriUtils
import org.openrdf.rio.RDFFormat

import scala.Console._
import scala.collection.JavaConverters._
import scala.io.Source
import scala.language.postfixOps
import scala.util.{Failure, Success}


/**
  * Created by Chile on 1/8/2016.
  * Creates all DataIds for each dataset.
  * Please run the CreateLinesBytesPacked script first
  */
object DataIdGenerator {

  private val logger = ExtractionLogger.getLogger(getClass, Language.None)

  //todo change external instances of lexvo to DBpedia+ worldfact ids
  private val dateformat = new SimpleDateFormat("yyyy-MM-dd")
  //statements
  private var stmtModel: Model = _
  private var agentModel: Model = _
  private var checksumModel: Model = _
  private var catalogModel: Model = _
  private var staticModel: Model = _
  private var relationModel: Model = _
  private var latestDataId: Option[(Model, String)] = None
  private var previousDataId: Option[(Model, String)] = None
  private var nextDataId: Option[(Model, String)] = None
  private var versionStatement: Resource = _
  private var rightsStatement: Resource = _
  private var dataidStandard: Resource = _
  private var dataidLdStandard: Resource = _

  private var catalogInUse: Resource = _
  private var currentRootSet: Resource = _
  private var currentDataIdUri: Resource = _
  private var currentDataid: Model = _

  private var configMap: JsonObject = _

  private var mediaTypeMap: Map[(String, String), Resource] = _
  private var lbpMap: Map[String, Map[String, String]] = _
  private var webDir: String = _
  private var dump: File = _

  private var documentation: String = _
  private var compression: List[String] = _
  private var extensions: List[String] = _
  private var vocabulary: String = _
  private var idVersion: String = _
  private var dbpVersion: String = _
  private var preamble: String = _
  private var rights: String = _
  private var license: String = _
  private var sparqlEndpoint: String = _
  private var coreList: List[String] = List()

  private var langList: List[Language] = _

  private var releaseDate: Date = _

  private val dumpFile = "^[a-zA-Z0-9-_]+".r

  def extractDataID(outer: File, dir: File): Unit =
  {
    val innerPath = if(dir.getName == "core") "" else outer.getName+"/"
    val lang = Language.get(dir.getName.replace("_", "-")) match {
      case Some(l) if this.langList.contains(l) => l
      case _ =>
        logger.log(Level.INFO, "no allowed language found for: " + dir.getName)
        Language.None
    }
    //reading in the lines-bytes-packed.csv file of this directory
    val lbp = Option(try {
      Source.fromFile(new File(dir, "lines-bytes-packed.csv"))
    } catch {
      case fnf: FileNotFoundException => null
    })
    lbpMap = lbp match {
      case Some(ld) => ld.getLines.map(_.split(";")).map(x => if(x.length > 4)
        x.head -> Map("lines" -> x(1), "bytes" -> x(2), "bz2" -> x(3), "md5" -> x(4))
      else if (x.length == 4)
        x.head -> Map("lines" -> x(1), "bytes" -> x(2), "bz2" -> x(3))
      else
        throw new InvalidParameterException("Lines-bytes-packed.csv file is not in an expected format, in directory: " + dir)
      ).toMap
      case None =>
        logger.log(Level.INFO, "No lines-bytes-packed.csv file found for directory " + dir)
        return
    }

    if(dir.getName == "core")
      coreList = lbpMap.keySet.map(x => x.replace("core/", "")).toList

    currentDataid = ModelFactory.createDefaultModel()
    val topsetModel = ModelFactory.createDefaultModel()
    agentModel = ModelFactory.createDefaultModel()
    val mainModel = ModelFactory.createDefaultModel()
    relationModel = ModelFactory.createDefaultModel()
    stmtModel = ModelFactory.createDefaultModel()
    checksumModel = ModelFactory.createDefaultModel()

    addPrefixes(currentDataid)
    addPrefixes(topsetModel)
    addPrefixes(mainModel)
    addPrefixes(agentModel)
    addPrefixes(stmtModel)
    addPrefixes(checksumModel)
    addPrefixes(relationModel)

    val ttlOutFile = new File(dir.getAbsolutePath.replace("\\", "/") + "/" + configMap.get("outputFileTemplate").getAsString.value + "_" + lang.wikiCode.replace("-", "_") + ".ttl")
    val jldOutFile = new File(dir.getAbsolutePath.replace("\\", "/") + "/" + configMap.get("outputFileTemplate").getAsString.value + "_" + lang.wikiCode.replace("-", "_") + ".json")
    logger.log(Level.INFO, "started DataId: " + ttlOutFile.getAbsolutePath)

    currentDataIdUri = currentDataid.createResource(webDir + innerPath + lang.wikiCode.replace("-", "_") + "/" + configMap.get("outputFileTemplate").getAsString.value + "_" + lang.wikiCode.replace("-", "_") + ".ttl")
    require(currentDataIdUri != null, "Please provide a valid directory")
    currentDataid.add(currentDataIdUri, RDF.`type`, currentDataid.createResource(currentDataid.getNsPrefixURI("dataid") + "DataId"))

    // add prev/next/latest statements
    addVersionPointers(currentDataid, currentDataIdUri)

    //statements
    versionStatement = addSimpleStatement(stmtModel, currentDataIdUri, "version", idVersion, idVersion)
    rightsStatement = addSimpleStatement(stmtModel, currentDataIdUri, "rights", "dbpedia-rights", rights, Language.English)
    dataidStandard = addSimpleStatement(stmtModel, currentDataIdUri, null, null, "DataID - dataset metadata ontology", Language.English, staticModel.createResource("http://dataid.dbpedia.org/ns/core"))
    dataidLdStandard = addSimpleStatement(stmtModel, currentDataIdUri, null, null, "DataID-LD - dataset metadata ontology with linked data extension", Language.English, staticModel.createResource("http://dataid.dbpedia.org/ns/ld"))

    val creator = addAgent(agentModel, currentDataIdUri, currentDataid, configMap.get("creator").getAsObject)
    val maintainer = addAgent(agentModel, currentDataIdUri, currentDataid, configMap.get("maintainer").getAsObject)
    val contact = addAgent(agentModel, currentDataIdUri, currentDataid, configMap.get("contact").getAsObject)
    require(creator != null, "Please define an dataid:Agent as a Creator in the dataid stump file (use dataid:Authorization).")

    currentDataid.add(currentDataIdUri, getProperty("dc", "modified"), createLiteral(dateformat.format(new Date()), "xsd", "date"))
    currentDataid.add(currentDataIdUri, getProperty("dc", "issued"), createLiteral(dateformat.format(releaseDate), "xsd", "date"))
    currentDataid.add(currentDataIdUri, getProperty("dataid", "associatedAgent"), creator)
    currentDataid.add(currentDataIdUri, getProperty("dataid", "associatedAgent"), maintainer)
    currentDataid.add(currentDataIdUri, getProperty("dataid", "associatedAgent"), contact)
    currentDataid.add(currentDataIdUri, getProperty("dataid", "inCatalog"), catalogInUse)
    currentDataid.add(currentDataIdUri, getProperty("dc", "title"), createLiteral("DataID metadata for the " + lang.name + " DBpedia", "en"))
    currentDataid.add(currentDataIdUri, getProperty("dc", "conformsTo"), dataidStandard)
    currentDataid.add(currentDataIdUri, getProperty("dc", "conformsTo"), dataidLdStandard)
    currentDataid.add(currentDataIdUri, getProperty("dc", "publisher"), creator)
    currentDataid.add(currentDataIdUri, getProperty("dc", "hasVersion"), versionStatement)
    catalogModel.add(catalogInUse, getProperty("dcat", "record"), currentDataIdUri)

    currentRootSet = addDataset(topsetModel, lang, DBpediaDatasets.MainDataset.getLanguageVersion(lang, this.dbpVersion).get, creator, null, superset = true)
    currentDataid.add(currentDataIdUri, getProperty("foaf", "primaryTopic"), currentRootSet)
    topsetModel.add(currentRootSet, getProperty("foaf", "isPrimaryTopicOf"), currentDataIdUri)
    topsetModel.add(currentRootSet, getProperty("void", "vocabulary"), topsetModel.createResource(vocabulary))
    topsetModel.add(currentRootSet, getProperty("void", "vocabulary"), topsetModel.createResource(vocabulary.replace(".owl", ".nt")))
    topsetModel.add(currentRootSet, getProperty("dc", "description"), createLiteral(configMap.get("description").getAsString.value, "en"))
    topsetModel.add(currentRootSet, getProperty("dc", "title"), createLiteral("DBpedia root dataset for " + lang.name + ", version " + dbpVersion, "en"))

    if (rights != null)
      topsetModel.add(currentRootSet, getProperty("dc", "rights"), rightsStatement)

    if (configMap.get("addDmpProps").getAsBoolean.value())
      addDmpStatements(topsetModel, currentRootSet)

    var lastFile: String = null
    var dataset: Resource = null
    var pagesArticles: Dataset = null

    //create datasets and distributions
    for (dis <- lbpMap.keys.toList.sorted) {
      if (lastFile != dis.split("\\.").head) {  //only add dataset once for all its distributions
        dataset = addDataset(mainModel, lang, dis, creator)
        if(dataset != null) {
          if (dis.contains("pages_articles"))
            pagesArticles = DBpediaDatasets.getDataset(dataset.getURI, lang, this.dbpVersion)
              .getOrElse(throw new IllegalArgumentException("A dataset named " + dataset.getURI + " is unknown."))
          else {
            topsetModel.add(currentRootSet, getProperty("void", "subset"), dataset)
            mainModel.add(dataset, getProperty("dc", "isPartOf"), currentRootSet)
          }
        }
      }
      lastFile = dis.split("\\.").head
      if(dataset != null) {
        if (coreList.contains(dis.substring(dis.lastIndexOf("/") + 1))) {
          mainModel.add(addSparqlEndpoint(dataset))
          mainModel.add(dataset, getProperty("void", "sparqlEndpoint"), mainModel.createResource(sparqlEndpoint))
        }
        addDistribution(mainModel, dataset, lang, dis, creator)
      }
    }
    //run through all dataset again to add dataset relations
    if(pagesArticles != null) {
      val pagesArticlesResource = mainModel.createResource(pagesArticles.versionUri.toString)
      for (dis <- lbpMap.keys.toList.sorted) {
        if (dis.contains("_" + dir.getName)) {
          if (lastFile != dis.substring(0, dis.lastIndexOf("_" + dir.getName))) {
            //only add dataset once for all its distributions
            lastFile = dis.substring(0, dis.lastIndexOf("_" + dir.getName))
            Option(getDataset(dis, lang)) match {
              case Some(d) =>
                val dResource = mainModel.createResource(d.versionUri.toString)
                val relation = mainModel.createResource(d.getRelationUri("source", pagesArticles))
                mainModel.add(dResource, getProperty("dataid", "relatedDataset"), pagesArticlesResource)
                mainModel.add(dResource, getProperty("dataid", "qualifiedDatasetRelation"), relation)

                relationModel.add(relation, RDF.`type`, currentDataid.createResource(currentDataid.getNsPrefixURI("dataid") + "DatasetRelationship"))
                relationModel.add(relation, getProperty("dataid", "qualifiedRelationOf"), dResource)
                relationModel.add(relation, getProperty("dataid", "qualifiedRelationTo"), pagesArticlesResource)
                relationModel.add(relation, getProperty("dataid", "datasetRelationRole"), currentDataid.createResource(currentDataid.getNsPrefixURI("dataid") + "SourceRole"))
              case None =>
            }
          }
        }
      }
    }

      //TODO validate & publish DataIds online!!!

      def writeModel(model:Model, headline: String) : String = {
        val baos = new ByteArrayOutputStream()
        currentDataid.add(model)
        model.write(baos, "TURTLE")
        val zw = Option(headline) match{
          case Some(h) => "\n########### " + h + " ###########\n"
          case None => ""
        }
        zw + new Predef.String(baos.toByteArray, Charset.defaultCharset())
      }

      var os = new FileOutputStream(ttlOutFile)
      var printStream = new PrintStream(os)

      Option(preamble) match{
        case Some(p) => if(p.trim.length > 0) printStream.print("# " + p.replaceAll("\\$version", dbpVersion).replaceAll("\\$language", lang.name).replaceAll("\\$landingpage", documentation).replaceAll("\\n", "\n# ") + "\n\n")
        case None =>
      }
      printStream.print(writeModel(currentDataid, null))
      currentDataid.add(agentModel)
      printStream.print(writeModel(agentModel, "Agents & Authorizations").replaceAll("(@prefix).*\\n", ""))
      currentDataid.add(topsetModel)
      printStream.print(writeModel(topsetModel, "Main Dataset").replaceAll("(@prefix).*\\n", ""))
      currentDataid.add(mainModel)
      printStream.print(writeModel(mainModel, "Datasets & Distributions").replaceAll("(@prefix).*\\n", ""))
      currentDataid.add(relationModel)
      printStream.print(writeModel(relationModel, "Relations").replaceAll("(@prefix).*\\n", ""))
      currentDataid.add(checksumModel)
      printStream.print(writeModel(checksumModel, "Checksums").replaceAll("(@prefix).*\\n", ""))
      currentDataid.add(stmtModel)
      printStream.print(writeModel(stmtModel, "Statements").replaceAll("(@prefix).*\\n", ""))
      currentDataid.add(staticModel)
      printStream.print(writeModel(staticModel, "MediaTypes").replaceAll("(@prefix).*\\n", ""))

      printStream.close()

      val jsonStr = OpenRdfUtils.writeSerialization(OpenRdfUtils.convertToOpenRdfModel(currentDataid), RDFFormat.JSONLD)
      os = new FileOutputStream(jldOutFile)
      printStream = new PrintStream(os)
      printStream.print(jsonStr)
      printStream.close()

      logger.log(Level.INFO, "finished DataId: " + ttlOutFile.getAbsolutePath)
  }

  def addPrefixes(model: Model): Unit = {
    val prefixMap = configMap.get("prefixMap").getAsObject
    for(prefix <- prefixMap.keys().asScala){
      model.setNsPrefix(prefix, prefixMap.get(prefix).getAsString.value())
    }
  }

  def addAgent(agentModel: Model, parentResource: Resource, parentModel: Model, agentMap: JsonObject, specialEntities: List[Resource] = List()): Resource = {
    val agent = agentModel.createResource(agentMap.get("uri").getAsString.value())
    agentModel.add(agent, RDF.`type`, agentModel.createResource(agentModel.getNsPrefixURI("dataid") + "Agent"))
    agentModel.add(agent, getProperty("foaf", "name"), createLiteral(agentMap.get("name").getAsString.value()))
    if (agentMap.get("homepage") != null)
      agentModel.add(agent, getProperty("foaf", "homepage"), agentModel.createResource(agentMap.get("homepage").getAsString.value()))
    agentModel.add(agent, getProperty("foaf", "mbox"), createLiteral(agentMap.get("mbox").getAsString.value()))


    val context = agentModel.createResource(parentResource.getURI + (if(parentResource.getURI.contains("?")) "&" else "?") + "auth=" + agentMap.get("role").getAsString.value().toLowerCase + "Authorization")
    agentModel.add(context, RDF.`type`, agentModel.createResource(agentModel.getNsPrefixURI("dataid") + "Authorization"))
    agentModel.add(context, getProperty("dataid", "authorizedAgent"), agent)
    agentModel.add(agent, getProperty("dataid", "hasAuthorization"), context)
    agentModel.add(context, getProperty("dataid", "authorityAgentRole"), agentModel.createResource(agentModel.getNsPrefixURI("dataid") + agentMap.get("role").getAsString.value()))
    agentModel.add(context, getProperty("dataid", "isInheritable"), createLiteral("true", "xsd", "boolean"))

    //distribute authorizations
    if (specialEntities.isEmpty) //no special entities -> we assume its valid for whole DataID
    {
      agentModel.add(context, getProperty("dataid", "authorizedFor"), parentResource)
      parentModel.add(parentResource, getProperty("dataid", "underAuthorization"), context)
    }
    else{
      for (ent <- specialEntities) //with special entities -> we assume they need dataid:needsSpecialAuthorization
      {
        agentModel.add(context, getProperty("dataid", "authorizedFor"), ent)
        parentModel.add(ent, getProperty("dataid", "needsSpecialAuthorization"), context)
      }
    }

    //add identifier
    if (agentMap.get("identifier") != null) {
      val idMap = agentMap.get("identifier").getAsObject
      val id = agentModel.createResource(idMap.get("url").getAsString.value())
      agentModel.add(agent, getProperty("dataid", "identifier"), id)
      if (idMap.get("literal") != null) {
        agentModel.add(id, RDF.`type`, agentModel.createResource(agentModel.getNsPrefixURI("dataid") + "Identifier"))
        agentModel.add(id, getProperty("dataid", "literal"), createLiteral(idMap.get("literal").getAsString.value()))
        agentModel.add(id, getProperty("dc", "references"), id)
        agentModel.add(id, getProperty("datacite", "usesIdentifierScheme"), agentModel.createResource(idMap.get("scheme").getAsString.value()))
        if (idMap.get("issued") != null)
          agentModel.add(id, getProperty("dc", "issued"), createLiteral(dateformat.format(dateformat.parse(idMap.get("issued").getAsString.value())), "xsd", "date"))
      }
    }
    agent
  }

  def getMediaType(outer: String, inner: String): Resource = {
    mediaTypeMap.get((outer, inner)) match{
      case None =>
        val o = Option(outer match {
          case y if y.contains("gz") => "application/x-gzip"
          case z if z.contains("bz2") => "application/x-bzip2"
          case "sparql" => "application/sparql-results+xml"
          case _ =>
            logger.log(Level.FATAL, "outer MediaType could not be determined: " + outer)
            null
        })
        val oe = Option(outer match {
          case y if y.contains("gz") => ".gz"
          case z if z.contains("bz2") => ".bz2"
          case _ =>
            logger.log(Level.WARN, "outer file extension could not be determined: " + outer)
            null
        })
        val i = Option(inner match {
          case ttl if ttl.contains("ttl") => "text/turtle"
          case tql if tql.contains("tql") || tql.contains(".nq") => "application/n-quads"
          case nt if nt.contains("nt") => "application/n-triples"
          case xml if xml.contains("xml") => "application/xml"
          case _ =>
            logger.log(Level.WARN, "inner MediaType could not be determined: " + inner)
            null
        })
        val ie = Option(inner match {
          case ttl if ttl.contains("ttl") => ".ttl"
          case tql if tql.contains("tql") || tql.contains(".nq") => ".tql"
          case nt if nt.contains("nt") => ".nt"
          case xml if xml.contains("xml") => "application/xml"
          case _ =>
            logger.log(Level.WARN, "inner file extension could not be determined: " + inner)
            null
        })

        //this is the outer mime type (don't be confused by the inner match!
        val mime = i match{
          case Some(in) => staticModel.createResource(staticModel.getNsPrefixURI("dataid-mt") + "MediaType_" + in.substring(in.lastIndexOf("/") + 1) + "_" + o.get.substring(o.get.lastIndexOf("/") + 1))
          case None => staticModel.createResource(staticModel.getNsPrefixURI("dataid-mt") + "MediaType_" + o.get.substring(o.get.lastIndexOf("/") + 1))
        }

        staticModel.add(mime, RDF.`type`, staticModel.createResource(staticModel.getNsPrefixURI("dataid") + "MediaType"))
        staticModel.add(mime, getProperty("dataid", "typeTemplate"), createLiteral(o.get))
        staticModel.add(mime, getProperty("dc", "conformsTo"), dataidStandard)
        oe match{
          case Some(ooe) => staticModel.add(mime, getProperty("dataid", "typeExtension"), createLiteral(ooe))
          case None =>
        }

        //this is the inner mime type
        i match{
          case Some(ii) =>
            val it = staticModel.createResource(staticModel.getNsPrefixURI("dataid") + "MediaType_" + ii.substring(ii.lastIndexOf("/") + 1))
            staticModel.add(it, RDF.`type`, staticModel.createResource(staticModel.getNsPrefixURI("dataid") + "MediaType"))
            staticModel.add(mime, getProperty("dataid", "innerMediaType"), it)
            staticModel.add(it, getProperty("dataid", "typeTemplate"), createLiteral(ii))
            staticModel.add(it, getProperty("dc", "conformsTo"), dataidStandard)
            ie match {
              case Some(iie) =>
                if (iie == ".tql")
                  staticModel.add(it, getProperty("dataid", "typeExtension"), createLiteral(".nq"))
                else
                  staticModel.add(it, getProperty("dataid", "typeExtension"), createLiteral(iie))
              case None =>
            }
            mediaTypeMap += (inner, null) -> it
          case None =>
        }
        mediaTypeMap += (outer, inner) -> mime
        mime
      case Some(m) => m
    }
  }

  def addSimpleStatement(model:Model, subject: Resource, typ: String, uriVal: String, stmt: String, lang: Language = null, ref: Resource = null): Resource = {
    val ss = if (ref != null && ref.isURIResource)
      model.createResource(ref.getURI + (if (uriVal != null) "#" + typ + "=" + URLEncoder.encode(uriVal, "UTF-8") else ""))
    else
      model.createResource(subject.getURI + (if (uriVal != null) "?" + typ + "=" + URLEncoder.encode(uriVal, "UTF-8") else ""))
    model.add(ss, RDF.`type`, model.createResource(model.getNsPrefixURI("dataid") + "SimpleStatement"))
    if (lang != null)
      model.add(ss, getProperty("dataid", "statement"), createLiteral(stmt, lang.isoCode))
    else
      model.add(ss, getProperty("dataid", "statement"), createLiteral(stmt))
    if (ref != null)
      model.add(ss, getProperty("dc", "references"), ref)
    ss
  }

  def addSparqlEndpoint(dataset: Resource): Model = {
    val sparql: Model = ModelFactory.createDefaultModel()
    addPrefixes(sparql)
    val dist = sparql.createResource(currentDataIdUri.getURI + "?sparql=DBpediaSparqlEndpoint")
    val sparqlAgent = addAgent(agentModel, dist, sparql, configMap.get("openLink").getAsObject )
    sparql.add(dist, RDF.`type`, sparql.createResource(sparql.getNsPrefixURI("dataid-ld") + "SparqlEndpoint"))
    sparql.add(dataset, getProperty("dcat", "distribution"), dist)
    sparql.add(dist, getProperty("dataid", "isDistributionOf"), dataset)
    sparql.add(dist, getProperty("dc", "hasVersion"), versionStatement)
    sparql.add(dist, getProperty("dc", "title"), createLiteral("The official DBpedia sparql endpoint", "en"))
    sparql.add(dist, getProperty("dc", "description"), createLiteral("The official sparql endpoint of DBpedia, hosted graciously by OpenLink Software (http://virtuoso.openlinksw.com/), containing all datasets of the /core directory.", "en"))
    sparql.add(dist, getProperty("rdfs", "label"), createLiteral("The official DBpedia sparql endpoint", "en"))
    sparql.add(dist, getProperty("dataid", "associatedAgent"), sparqlAgent)
    sparql.add(dist, getProperty("dc", "modified"), createLiteral(dateformat.format(new Date()), "xsd", "date"))
    sparql.add(dist, getProperty("dc", "issued"), createLiteral(dateformat.format(releaseDate), "xsd", "date"))
    sparql.add(dist, getProperty("dc", "license"), sparql.createResource(license))
    sparql.add(dist, getProperty("dcat", "mediaType"), getMediaType("sparql", ""))
    sparql.add(dist, getProperty("dcat", "accessURL"), sparql.createResource(sparqlEndpoint))
    sparql.add(dist, getProperty("dataid", "accessProcedure"), addSimpleStatement(stmtModel, currentDataIdUri, "stmt", "sparqlaccproc", "An endpoint for sparql queries: provide valid queries."))
    sparql.add(dist, getProperty("dc", "conformsTo"), dataidStandard)
    sparql.add(dist, getProperty("dc", "conformsTo"), dataidLdStandard)
    sparql
  }

  def addDmpStatements(model: Model, dataset: Resource): Unit = {
    model.add(dataset, getProperty("dataid", "usefulness"), addSimpleStatement(stmtModel, currentDataIdUri, "stmt", "usefulness", configMap.get("dmpusefulness").getAsString.value, Language.English))
    model.add(dataset, getProperty("dataid", "similarData"), addSimpleStatement(stmtModel, currentDataIdUri, "stmt", "similarData", configMap.get("dmpsimilarData").getAsString.value, Language.English))
    model.add(dataset, getProperty("dataid", "reuseAndIntegration"), addSimpleStatement(stmtModel, currentDataIdUri, "stmt", "reuseAndIntegration", configMap.get("dmpreuseAndIntegration").getAsString.value, Language.English))
    //TODO put that to distributions... model.add(dataset, getProperty("dataid", "softwareRequirement"), addSimpleStatement("stmt", "softwareRequirement", configMap.get("dmpadditionalSoftware").getAsString.value, Language.English))
    //TODO model.add(dataset, getProperty("dmp", "repositoryUrl"), model.createResource(configMap.get("dmprepositoryUrl").getAsString.value))
    model.add(dataset, getProperty("dataid", "growth"), addSimpleStatement(stmtModel, currentDataIdUri, "stmt", "growth", configMap.get("dmpgrowth").getAsString.value, Language.English))
    //TODO model.add(dataset, getProperty("dmp", "archiveLink"), model.createResource(configMap.get("dmparchiveLink").getAsString.value))
    //TODO model.add(dataset, getProperty("dmp", "preservation"), createLiteral(configMap.get("dmppreservation").getAsString.value, "en"))
    model.add(dataset, getProperty("dataid", "openness"), addSimpleStatement(stmtModel, currentDataIdUri, "stmt", "openness", configMap.get("dmpopenness").getAsString.value, Language.English))
  }

  def getDBpediaDataset(fileName: String, lang: Language, dbpv: String): Option[Dataset] ={

    def internalGet(name:String, ext: String): Option[Dataset] = scala.util.Try {
      DBpediaDatasets.getDataset(name + (if(ext != null) ext else ""), lang, dbpv).getOrElse(
        throw new IllegalArgumentException("A dataset named " + name + (if(ext != null) ext else "") + " is unknown.")
      )
    } match{
      case Success(s) => Some(s)
      case Failure(e) => None
    }
    internalGet(fileName, null) match{
      case Some(d) => return Some(d)
      case None =>
    }
    val splits = fileName.split("_")
    for(i <- (1 until splits.length).reverse)
    {
      val name = splits.slice(0, i).foldLeft("")(_ + "_" + _).substring(1)
      val ext = splits.slice(i, splits.length).foldLeft("")(_ + "_" + _)
      internalGet(name, ext) match{
        case Some(d) => return d.getLanguageVersion(lang, dbpv).toOption
        case None =>
      }
    }
    None
  }

  def getDataset(fileName: String, lang:Language): Dataset = {
    val actualLang = if(lang == Language.Core || lang == Language.None) {
      val i = fileName.lastIndexOf("_")+1
      if(i > 0)
        Language.get(fileName.substring(i, fileName.indexOf("."))).orNull
      else
        null
    }
    else lang

    if(actualLang == null)
      return null

    val datasetName = if(fileName.contains("_" + actualLang.wikiCode + "."))
      fileName.substring(fileName.lastIndexOf("/")+1, fileName.lastIndexOf("_" + actualLang.wikiCode + "."))
    else
      fileName.substring(fileName.lastIndexOf("/")+1).replaceAll("\\.\\w+", "")

    getDBpediaDataset(datasetName, actualLang, this.dbpVersion) match{
      case Some(d) => d
      case None =>
        err.println("Dataset " + datasetName + " was not found! Please edit the DBpediaDatasets.scala file to include it.")
        null
    }
  }

  def addDataset(model: Model, lang: Language, currentFile: String, associatedAgent: Resource, superset: Boolean = false): Resource = {
    val dataset = Option(getDataset(currentFile, lang)) match {
      case Some(d) => d
      case None => return null
    }
    val map = lbpMap.getOrElse(currentFile, null)
    addDataset(model, lang, dataset, associatedAgent, map, superset)
  }

  def addDataset(model: Model, lang: Language, dataset: Dataset, associatedAgent: Resource, lbpMap: Map[String, String], superset: Boolean): Resource = {
    val datasetUri = model.createResource(dataset.versionUri.toString)
    //add dataset to catalog
    catalogModel.add(catalogInUse, getProperty("dcat", "dataset"), datasetUri)
    if(superset) {
      model.add(datasetUri, RDF.`type`, model.createResource(model.getNsPrefixURI("dataid") + "Superset"))
      model.add(datasetUri, RDF.`type`, model.createResource(model.getNsPrefixURI("dataid") + "Dataset"))
    }
    else if(dataset.encoded.contains("pages_articles")){
      model.add(datasetUri, RDF.`type`, model.createResource(model.getNsPrefixURI("dataid") + "Dataset"))
    }
    else {
      model.add(datasetUri, RDF.`type`, model.createResource(model.getNsPrefixURI("dataid") + "Dataset"))
      model.add(datasetUri, RDF.`type`, model.createResource(model.getNsPrefixURI("dataid-ld") + "LinkedDataDataset"))
    }

    // add prev/next/latest statements
    addVersionPointers(model, datasetUri)
    model.add(datasetUri, getProperty("rdfs", "label"), createLiteral(dataset.name.replace("-", " ").replace("_", " "), "en"))

    if (!superset) //not!
    {
      model.add(datasetUri, getProperty("dc", "title"), createLiteral(dataset.name.replace("-", " ").replace("_", " "), "en"))
      dataset.description match{
        case Some(desc) => model.add(datasetUri, getProperty("dc", "description"), createLiteral(desc, "en"))
        case None =>
          model.add(datasetUri, getProperty("dc", "description"), createLiteral("DBpedia dataset " + dataset.encoded + ", subset of " + currentRootSet.getLocalName, "en"))
          err.println("Could not find description for dataset: " + lang.wikiCode.replace("-", "_") + "/" + dataset.encoded)
      }
      if(dataset.isLinkedDataDataset) {
        model.add(datasetUri, getProperty("void", "rootResource"), currentRootSet)
        dataset.defaultGraph match {
          case Some(dg) => model.add(datasetUri, getProperty("sd", "defaultGraph"), model.createResource(dg))
          case None =>
        }
      }
    }
    if (dataset.isLinkedDataDataset)
    {
      model.add(datasetUri, getProperty("dcat", "landingPage"), model.createResource("http://dbpedia.org/"))
      model.add(datasetUri, getProperty("foaf", "page"), model.createResource(documentation))
      //TODO done by DataId Hub
      model.add(datasetUri, getProperty("dc", "hasVersion"), versionStatement)
      model.add(datasetUri, getProperty("dataid", "associatedAgent"), associatedAgent)
      model.add(datasetUri, getProperty("dc", "modified"), createLiteral(dateformat.format(new Date()), "xsd", "date"))
      model.add(datasetUri, getProperty("dc", "issued"), createLiteral(dateformat.format(releaseDate), "xsd", "date"))
      model.add(datasetUri, getProperty("dc", "license"), model.createResource(license))
      model.add(datasetUri, getProperty("dc", "publisher"), associatedAgent)
      model.add(datasetUri, getProperty("dcat", "keyword"), createLiteral("DBpedia", "en"))
      model.add(datasetUri, getProperty("dcat", "keyword"), createLiteral(dataset.name, "en"))
      if(lbpMap != null)
        model.add(datasetUri, getProperty("void", "triples"), createLiteral((new Integer(lbpMap("lines")) - 2).toString, "xsd", "integer"))
      for(key <- dataset.keywords)
        model.add(datasetUri, getProperty("dcat", "keyword"), createLiteral(key, "en"))
      model.add(datasetUri, getProperty("dc", "conformsTo"), dataidStandard)
      model.add(datasetUri, getProperty("dc", "conformsTo"), dataidLdStandard)
    }
    else if(dataset.encoded.contains("pages_articles")) { //is wikipedia dump dataset
      getWikipediaDownloadInfo(lang) match {
        case Some(dlInfo) =>
          val rDate = dlInfo._1.trim.substring(0, 4) + "-" + dlInfo._1.trim.substring(4, 6) + "-" + dlInfo._1.trim.substring(6)
          model.add(datasetUri, getProperty("dc", "hasVersion"), dlInfo._1.trim)
          model.add(datasetUri, getProperty("dc", "issued"), createLiteral(rDate, "xsd", "date"))
        case None =>
      }
      val wikimedia = addAgent(agentModel, datasetUri, model, configMap.get("wikimedia").getAsObject)
      model.add(datasetUri, getProperty("dc", "license"), model.createResource(license))
      model.add(datasetUri, getProperty("dc", "publisher"), wikimedia)
      model.add(datasetUri, getProperty("dataid", "associatedAgent"), wikimedia)
      model.add(datasetUri, getProperty("dcat", "keyword"), createLiteral("Wikipedia", "en"))
      model.add(datasetUri, getProperty("dcat", "keyword"), createLiteral("XML dump file", "en"))
      model.add(datasetUri, getProperty("dcat", "landingPage"), model.createResource("https://meta.wikimedia.org/wiki/Data_dumps"))
    }

    if (lang.iso639_3 != null && lang.iso639_3.length > 0)
      model.add(datasetUri, getProperty("dc", "language"), model.createResource("http://lexvo.org/id/iso639-3/" + lang.iso639_3))

    datasetUri
  }

  def addDistribution(model: Model, datasetUri: Resource, lang: Language, currentFile: String, associatedAgent: Resource): Resource = {
    val dataset = getDataset(currentFile, lang)
    val dist = model.createResource(dataset.getDistributionUri("file", currentFile.substring(currentFile.indexOf('.'))))
    model.add(dist, RDF.`type`, model.createResource(model.getNsPrefixURI("dataid") + "SingleFile"))
    model.add(datasetUri, getProperty("dcat", "distribution"), dist)
    model.add(dist, getProperty("dataid", "isDistributionOf"), datasetUri)

    model.add(dist, getProperty("dc", "title"), createLiteral(dataset.name.replace("-", " ").replace("_", " "), "en"))
    model.add(dist, getProperty("rdfs", "label"), createLiteral(dataset.name.replace("-", " ").replace("_", " "), "en"))
    dataset.description match{
      case Some(desc) => model.add(dist, getProperty("dc", "description"), createLiteral(desc, "en"))
      case None =>
        model.add(dist, getProperty("dc", "description"), createLiteral("DBpedia dataset " + dataset.encoded + ", subset of " + currentRootSet.getLocalName, "en"))
        err.println("Could not find description for distribution: " + lang.wikiCode.replace("-", "_") + "/" + currentFile)
    }

    // add prev/next/latest statements
    addVersionPointers(model, dist)

    if(currentFile.contains("pages_articles"))  //is Wikipedia file
    {
      val wikimedia = addAgent(agentModel, dist, model, configMap.get("wikimedia").getAsObject)
      getWikipediaDownloadInfo(lang) match {
        case Some(dlInfo) =>
          val rDate = dlInfo._1.trim.substring(0, 4) + "-" + dlInfo._1.trim.substring(4, 6) + "-" + dlInfo._1.trim.substring(6)
          model.add(dist, getProperty("dc", "hasVersion"), dlInfo._1.trim)
          model.add(dist, getProperty("dc", "issued"), createLiteral(rDate, "xsd", "date"))
          model.add(dist, getProperty("dcat", "downloadURL"), model.createResource(dlInfo._2.trim))
        case None =>
      }
      model.add(dist, getProperty("dc", "publisher"), wikimedia)
      model.add(dist, getProperty("dataid", "associatedAgent"), wikimedia)
      model.add(dist, getProperty("rdfs", "label"), createLiteral(currentFile))
      model.add(dist, getProperty("dc", "license"), model.createResource(license))
    }
    else {
      model.add(dist, getProperty("rdfs", "label"), createLiteral(currentFile))
      //TODO done by DataId Hub
      model.add(dist, getProperty("dc", "hasVersion"), versionStatement)
      model.add(dist, getProperty("dataid", "associatedAgent"), associatedAgent)
      model.add(dist, getProperty("dc", "publisher"), associatedAgent)
      model.add(dist, getProperty("dc", "modified"), createLiteral(dateformat.format(new Date()), "xsd", "date"))
      model.add(dist, getProperty("dc", "issued"), createLiteral(dateformat.format(releaseDate), "xsd", "date"))
      model.add(dist, getProperty("dc", "license"), model.createResource(license))
      model.add(dist, getProperty("dc", "conformsTo"), dataidStandard)
    }

    lbpMap.get(currentFile) match {
      case Some(bytes) =>
        model.add(dist, getProperty("dcat", "byteSize"), createLiteral(bytes("bz2"), "xsd", "integer"))
        model.add(dist, getProperty("dataid", "uncompressedByteSize"), createLiteral(bytes("bytes"), "xsd", "integer"))
        //add checksum
        bytes.get("md5") match{
          case Some(md5) =>
            if (checksumModel != null) {
              val checksum = model.createResource(dist.getURI + "&checksum=" + "md5")
              model.add(dist, getProperty("dataid", "checksum"), checksum)
              checksumModel.add(checksum, RDF.`type`, model.createResource(model.getNsPrefixURI("spdx") + "Checksum"))
              checksumModel.add(checksum, getProperty("spdx", "algorithm"), model.createResource(model.getNsPrefixURI("spdx") + "checksumAlgorithm_md5"))
              checksumModel.add(checksum, getProperty("spdx", "checksumValue"), createLiteral(md5, "xsd", "hexBinary"))
            }
          case None =>
        }
      case None =>
    }
    model.add(dist, getProperty("dcat", "downloadURL"), model.createResource(webDir + currentFile))
    model.add(dist, getProperty("dataid", "preview"), model.createResource("http://downloads.dbpedia.org/preview.php?file=" + dbpVersion + "_sl_" + currentFile.replace("/", "_sl_")))

    val inner = dist.getURI.substring(dist.getURI.lastIndexOf("_")).split("\\.")
    model.add(dist, getProperty("dcat", "mediaType"), getMediaType(inner(2), inner(1)))
    dist
  }

  def main(args: Array[String]) {

    require(args != null && args.length >= 1,
      "we need one argument: " +
        /*0*/ "config file location"
    )

    val config = new Config(args(0))

    config.getArbitraryStringProperty("dataid-config") match{
      case Some(s) =>
        val path = if(s.startsWith("/")) s else  System.getProperty("user.dir") + "/" + s
        val source = scala.io.Source.fromFile(path)
        configMap = JSON.parse(source.mkString.replaceAll("#[^\"]+", ""))
        source.close()
      case None => throw new IllegalArgumentException("No DataId configuration file was found, provided via the properties file (dataid-config)")
    }

    langList = config.languages.toList
    currentRootSet = null

    // Collect arguments
    webDir = configMap.get("base-url").getAsString.value() + (if (configMap.get("base-url").getAsString.value().endsWith("/")) "" else "/")
    require(URI.create(webDir) != null, "Please specify a valid web directory!")

    dump = config.dumpDir
    require(dump.isDirectory && dump.canRead, "Please specify a valid local dump directory!")

    documentation = configMap.get("documentation").getAsString.value
    require(URI.create(documentation) != null, "Please specify a valid documentation web page!")

    compression = config.formats.keySet.map(k => k.split("\\.")(1)).toList.distinct

    extensions = config.formats.keySet.map(k => k.split("\\.")(0)).toList.distinct

    require(!configMap.get("outputFileTemplate").getAsString.value.contains("."), "Please specify a valid output file name without extension")

    dbpVersion = config.dbPediaVersion
    idVersion = configMap.get("dataidVersion").getAsString.value
    vocabulary = configMap.get("vocabularyUri").getAsString.value
    require(URI.create(vocabulary) != null, "Please enter a valid ontology uri of ths DBpedia release")

    sparqlEndpoint = configMap.get("sparqlEndpoint").getAsString.value
    require(configMap.get("sparqlEndpoint") == null || UriUtils.createURI(sparqlEndpoint).isSuccess, "Please specify a valid sparql endpoint!")

    license = configMap.get("licenseUri").getAsString.value
    require(URI.create(license) != null, "Please enter a valid license uri (odrl license)")

    rights = configMap.get("rightsStatement").getAsString.value
    preamble = configMap.get("preamble").getAsString.value

    releaseDate = Option(configMap.get("releaseDate").getAsString.value) match{
      case Some(x) => dateformat.parse(x)
      case None => new Date()
    }

    //load pre/next/latest DataID catalogs, get all datasets and Dataids
    previousDataId = getOtherDataId("previousCatalog")
    nextDataId = getOtherDataId("nextCatalog")
    latestDataId = getOtherDataId("latestCatalog")

    //model for all type statements will be merged with submodels before write...
    staticModel = ModelFactory.createDefaultModel()
    addPrefixes(staticModel)

    //creating a dcat:Catalog pointing to all DataIds
    catalogModel = ModelFactory.createDefaultModel()
    addPrefixes(catalogModel)

    catalogInUse = catalogModel.createResource(webDir + dbpVersion + "_dataid_catalog.ttl")
    createCatalogInstance

    mediaTypeMap = Map(("", "") -> staticModel.createResource(staticModel.getNsPrefixURI("dataid"))) //alibi entry

    //TODO links...
    //visit all subdirectories, determine if its a dbpedia language dir, and create a DataID for this language
    //first create DataId for the core directory
    extractDataID(dump, new File(dump, "core"))
    for (outer <- dump.listFiles().filter(_.isDirectory).filter(_.getName != "core")) {
      //core has other structure (no languages)
      for (dir <- outer.listFiles().filter(_.isDirectory).filter(!_.getName.startsWith(".")))
        extractDataID(outer, dir)
    }

    //write catalog
    catalogModel.write(new FileOutputStream(new File(dump + "/" + dbpVersion + "_dataid_catalog.ttl")), "TURTLE")

    val outString = OpenRdfUtils.writeSerialization(OpenRdfUtils.convertToOpenRdfModel(catalogModel), RDFFormat.JSONLD).replace(".ttl\"", ".json\"")
    val os = new FileOutputStream(new File(dump + "/" + dbpVersion + "_dataid_catalog.json"), false)
    val printStream = new PrintStream(os)
    printStream.print(outString)
    printStream.close()
  }

  def getOtherDataId(catalogTag: String): Option[(Model, String)] = {
    Option(configMap.get(catalogTag).getAsString.value()) match {
      case Some(x) => try {
        val m = ModelFactory.createDefaultModel()
        m.read(x, "TURTLE")
        getCreationDate(m) match {
          case Some(created) if releaseDate.after(created) => throw new InvalidParameterException("The creation date of the next DataID catalog is after the current release Date.")
          case _ =>
        }

        val blank = m.listObjectsOfProperty(m.getProperty(m.getNsPrefixURI("dc"), "hasVersion")).next().asResource()
        val version = m.listObjectsOfProperty(blank, m.getProperty(m.getNsPrefixURI("dataid"), "statement")).asScala.map(y => y.asLiteral().getString).toList.headOption match {
          case Some(s) => s
          case None =>
            logger.log(Level.INFO, "The catalog provided has no dct:hasVersion property: " + catalogTag)
            return None
        }
        Option((m, version))
      }
      catch {
        case e : Throwable =>
          logger.log(Level.INFO, "An exception occurred while reading a DataID of the " + catalogTag + ": " + e.getMessage)
          None
      }
      case None => None
    }
  }

  def createCatalogInstance: Model = {
    val catalogAgent = addAgent(catalogModel, catalogInUse, catalogModel, configMap.get("creator").getAsObject)
    catalogModel.add(catalogInUse, RDF.`type`, catalogModel.createResource(catalogModel.getNsPrefixURI("dcat") + "Catalog"))
    catalogModel.add(catalogInUse, getProperty("dc", "title"), createLiteral("DataId catalog for DBpedia version " + dbpVersion))
    catalogModel.add(catalogInUse, getProperty("rdfs", "label"), createLiteral("DataId catalog for DBpedia version " + dbpVersion))
    catalogModel.add(catalogInUse, getProperty("dc", "description"), createLiteral("DataId catalog for DBpedia version " + dbpVersion + ". Every DataId represents a language dataset of DBpedia.", "en"))
    catalogModel.add(catalogInUse, getProperty("dc", "modified"), createLiteral(dateformat.format(new Date()), "xsd", "date"))
    catalogModel.add(catalogInUse, getProperty("dc", "issued"), createLiteral(dateformat.format(releaseDate), "xsd", "date"))
    catalogModel.add(catalogInUse, getProperty("dc", "publisher"), catalogAgent)
    catalogModel.add(catalogInUse, getProperty("dc", "license"), catalogModel.createResource(license))
    catalogModel.add(catalogInUse, getProperty("foaf", "homepage"), catalogModel.createResource(configMap.get("creator").getAsObject.get("homepage").getAsString.value()))
    catalogModel.add(catalogInUse, getProperty("dc", "hasVersion"), addSimpleStatement(catalogModel, catalogInUse, "version", dbpVersion, dbpVersion))

    // add prev/next/latest statements
    addVersionPointers(catalogModel, catalogInUse)
    catalogModel
  }

  def getProperty(prefix:String, propName: String): Property ={
        val pre = staticModel.getNsPrefixURI(prefix)
        staticModel.createProperty(pre, propName)
  }

  def createLiteral(value: String, lang: String = null): RDFNode ={
    if(lang == null)
      staticModel.createLiteral(value)
    else
      staticModel.createLiteral(value, lang)
  }

  def createLiteral(value: String, prefix: String, datatype: String): RDFNode ={
    staticModel.createTypedLiteral(value, staticModel.getNsPrefixURI(prefix) + datatype)
  }

  def stringCompareIgnoreDash(str1: String, str2: String): Boolean = {
    val s1 = str1.trim.toLowerCase()
    val s2 = str2.trim.toLowerCase()
    s1.replace("-", "_") == s2.replace("-", "_")
  }

  def getCreationDate(fromModel: Model): Option[Date] ={
    Option(
    try {
      val c = fromModel.listSubjectsWithProperty(fromModel.getProperty(fromModel.getNsPrefixURI("dc"), "issued")).next().asLiteral().toString
      dateformat.parse(c)
    }
    catch{
      case _: Throwable => null
    }
    )
  }

  def addVersionPointers(model: Model, currentUri: Resource): Unit = {
    previousDataId match{
      case Some(t) => getOtherVersionUri(t._1, t._2, currentUri) match{
        case Some(uri) =>
          model.add(currentUri, getProperty("dataid", "previousVersion"), uri)
        case None =>
      }
      case None =>
    }
    nextDataId match{
      case Some(t) => getOtherVersionUri(t._1, t._2, currentUri) match{
        case Some(uri) => model.add(currentUri, getProperty("dataid", "nextVersion"), uri)
        case None =>
      }
      case None =>
    }
    latestDataId match{
      case Some(t) => getOtherVersionUri(t._1, t._2, currentUri) match{
        case Some(uri) => model.add(currentUri, getProperty("dataid", "latestVersion"), uri)
        case None =>
      }
      case None => model.add(currentUri, getProperty("dataid", "latestVersion"), currentUri)
    }
  }

  def getOtherVersionUri(targetModel: Model, version: String, currentUri: Resource): Option[Resource] = {
    if (targetModel == null || targetModel.isEmpty)
      return None

    val uri = UriUtils.createURI(currentUri.getURI).get
    val params = URLEncodedUtils.parse(uri.toURI, "UTF-8").asScala
    var target = uri.getScheme + "://" + uri.getHost + uri.getPath
    target = target.replace(dbpVersion, version)
    for(i <- params.indices)
    {
      if(i == 0)
        target += "?"
      else
        target += "&"
      if(params(i).getName == "dbpv")
        target += params(i).getName + "=" + version
      else
        target += params(i).getName + "=" + params(i).getValue
    }

    val res = targetModel.createResource(target)
    lazy val subject = targetModel.query(new SimpleSelector(res, null, null.asInstanceOf[RDFNode]))
    lazy val dataid = targetModel.query(new SimpleSelector(null, targetModel.getProperty(targetModel.getNsPrefixURI("dcat"), "record"), res))
    lazy val dataset = targetModel.query(new SimpleSelector(null, targetModel.getProperty(targetModel.getNsPrefixURI("dcat"), "dataset"), res))
    if(res == null || (subject.isEmpty && dataid.isEmpty && dataset.isEmpty))
      None
    else
      Some(res)
  }

  def getWikipediaDownloadInfo(lang: Language): Option[(String, String)] ={
    val dlFile = new File(dump + "/core-i18n/" + lang.wikiCode, "download_complete_" + lang.wikiCode + ".download-complete")
    if(dlFile.exists())
      {
        try {
          val lines = scala.io.Source.fromFile(dlFile).getLines()
          Option((lines.next(), lines.next()))
        } catch {
          case _: Throwable => None
        }
      }
    else
      None
  }
}
package org.dbpedia.extraction.scripts

import java.io._
import java.net.{URLEncoder, URI}
import java.nio.charset.Charset
import java.security.InvalidParameterException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.logging.{Level, Logger}

import com.hp.hpl.jena.rdf.model.{Model, ModelFactory, Resource}
import com.hp.hpl.jena.vocabulary.RDF
import org.apache.commons.lang3.SystemUtils
import org.apache.jena.atlas.json.{JSON, JsonObject}
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Dataset}
import org.dbpedia.extraction.util.{OpenRdfUtils, Language}
import org.openrdf.rio.RDFFormat

import scala.Console._
import scala.collection.JavaConverters._
import scala.io.Source
import scala.reflect.runtime.currentMirror
import scala.reflect.runtime.universe._

import scala.language.postfixOps
import sys.process._


/**
  * Created by Chile on 1/8/2016.
  */
object DataIdGenerator {

  //todo change external instances of lexvo to DBpedia+ worldfact ids
  val dateformat = new SimpleDateFormat("yyyy-MM-dd")
  //statements
  var stmtModel: Model = null
  var agentModel: Model = null
  var checksumModel: Model = null
  var versionStatement: Resource = null
  var rightsStatement: Resource = null
  var dataidStandard: Resource = null
  var dataidLdStandard: Resource = null

  var currentDataid: Model = null

  def main(args: Array[String]) {

    require(args != null && args.length >= 1,
      "need three args: " +
        /*0*/ "config file location"
    )

    val source = scala.io.Source.fromFile(args(0))
    val jsonString = source.mkString.replaceAll("#[^\"]+", "")
    source.close()

    val configMap = JSON.parse(jsonString)
    var uri: Resource = null
    var topset: Resource = null

    val logger = Logger.getLogger(getClass.getName)

    // Collect arguments
    val webDir = configMap.get("webDir").getAsString.value() + (if (configMap.get("webDir").getAsString.value().endsWith("/")) "" else "/")
    require(URI.create(webDir) != null, "Please specify a valid web directory!")

    val dump = new File(configMap.get("localDir").getAsString.value)
    require(dump.isDirectory() && dump.canRead(), "Please specify a valid local dump directory!")

    //this csv file is created with the command below
    //line structure: file;line count;uncompressed byte size;byte size;md5 checksum
    //for file in $(find -name '*.bz2' | sort) ; do md5sum <$file | tr --delete '\n' ; stat --printf='%n %s ' $file ; bzip2 -d <$file | wc -cl ; done | awk '{sub(/-\.\//, "", $2); sub(/.bz2$/, "", $2); print $2";"$4";"$5";"$3";"$1}' >lines-bytes-packed.csv
    val lbp = Option(try {
      Source.fromFile(configMap.get("linesBytesPacked").getAsString.value)
    } catch {
      case fnf: FileNotFoundException => null
    })
    val lbpMap = lbp match {
      case Some(ld) => ld.getLines.map(_.split(";")).map(x => if(x.length > 4)
          x(0) -> Map("lines" -> x(1), "bytes" -> x(2), "bz2" -> x(3), "md5" -> x(4))
      else if (x.length == 4)
          x(0) -> Map("lines" -> x(1), "bytes" -> x(2), "bz2" -> x(3))
      else
        throw new InvalidParameterException("Lines-bytes-packed.csv file is not in an expected format!")
      ).toMap
      case None => Map[String, Map[String, String]]()
    }

    val documentation = configMap.get("documentation").getAsString.value
    require(URI.create(documentation) != null, "Please specify a valid documentation web page!")

    val compression = configMap.get("fileExtension").getAsString.value
    require(compression.startsWith("."), "please provide a valid file extension starting with a dot")

    val extensions = configMap.get("serializations").getAsArray.subList(0, configMap.get("serializations").getAsArray.size()).asScala
    require(extensions.map(x => x.getAsString.value().startsWith(".")).foldLeft(true)(_ && _), "list of valid serialization extensions starting with a dot")

    require(!configMap.get("outputFileTemplate").getAsString.value.contains("."), "Please specify a valid output file name without extension")

    val dbpVersion = configMap.get("dbpediaVersion").getAsString.value
    val idVersion = configMap.get("dataidVersion").getAsString.value
    val vocabulary = configMap.get("vocabularyUri").getAsString.value
    require(URI.create(vocabulary) != null, "Please enter a valid ontology uri of ths DBpedia release")

    val sparqlEndpoint = configMap.get("sparqlEndpoint").getAsString.value
    require(configMap.get("sparqlEndpoint") == null || URI.create(sparqlEndpoint) != null, "Please specify a valid sparql endpoint!")

    var coreList = List("")

    val license = configMap.get("licenseUri").getAsString.value
    require(URI.create(license) != null, "Please enter a valid license uri (odrl license)")

    val rights = configMap.get("rightsStatement").getAsString.value

    val r = currentMirror.reflect(DBpediaDatasets)

    val datasetDescriptionsOriginal = r.symbol.typeSignature.members.toStream
      .collect { case s: TermSymbol if !s.isMethod => r.reflectField(s) }
      .map(t => t.get match {
        case y: Dataset => y
        case _ =>
      }).toList.asInstanceOf[List[Dataset]]

    val datasetDescriptions = datasetDescriptionsOriginal
      .map(d => new Dataset(d.name.replace("_", "-"), d.description)) ++ datasetDescriptionsOriginal
      .filter(_.name.endsWith("unredirected"))
      .map(d => new Dataset(d.name.replace("_unredirected", "").replace("_", "-"), d.description + " This dataset has Wikipedia redirects resolved.")) ++ datasetDescriptionsOriginal
      .map(d => new Dataset(d.name.replace(d.name, d.name + "-en-uris").replace("_", "-"), d.description + " Normalized resources matching English DBpedia.")) ++ datasetDescriptionsOriginal
      .map(d => new Dataset(d.name.replace(d.name, d.name + "-en-uris-unredirected").replace("_", "-"), d.description + " Normalized resources matching English DBpedia. This dataset has Wikipedia redirects resolved.")).sortBy(x => x.name)

    def addPrefixes(model: Model): Unit = {
      val prefixMap = configMap.get("prefixMap").getAsObject
      for(prefix <- prefixMap.keys().asScala){
        model.setNsPrefix(prefix, prefixMap.get(prefix).getAsString.toString)
      }
    }

    def addAgent(agentModel: Model, motherResource: Resource, agentMap: JsonObject, specialEntities: List[Resource] = List()): Resource = {
      val agent = agentModel.createResource(agentMap.get("uri").getAsString.value())
      agentModel.add(agent, RDF.`type`, agentModel.createResource(agentModel.getNsPrefixURI("dataid") + "Agent"))
      agentModel.add(agent, agentModel.createProperty(agentModel.getNsPrefixURI("foaf"), "name"), agentModel.createLiteral(agentMap.get("name").getAsString.value()))
      if (agentMap.get("homepage") != null)
        agentModel.add(agent, agentModel.createProperty(agentModel.getNsPrefixURI("foaf"), "homepage"), agentModel.createResource(agentMap.get("homepage").getAsString.value()))
      agentModel.add(agent, agentModel.createProperty(agentModel.getNsPrefixURI("foaf"), "mbox"), agentModel.createLiteral(agentMap.get("mbox").getAsString.value()))


        val context = agentModel.createResource(motherResource.getURI + (if(motherResource.getURI.contains("?")) "&" else "?") + "auth=" + agentMap.get("role").getAsString.value().toLowerCase + "Authorization")
        agentModel.add(context, RDF.`type`, agentModel.createResource(agentModel.getNsPrefixURI("dataid") + "Authorization"))
        agentModel.add(context, agentModel.createProperty(agentModel.getNsPrefixURI("dataid"), "authorizedAgent"), agent)
        agentModel.add(agent, agentModel.createProperty(agentModel.getNsPrefixURI("dataid"), "hasAuthorization"), context)
        agentModel.add(context, agentModel.createProperty(agentModel.getNsPrefixURI("dataid"), "authorityAgentRole"), agentModel.createResource(agentModel.getNsPrefixURI("dataid") + agentMap.get("role").getAsString.value()))
        agentModel.add(context, agentModel.createProperty(agentModel.getNsPrefixURI("dataid"), "isInheritable"), agentModel.createTypedLiteral("true", agentModel.getNsPrefixURI("xsd") + "boolean"))

        //distribute authorizations
        if (specialEntities.isEmpty) //no special entities -> we assume its valid for whole DataID
        {
          agentModel.add(context, agentModel.createProperty(agentModel.getNsPrefixURI("dataid"), "authorizedFor"), motherResource)
          currentDataid.add(motherResource, currentDataid.createProperty(currentDataid.getNsPrefixURI("dataid"), "underAuthorization"), context)
        }
        else {
          for (ent <- specialEntities) //with special entities -> we assume they need dataid:needsSpecialAuthorization
          {
            agentModel.add(context, agentModel.createProperty(agentModel.getNsPrefixURI("dataid"), "authorizedFor"), ent)
            currentDataid.add(ent, currentDataid.createProperty(currentDataid.getNsPrefixURI("dataid"), "needsSpecialAuthorization"), context)
          }
        }

      //add identifier
      if (agentMap.get("identifier") != null) {
        val idMap = agentMap.get("identifier").getAsObject
        val id = agentModel.createResource(idMap.get("url").getAsString.value())
        agentModel.add(agent, agentModel.createProperty(agentModel.getNsPrefixURI("dataid"), "identifier"), id)
        if (idMap.get("literal") != null) {
          agentModel.add(id, RDF.`type`, agentModel.createResource(agentModel.getNsPrefixURI("dataid") + "Identifier"))
          agentModel.add(id, agentModel.createProperty(agentModel.getNsPrefixURI("dataid"), "literal"), agentModel.createLiteral(idMap.get("literal").getAsString.value()))
          agentModel.add(id, agentModel.createProperty(agentModel.getNsPrefixURI("dc"), "references"), id)
          agentModel.add(id, agentModel.createProperty(agentModel.getNsPrefixURI("datacite"), "usesIdentifierScheme"), agentModel.createResource(idMap.get("scheme").getAsString.value()))
          if (idMap.get("issued") != null)
            agentModel.add(id, agentModel.createProperty(agentModel.getNsPrefixURI("dc"), "issued"), agentModel.createTypedLiteral(dateformat.format(dateformat.parse(idMap.get("issued").getAsString.value())), agentModel.getNsPrefixURI("xsd") + "date"))
        }
      }
      agent
    }

    //model for all type statements will be merged with submodels before write...
    val staticModel = ModelFactory.createDefaultModel()
    val defaultAgentModel: Model =  ModelFactory.createDefaultModel()
    addPrefixes(staticModel)
    addPrefixes(defaultAgentModel)

    var mediaTypeMap = Map(("", "") -> staticModel.createResource(staticModel.getNsPrefixURI("dataid"))) //alibi entry

    def getMediaType(outer: String, inner: String): Resource = {
      try {
        return mediaTypeMap((outer, inner))
      }
      catch {
        case e: NoSuchElementException => {
          val o = Option(outer match {
            case y if y.contains("gz") => "application/x-gzip"
            case z if z.contains("bz2") => "application/x-bzip2"
            case "sparql" => "application/sparql-results+xml"
            case _ => {
              logger.log(Level.SEVERE, "outer MediaType could not be determined: " + outer)
              null
            }
          })
          val oe = Option(outer match {
            case y if y.contains("gz") => ".gz"
            case z if z.contains("bz2") => ".bz2"
            case _ => {
              logger.log(Level.WARNING, "outer file extension could not be determined: " + outer)
              null
            }
          })
          val i = Option(inner match {
            case ttl if ttl.contains(".ttl") => "text/turtle"
            case tql if tql.contains(".tql") || tql.contains(".nq") => "application/n-quads"
            case nt if nt.contains(".nt") => "application/n-triples"
            case xml if xml.contains(".xml") => "application/xml"
            case _ => {
              logger.log(Level.WARNING, "inner MediaType could not be determined: " + inner)
              null
            }
          })
          val ie = Option(inner match {
            case ttl if ttl.contains(".ttl") => ".ttl"
            case tql if tql.contains(".tql") || tql.contains(".nq") => ".tql"
            case nt if nt.contains(".nt") => ".nt"
            case xml if xml.contains(".xml") => "application/xml"
            case _ => {
              logger.log(Level.WARNING, "inner file extension could not be determined: " + inner)
              null
            }
          })

          //this is the outer mime type (don't be confused by the inner match!
          val mime = i match{
            case Some(in) => staticModel.createResource(staticModel.getNsPrefixURI("dataid-mt") + "MediaType_" + in.substring(in.lastIndexOf("/") + 1) + "_" + o.get.substring(o.get.lastIndexOf("/") + 1))
            case None => staticModel.createResource(staticModel.getNsPrefixURI("dataid-mt") + "MediaType_" + o.get.substring(o.get.lastIndexOf("/") + 1))
          }

          staticModel.add(mime, RDF.`type`, staticModel.createResource(staticModel.getNsPrefixURI("dataid") + "MediaType"))
          staticModel.add(mime, staticModel.createProperty(staticModel.getNsPrefixURI("dataid"), "typeTemplate"), staticModel.createLiteral(o.get))
          staticModel.add(mime, staticModel.createProperty(staticModel.getNsPrefixURI("dc"), "conformsTo"), dataidStandard)
          oe match{
            case Some(ooe) => staticModel.add(mime, staticModel.createProperty(staticModel.getNsPrefixURI("dataid"), "typeExtension"), staticModel.createLiteral(ooe))
            case None =>
          }

          //this is the inner mime type
          i match{
            case Some(ii) =>{
              val it = staticModel.createResource(staticModel.getNsPrefixURI("dataid") + "MediaType_" + ii.substring(ii.lastIndexOf("/") + 1))
              staticModel.add(it, RDF.`type`, staticModel.createResource(staticModel.getNsPrefixURI("dataid") + "MediaType"))
              staticModel.add(mime, staticModel.createProperty(staticModel.getNsPrefixURI("dataid"), "innerMediaType"), it)
              staticModel.add(it, staticModel.createProperty(staticModel.getNsPrefixURI("dataid"), "typeTemplate"), staticModel.createLiteral(ii))
              staticModel.add(it, staticModel.createProperty(staticModel.getNsPrefixURI("dc"), "conformsTo"), dataidStandard)
              ie match {
                case Some(iie) =>
                  if (iie == ".tql")
                    staticModel.add(it, staticModel.createProperty(staticModel.getNsPrefixURI("dataid"), "typeExtension"), staticModel.createLiteral(".nq"))
                  else
                    staticModel.add(it, staticModel.createProperty(staticModel.getNsPrefixURI("dataid"), "typeExtension"), staticModel.createLiteral(iie))
                case None =>
              }
              mediaTypeMap += (inner, null) -> it
            }
            case None =>
          }
          mediaTypeMap += (outer, inner) -> mime
          mime
        }
        case ex : Exception => throw ex
      }
    }

    def addSimpleStatement(typ: String, uriVal: String, stmt: String, lang: Language = null, ref: Resource = null): Resource = {
      val ss = if (ref != null && ref.isURIResource)
        stmtModel.createResource(ref.getURI + (if (uriVal != null) "#" + typ + "=" + URLEncoder.encode(uriVal, "UTF-8") else ""))
      else
        stmtModel.createResource(uri.getURI + (if (uriVal != null) "?" + typ + "=" + URLEncoder.encode(uriVal, "UTF-8") else ""))
      stmtModel.add(ss, RDF.`type`, stmtModel.createResource(stmtModel.getNsPrefixURI("dataid") + "SimpleStatement"))
      if (lang != null)
        stmtModel.add(ss, stmtModel.createProperty(stmtModel.getNsPrefixURI("dataid"), "statement"), stmtModel.createLiteral(stmt, lang.isoCode))
      else
        stmtModel.add(ss, stmtModel.createProperty(stmtModel.getNsPrefixURI("dataid"), "statement"), stmtModel.createLiteral(stmt))
      if (ref != null)
        stmtModel.add(ss, stmtModel.createProperty(stmtModel.getNsPrefixURI("dc"), "references"), ref)
      ss
    }

    def addSparqlEndpoint(dataset: Resource): Model = {
      val sparql: Model = ModelFactory.createDefaultModel()
      addPrefixes(sparql)
      val dist = sparql.createResource(uri.getURI + "?sparql=DBpediaSparqlEndpoint")
      val sparqlAgent = addAgent(defaultAgentModel, dist, configMap.get("openLink").getAsObject )
      sparql.add(dist, RDF.`type`, sparql.createResource(sparql.getNsPrefixURI("dataid-ld") + "SparqlEndpoint"))
      sparql.add(dataset, sparql.createProperty(sparql.getNsPrefixURI("dcat"), "distribution"), dist)
      sparql.add(dist, sparql.createProperty(sparql.getNsPrefixURI("dataid"), "isDistributionOf"), dataset)
      sparql.add(dist, sparql.createProperty(sparql.getNsPrefixURI("dc"), "hasVersion"), versionStatement)
      sparql.add(dist, sparql.createProperty(sparql.getNsPrefixURI("dc"), "title"), sparql.createLiteral("The official DBpedia sparql endpoint", "en"))
      sparql.add(dist, sparql.createProperty(sparql.getNsPrefixURI("dc"), "description"), sparql.createLiteral("The official sparql endpoint of DBpedia, hosted graciously by OpenLink Software (http://virtuoso.openlinksw.com/), containing all datasets of the /core directory.", "en"))
      sparql.add(dist, sparql.createProperty(sparql.getNsPrefixURI("rdfs"), "label"), sparql.createLiteral("The official DBpedia sparql endpoint", "en"))
      sparql.add(dist, sparql.createProperty(sparql.getNsPrefixURI("dataid"), "associatedAgent"), sparqlAgent)
      sparql.add(dist, sparql.createProperty(sparql.getNsPrefixURI("dc"), "modified"), sparql.createTypedLiteral(dateformat.format(new Date()), sparql.getNsPrefixURI("xsd") + "date"))
      sparql.add(dist, sparql.createProperty(sparql.getNsPrefixURI("dc"), "issued"), sparql.createTypedLiteral(dateformat.format(new Date()), sparql.getNsPrefixURI("xsd") + "date"))
      sparql.add(dist, sparql.createProperty(sparql.getNsPrefixURI("dc"), "license"), sparql.createResource(license))
      sparql.add(dist, sparql.createProperty(sparql.getNsPrefixURI("dcat"), "mediaType"), getMediaType("sparql", ""))
      sparql.add(dist, sparql.createProperty(sparql.getNsPrefixURI("dcat"), "accessURL"), sparql.createResource(sparqlEndpoint))
      sparql.add(dist, sparql.createProperty(sparql.getNsPrefixURI("void"), "sparqlEndpoint"), sparql.createResource(sparqlEndpoint))
      sparql.add(dist, sparql.createProperty(sparql.getNsPrefixURI("dataid-ld"), "graphName"), sparql.createResource("http://dbpedia.org"))
      sparql.add(dist, sparql.createProperty(sparql.getNsPrefixURI("dataid"), "accessProcedure"), addSimpleStatement("stmt", "sparqlaccproc", "An endpoint for sparql queries: provide valid queries."))
      sparql.add(dist, sparql.createProperty(sparql.getNsPrefixURI("dc"), "conformsTo"), dataidStandard)
      sparql.add(dist, sparql.createProperty(sparql.getNsPrefixURI("dc"), "conformsTo"), dataidLdStandard)
      sparql
    }

    //creating a dcat:Catalog pointing to all DataIds
    val catalogModel = ModelFactory.createDefaultModel()
    addPrefixes(catalogModel)

    val catalog = catalogModel.createResource(webDir + dbpVersion + "_dataid_catalog.ttl")
    //val catalogAgent = addAgent(defaultAgentModel, catalog, configMap.get("creator").getAsObject)

    catalogModel.add(catalog, RDF.`type`, catalogModel.createResource(catalogModel.getNsPrefixURI("dcat") + "Catalog"))
    catalogModel.add(catalog, catalogModel.createProperty(catalogModel.getNsPrefixURI("dc"), "title"), catalogModel.createLiteral("DataId catalog for DBpedia version " + dbpVersion))
    catalogModel.add(catalog, catalogModel.createProperty(catalogModel.getNsPrefixURI("rdfs"), "label"), catalogModel.createLiteral("DataId catalog for DBpedia version " + dbpVersion))
    catalogModel.add(catalog, catalogModel.createProperty(catalogModel.getNsPrefixURI("dc"), "description"), catalogModel.createLiteral("DataId catalog for DBpedia version " + dbpVersion + ". Every DataId represents a language dataset of DBpedia.", "en"))
    catalogModel.add(catalog, catalogModel.createProperty(catalogModel.getNsPrefixURI("dc"), "modified"), catalogModel.createTypedLiteral(dateformat.format(new Date()), catalogModel.getNsPrefixURI("xsd") + "date"))
    catalogModel.add(catalog, catalogModel.createProperty(catalogModel.getNsPrefixURI("dc"), "issued"), catalogModel.createTypedLiteral(dateformat.format(new Date()), catalogModel.getNsPrefixURI("xsd") + "date"))
    //catalogModel.add(catalog, catalogModel.createProperty(catalogModel.getNsPrefixURI("dc"), "publisher"), catalogAgent)
    catalogModel.add(catalog, catalogModel.createProperty(catalogModel.getNsPrefixURI("dc"), "license"), catalogModel.createResource(license))

    catalogModel.add(catalog, catalogModel.createProperty(catalogModel.getNsPrefixURI("foaf"), "homepage"), catalogModel.createResource(configMap.get("creator").getAsObject.get("homepage").getAsString.value()))

    def addDistribution(model: Model, dataset: Resource, lang: Language, outerDirectory: String, currentFile: String, associatedAgent: Resource): Resource = {
      val dist = model.createResource(uri.getURI + "?file=" + currentFile)
      model.add(dist, RDF.`type`, model.createResource(model.getNsPrefixURI("dataid") + "SingleFile"))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dcat"), "distribution"), dist)
      model.add(dist, model.createProperty(model.getNsPrefixURI("dataid"), "isDistributionOf"), dataset)

      datasetDescriptions.find(x => stringCompareIgnoreDash(x.name, currentFile.substring(0, currentFile.lastIndexOf("_")))) match {
        case Some(d) => model.add(dist, model.createProperty(model.getNsPrefixURI("dc"), "title"), model.createLiteral(d.name.replace("-", " ").replace("_", " "), "en"))
        case None => model.add(dist, model.createProperty(model.getNsPrefixURI("dc"), "title"), model.createLiteral(currentFile.substring(0, currentFile.lastIndexOf("_")).replace("-", " ").replace("_", " ") + " dataset", "en"))
      }

      datasetDescriptions.find(x => stringCompareIgnoreDash(x.name, currentFile.substring(0, currentFile.lastIndexOf("_"))) && x.description != null) match {
        case Some(d) =>
          model.add(dist, model.createProperty(model.getNsPrefixURI("dc"), "description"), model.createLiteral(d.description, "en"))
        case None => err.println("Could not find description for distribution: " + (if (lang != null) {
          "_" + lang.wikiCode.replace("-", "_")
        } else "") + " / " + currentFile)
      }

      model.add(dist, model.createProperty(model.getNsPrefixURI("rdfs"), "label"), model.createLiteral(currentFile))
      //TODO done by DataId Hub
      model.add(dist, model.createProperty(model.getNsPrefixURI("dc"), "hasVersion"), versionStatement)
      //TODO model.add(dist, model.createProperty(model.getNsPrefixURI("dataid"), "latestVersion"), dist)
      model.add(dist, model.createProperty(model.getNsPrefixURI("dataid"), "associatedAgent"), associatedAgent)
      model.add(dist, model.createProperty(model.getNsPrefixURI("dc"), "publisher"), associatedAgent)
      model.add(dist, model.createProperty(model.getNsPrefixURI("dc"), "modified"), model.createTypedLiteral(dateformat.format(new Date()), model.getNsPrefixURI("xsd") + "date"))
      model.add(dist, model.createProperty(model.getNsPrefixURI("dc"), "issued"), model.createTypedLiteral(dateformat.format(new Date()), model.getNsPrefixURI("xsd") + "date"))
      model.add(dist, model.createProperty(model.getNsPrefixURI("dc"), "license"), model.createResource(license))
      model.add(dist, model.createProperty(model.getNsPrefixURI("dc"), "conformsTo"), dataidStandard)

      if (outerDirectory != null && lang != null) {
        lbpMap.get(outerDirectory + "/" + lang.wikiCode.replace("-", "_") + "/" + currentFile)
        match {
          case Some(bytes) => {
            model.add(dist, model.createProperty(model.getNsPrefixURI("dcat"), "byteSize"), model.createTypedLiteral(bytes.get("bz2").get, model.getNsPrefixURI("xsd") + "integer"))
            model.add(dist, model.createProperty(model.getNsPrefixURI("dataid"), "uncompressedByteSize"), model.createTypedLiteral(bytes.get("bytes").get, model.getNsPrefixURI("xsd") + "integer"))
            //add checksum
            bytes.get("md5") match{
              case Some(md5) => {
                if (checksumModel != null) {
                  val checksum = model.createResource(dist.getURI + "&checksum=" + "md5")
                  model.add(dist, model.createProperty(model.getNsPrefixURI("dataid"), "checksum"), checksum)
                  checksumModel.add(checksum, RDF.`type`, model.createResource(model.getNsPrefixURI("spdx") + "Checksum"))
                  checksumModel.add(checksum, model.createProperty(model.getNsPrefixURI("spdx"), "algorithm"), model.createResource(model.getNsPrefixURI("spdx") + "checksumAlgorithm_md5"))
                  checksumModel.add(checksum, model.createProperty(model.getNsPrefixURI("spdx"), "checksumValue"), model.createTypedLiteral(md5, model.getNsPrefixURI("xsd") + "hexBinary"))
                }
              }
              case None =>
            }
          }
          case None =>
        }
        model.add(dist, model.createProperty(model.getNsPrefixURI("dcat"), "downloadURL"), model.createResource(webDir + outerDirectory + "/" + lang.wikiCode.replace("-", "_").replace("-", "_") + "/" + currentFile))
        model.add(dist, model.createProperty(model.getNsPrefixURI("dataid"), "preview"), model.createResource("http://downloads.dbpedia.org/preview.php?file=" + dbpVersion + "_sl_" + outerDirectory + "_sl_" + lang.wikiCode.replace("-", "_").replace("-", "_") + "_sl_" + currentFile))
      }
      var inner = dist.getURI.substring(dist.getURI.lastIndexOf("_"))
      inner = inner.substring(inner.indexOf(".")).replace(compression, "")
      model.add(dist, model.createProperty(model.getNsPrefixURI("dcat"), "mediaType"), getMediaType(compression, inner))
      dist
    }

    val dumpFile = "^[a-zA-Z0-9-_]+".r

    def extractDataID(outer: File, dir: File): Unit =
    {
      val innerPath = if(outer.getName == "core") "" else outer.getName
      val lang = Language.get(dir.getName.replace("_", "-")) match {
        case Some(l) => l
        case _ =>
          logger.log(Level.INFO, "no language found for: " + dir.getName)
          null
      }
      val fileFilter = ("^[^$]+_[a-z-_]+(" + extensions.foldLeft(new StringBuilder) { (sb, s) => sb.append("|" + s.getAsString.value()) }.toString.substring(1) + "|.xml)" + compression).replace(".", "\\.")

      val distributions = try {
        if (SystemUtils.IS_OS_UNIX) {
          //Linux: have to use processes to avoid symlink problem with listFiles
          val commandRes: String = ("ls -1 " + dir.getAbsolutePath).!!
          commandRes.split("\\n").flatMap(x => fileFilter.r.findFirstIn(x)).map(_.trim.replace("-", "_")).toList.sorted
        }
        else if (SystemUtils.IS_OS_WINDOWS) {
          val filter = new FilenameFilter {
            override def accept(dir: File, name: String): Boolean = {
              if (name.matches(fileFilter))
                return true
              else
                return false
            }
          }
          dir.listFiles(filter).map(x => x.getName.replace("-", "_")).toList.sorted
        }
        else
          List()
      }
      catch {
        case e : Exception => {
          logger.log(Level.WARNING, "problems with directory: " + dir)
          List()
        }
      }

        if(dir.getName == "core")
          coreList = distributions.flatMap( dis => dumpFile.findFirstIn(dis))

        if (lang != null && distributions.map(x => x.contains("short_abstracts") || x.contains("interlanguage_links")).foldRight(false)(_ || _)) {
          currentDataid = ModelFactory.createDefaultModel()
          val topsetModel = ModelFactory.createDefaultModel()
          agentModel = ModelFactory.createDefaultModel()
          val mainModel = ModelFactory.createDefaultModel()
          stmtModel = ModelFactory.createDefaultModel()
          checksumModel = ModelFactory.createDefaultModel()

          addPrefixes(currentDataid)
          addPrefixes(topsetModel)
          addPrefixes(mainModel)
          addPrefixes(agentModel)
          addPrefixes(stmtModel)
          addPrefixes(checksumModel)

          val ttlOutFile = new File(dir.getAbsolutePath.replace("\\", "/") + "/" + configMap.get("outputFileTemplate").getAsString.value + "_" + lang.wikiCode.replace("-", "_") + ".ttl")
          val jldOutFile = new File(dir.getAbsolutePath.replace("\\", "/") + "/" + configMap.get("outputFileTemplate").getAsString.value + "_" + lang.wikiCode.replace("-", "_") + ".json")
          logger.log(Level.INFO, "started DataId: " + ttlOutFile.getAbsolutePath)

          uri = currentDataid.createResource(webDir + innerPath + "/" + lang.wikiCode.replace("-", "_") + "/" + configMap.get("outputFileTemplate").getAsString.value + "_" + lang.wikiCode.replace("-", "_") + ".ttl")
          require(uri != null, "Please provide a valid directory")
          currentDataid.add(uri, RDF.`type`, currentDataid.createResource(currentDataid.getNsPrefixURI("dataid") + "DataId"))

          //statements
          versionStatement = addSimpleStatement("version", idVersion, idVersion)
          rightsStatement = addSimpleStatement("rights", "dbpedia-rights", rights, Language.English)
          dataidStandard = addSimpleStatement(null, null, "DataID - dataset metadata ontology", Language.English, staticModel.createResource("http://dataid.dbpedia.org/ns/core"))
          dataidLdStandard = addSimpleStatement(null, null, "DataID-LD - dataset metadata ontology with linked data extension", Language.English, staticModel.createResource("http://dataid.dbpedia.org/ns/ld"))

          val creator = addAgent(agentModel, uri, configMap.get("creator").getAsObject)
          val maintainer = addAgent(agentModel, uri, configMap.get("maintainer").getAsObject)
          val contact = addAgent(agentModel, uri, configMap.get("contact").getAsObject)
          require(creator != null, "Please define an dataid:Agent as a Creator in the dataid stump file (use dataid:Authorization).")

          currentDataid.add(uri, currentDataid.createProperty(currentDataid.getNsPrefixURI("dc"), "modified"), currentDataid.createTypedLiteral(dateformat.format(new Date()), currentDataid.getNsPrefixURI("xsd") + "date"))
          currentDataid.add(uri, currentDataid.createProperty(currentDataid.getNsPrefixURI("dc"), "issued"), currentDataid.createTypedLiteral(dateformat.format(new Date()), currentDataid.getNsPrefixURI("xsd") + "date"))
          currentDataid.add(uri, currentDataid.createProperty(currentDataid.getNsPrefixURI("dataid"), "latestVersion"), uri)
          currentDataid.add(uri, currentDataid.createProperty(currentDataid.getNsPrefixURI("dataid"), "associatedAgent"), creator)
          currentDataid.add(uri, currentDataid.createProperty(currentDataid.getNsPrefixURI("dataid"), "associatedAgent"), maintainer)
          currentDataid.add(uri, currentDataid.createProperty(currentDataid.getNsPrefixURI("dataid"), "associatedAgent"), contact)
          currentDataid.add(uri, currentDataid.createProperty(currentDataid.getNsPrefixURI("dataid"), "inCatalog"), catalog)
          currentDataid.add(uri, currentDataid.createProperty(currentDataid.getNsPrefixURI("dc"), "title"), currentDataid.createLiteral("DataID metadata for the " + lang.name + " DBpedia", "en"))
          currentDataid.add(uri, currentDataid.createProperty(currentDataid.getNsPrefixURI("dc"), "conformsTo"), dataidStandard)
          currentDataid.add(uri, currentDataid.createProperty(currentDataid.getNsPrefixURI("dc"), "conformsTo"), dataidLdStandard)
          currentDataid.add(uri, currentDataid.createProperty(currentDataid.getNsPrefixURI("dc"), "publisher"), creator)
          currentDataid.add(uri, currentDataid.createProperty(currentDataid.getNsPrefixURI("dc"), "hasVersion"), versionStatement)
          catalogModel.add(catalog, catalogModel.createProperty(catalogModel.getNsPrefixURI("dcat"), "record"), uri)

          topset = addDataset(topsetModel, lang, "maindataset", creator, true)

          catalogModel.add(catalog, catalogModel.createProperty(catalogModel.getNsPrefixURI("dcat"), "dataset"), topset)
          currentDataid.add(uri, currentDataid.createProperty(currentDataid.getNsPrefixURI("foaf"), "primaryTopic"), topset)
          topsetModel.add(topset, topsetModel.createProperty(topsetModel.getNsPrefixURI("foaf"), "isPrimaryTopicOf"), uri)
          topsetModel.add(topset, topsetModel.createProperty(topsetModel.getNsPrefixURI("void"), "vocabulary"), topsetModel.createResource(vocabulary))
          topsetModel.add(topset, topsetModel.createProperty(topsetModel.getNsPrefixURI("void"), "vocabulary"), topsetModel.createResource(vocabulary.replace(".owl", ".nt")))
          topsetModel.add(topset, topsetModel.createProperty(topsetModel.getNsPrefixURI("dc"), "description"), topsetModel.createLiteral(configMap.get("description").getAsString.value, "en"))
          topsetModel.add(topset, topsetModel.createProperty(topsetModel.getNsPrefixURI("dc"), "title"), topsetModel.createLiteral("DBpedia root dataset for " + lang.name + ", version " + dbpVersion, "en"))

          if (rights != null)
            topsetModel.add(topset, topsetModel.createProperty(topsetModel.getNsPrefixURI("dc"), "rights"), rightsStatement)

          if ((configMap.get("addDmpProps").getAsBoolean.value()))
            addDmpStatements(topsetModel, topset)

          var lastFile: String = null
          var dataset: Resource = null
          for (dis <- distributions) {
            if(dis.contains("_" + dir.getName))
            {
              if (lastFile != dis.substring(0, dis.lastIndexOf("_" + dir.getName))) {
                lastFile = dis.substring(0, dis.lastIndexOf("_" + dir.getName))
                dataset = addDataset(mainModel, lang, dis, creator)
                topsetModel.add(topset, topsetModel.createProperty(topsetModel.getNsPrefixURI("void"), "subset"), dataset)
                mainModel.add(dataset, mainModel.createProperty(mainModel.getNsPrefixURI("dc"), "isPartOf"), topset)
              }
              dumpFile.findFirstIn(dis) match {
                case Some(l) =>
                  if(coreList.contains(l))
                    mainModel.add(addSparqlEndpoint(dataset))
                case None =>
              }
              addDistribution(mainModel, dataset, lang, outer.getName, dis, creator)
            }
          }

          //TODO validate & publish DataIds online!!!

          //dataidModel.add(staticModel)                                                     //adding type statements
          currentDataid.write(new FileOutputStream(ttlOutFile), "TURTLE")
          currentDataid.add(agentModel)
          var baos = new ByteArrayOutputStream()
          agentModel.write(baos, "TURTLE")
          var outString = new String(baos.toByteArray(), Charset.defaultCharset())
          outString = "\n#### Agents & Authorizations ####\n" +
            outString.replaceAll("(@prefix).*\\n", "")

          currentDataid.add(defaultAgentModel)
          baos = new ByteArrayOutputStream()
          defaultAgentModel.write(baos, "TURTLE")
          outString += new String(baos.toByteArray(), Charset.defaultCharset()).replaceAll("(@prefix).*\\n", "")

          currentDataid.add(topsetModel)
          baos = new ByteArrayOutputStream()
          topsetModel.write(baos, "TURTLE")
          outString += "\n########## Main Dataset ##########\n" +
            new String(baos.toByteArray(), Charset.defaultCharset()).replaceAll("(@prefix).*\\n", "")

          currentDataid.add(mainModel)
          baos = new ByteArrayOutputStream()
          mainModel.write(baos, "TURTLE")
          outString += "\n#### Datasets & Distributions ####\n" +
            new String(baos.toByteArray(), Charset.defaultCharset()).replaceAll("(@prefix).*\\n", "")

          currentDataid.add(checksumModel)
          baos = new ByteArrayOutputStream()
          checksumModel.write(baos, "TURTLE")
          outString += "\n#### Checksums ####\n" +
            new String(baos.toByteArray(), Charset.defaultCharset()).replaceAll("(@prefix).*\\n", "")

          currentDataid.add(stmtModel)
          baos = new ByteArrayOutputStream()
          stmtModel.write(baos, "TURTLE")
          outString += "\n########### Statements ###########\n" +
            new String(baos.toByteArray(), Charset.defaultCharset()).replaceAll("(@prefix).*\\n", "")

          currentDataid.add(staticModel)
          baos = new ByteArrayOutputStream()
          staticModel.write(baos, "TURTLE")
          outString += "\n########### MediaTypes ###########\n" +
            new String(baos.toByteArray(), Charset.defaultCharset()).replaceAll("(@prefix).*\\n", "")

          var os = new FileOutputStream(ttlOutFile, true)
          var printStream = new PrintStream(os)
          printStream.print(outString)
          printStream.close()

          outString = OpenRdfUtils.writeSerialization(OpenRdfUtils.convertToOpenRdfModel(currentDataid), RDFFormat.JSONLD)
          os = new FileOutputStream(jldOutFile, false)
          printStream = new PrintStream(os)
          printStream.print(outString)
          printStream.close()

          logger.log(Level.INFO, "finished DataId: " + ttlOutFile.getAbsolutePath)

        }
      }

    def addDmpStatements(model: Model, dataset: Resource): Unit = {
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dataid"), "usefulness"), addSimpleStatement("stmt", "usefulness", configMap.get("dmpusefulness").getAsString.value, Language.English))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dataid"), "similarData"), addSimpleStatement("stmt", "similarData", configMap.get("dmpsimilarData").getAsString.value, Language.English))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dataid"), "reuseAndIntegration"), addSimpleStatement("stmt", "reuseAndIntegration", configMap.get("dmpreuseAndIntegration").getAsString.value, Language.English))
      //TODO put that to distributions... model.add(dataset, model.createProperty(model.getNsPrefixURI("dataid"), "softwareRequirement"), addSimpleStatement("stmt", "softwareRequirement", configMap.get("dmpadditionalSoftware").getAsString.value, Language.English))
      //TODO model.add(dataset, model.createProperty(model.getNsPrefixURI("dmp"), "repositoryUrl"), model.createResource(configMap.get("dmprepositoryUrl").getAsString.value))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dataid"), "growth"), addSimpleStatement("stmt", "growth", configMap.get("dmpgrowth").getAsString.value, Language.English))
      //TODO model.add(dataset, model.createProperty(model.getNsPrefixURI("dmp"), "archiveLink"), model.createResource(configMap.get("dmparchiveLink").getAsString.value))
      //TODO model.add(dataset, model.createProperty(model.getNsPrefixURI("dmp"), "preservation"), model.createLiteral(configMap.get("dmppreservation").getAsString.value, "en"))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dataid"), "openness"), addSimpleStatement("stmt", "openness", configMap.get("dmpopenness").getAsString.value, Language.English))
    }

    def addDataset(model: Model, lang: Language, currentFile: String, associatedAgent: Resource, toplevelSet: Boolean = false): Resource = {
      val datasetName = if (currentFile.contains("_")) currentFile.substring(0, currentFile.lastIndexOf("_")) else currentFile
      val dataset = model.createResource(uri.getURI + "?set=" + datasetName)
      model.add(dataset, RDF.`type`, model.createResource(model.getNsPrefixURI("dataid") + "Dataset"))
      if (!toplevelSet) //not!
      {
        model.add(dataset, model.createProperty(model.getNsPrefixURI("void"), "rootResource"), topset)


        datasetDescriptions.find(x => stringCompareIgnoreDash(x.name, datasetName)) match {
          case Some(d) => {
            model.add(dataset, model.createProperty(model.getNsPrefixURI("dc"), "title"), model.createLiteral(d.name.replace("-", " ").replace("_", " "), "en"))
            model.add(dataset, model.createProperty(model.getNsPrefixURI("rdfs"), "label"), model.createLiteral(d.name.replace("-", " ").replace("_", " "), "en"))
          }
          case None => {
            model.add(dataset, model.createProperty(model.getNsPrefixURI("dc"), "title"), model.createLiteral(currentFile.substring(0, currentFile.lastIndexOf("_")).replace("-", " ").replace("_", " ") + " dataset", "en"))
            model.add(dataset, model.createProperty(model.getNsPrefixURI("rdfs"), "label"), model.createLiteral(currentFile.substring(0, currentFile.lastIndexOf("_")).replace("-", " ").replace("_", " ") + " dataset", "en"))
          }
        }

        datasetDescriptions.find(x => stringCompareIgnoreDash(x.name, datasetName) && x.description != null) match {
          case Some(d) => model.add(dataset, model.createProperty(model.getNsPrefixURI("dc"), "description"), model.createLiteral(d.description, "en"))
          case None => {
            model.add(dataset, model.createProperty(model.getNsPrefixURI("dc"), "description"), model.createLiteral("DBpedia dataset " + datasetName + ", subset of " + topset.getLocalName, "en"))
            err.println("Could not find description for dataset: " + lang.wikiCode.replace("-", "_") + "/" + currentFile)
          }
        }
      }

      model.add(dataset, model.createProperty(model.getNsPrefixURI("dcat"), "landingPage"), model.createResource("http://dbpedia.org/"))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("foaf"), "page"), model.createResource(documentation))
      //TODO done by DataId Hub
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dc"), "hasVersion"), versionStatement)
      //TODO model.add(dataset, model.createProperty(model.getNsPrefixURI("dataid"), "latestVersion"), dataset)
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dataid"), "associatedAgent"), associatedAgent)
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dc"), "modified"), model.createTypedLiteral(dateformat.format(new Date()), model.getNsPrefixURI("xsd") + "date"))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dc"), "issued"), model.createTypedLiteral(dateformat.format(new Date()), model.getNsPrefixURI("xsd") + "date"))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dc"), "license"), model.createResource(license))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dc"), "publisher"), associatedAgent)
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dcat"), "keyword"), model.createLiteral("DBpedia", "en"))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dcat"), "keyword"), model.createLiteral(datasetName, "en"))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dc"), "conformsTo"), dataidStandard)
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dc"), "conformsTo"), dataidLdStandard)

      if (lang.iso639_3 != null && lang.iso639_3.length > 0)
        model.add(dataset, model.createProperty(model.getNsPrefixURI("dc"), "language"), model.createResource("http://lexvo.org/id/iso639-3/" + lang.iso639_3))

      lbpMap.get("core-i18n/" + lang.wikiCode.replace("-", "_") + "/" + currentFile) match {
        case Some(triples) =>
          model.add(dataset, model.createProperty(model.getNsPrefixURI("void"), "triples"), model.createTypedLiteral(new Integer(triples.get("lines").get) - 2, model.getNsPrefixURI("xsd") + "integer"))
        case None =>
      }
      dataset
    }

    //TODO links...
    //visit all subdirectories, determine if its a dbpedia language dir, and create a DataID for this language
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

  def stringCompareIgnoreDash(str1: String, str2: String): Boolean = {
    val s1 = str1.trim.toLowerCase()
    val s2 = str2.trim.toLowerCase()
    val zw = s1.replace("-", "_") == s2.replace("-", "_")
    zw
  }
}
package org.dbpedia.extraction.scripts

import java.io._
import java.net.URI
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.logging.{Level, Logger}

import com.hp.hpl.jena.rdf.model.{Model, ModelFactory, Resource}
import com.hp.hpl.jena.vocabulary.RDF
import org.apache.jena.atlas.json.{JSON, JsonObject}

import scala.collection.JavaConverters._

/**
 * Created by Chile on 1/8/2016.
 */
object DataIdGenerator {

  val dateformat = new SimpleDateFormat("yyyy-MM-dd")
  def main(args: Array[String]) {

    require(args != null && args.length >= 1,
      "need three args: " +
        /*0*/ "config file location"
    )

    val source = scala.io.Source.fromFile(args(0))
    val jsonString = source.mkString
    source.close()

    val configMap = JSON.parse(jsonString)
    var uri: Resource = null
    var dataset: Resource = null
    var topset: Resource = null

    val logger = Logger.getLogger(getClass.getName)

    // Collect arguments
    val webDir = configMap.get("webDir").getAsString.value()
    require(URI.create(webDir) != null, "Please specify a valid web directory!")

    val dump = new File(configMap.get("localDir").getAsString.value)
    require(dump.isDirectory() && dump.canRead(), "Please specify a valid local dump directory!")

    val compression = configMap.get("fileExtension").getAsString.value
    require(compression.startsWith("."), "please provide a valid file extension starting with a dot")

    val extensions = configMap.get("serializations").getAsArray.subList(0,configMap.get("serializations").getAsArray.size()).asScala
    require(extensions.map(x => x.getAsString.value().startsWith(".")).foldLeft(true)(_ && _), "list of valid serialization extensions starting with a dot")

     require(!configMap.get("outputFileTemplate").getAsString.value.contains("."), "Please specify a valid output file name without extension")

    val dbpVersion = configMap.get("dbpediaVersion").getAsString.value
    val idVersion = configMap.get("dataidVersion").getAsString.value
    val vocabulary = configMap.get("vocabularyUri").getAsString.value
    require(URI.create(vocabulary) != null, "Please enter a valid ontology uri of ths DBpedia release")

    val license = configMap.get("licenseUri").getAsString.value
    require(URI.create(license) != null, "Please enter a valid license uri (odrl license)")

    val defaultModel = ModelFactory.createDefaultModel()

    addPrefixes(defaultModel)

    for(dir <- dump.listFiles())
    {
      if(dir.isDirectory)
      {
        val lang = dir.getName
        val subModel = defaultModel.difference(ModelFactory.createDefaultModel())
        val model = ModelFactory.createDefaultModel()

        val outfile = new File(dump + "/" + lang + "/" + configMap.get("outputFileTemplate").getAsString.value + "_" + lang + ".ttl")
        addPrefixes(subModel)
        addPrefixes(model)

        val filterstring = ("^[^$]+_" + lang + "(" + extensions.foldLeft(new StringBuilder){ (sb, s) => sb.append("|" + s.getAsString.value()) }.toString.substring(1) + ")" + compression).replace(".", "\\.")
        val filter = new FilenameFilter {
          override def accept(dir: File, name: String): Boolean = {
            if(name.matches(filterstring))
              return true
            else
              return false
          }
        }

        uri = subModel.createResource(webDir + lang + "/" + configMap.get("outputFileTemplate").getAsString.value + "_" + lang + ".ttl")
        require(uri != null, "Please provide a valid directory")
        subModel.add(uri, RDF.`type`, subModel.createResource(subModel.getNsPrefixURI("dataid") + "DataId"))

        val creator = addAgent(subModel, lang, configMap.get("creator").getAsObject)
        val maintainer = addAgent(subModel, lang, configMap.get("maintainer").getAsObject)
        val contact = addAgent(subModel, lang, configMap.get("contact").getAsObject)
        require(creator != null, "Please define an dataid:Agent as a Creator in the dataid stump file (use AuthorityEntityContext).")

        subModel.add(uri, subModel.createProperty(subModel.getNsPrefixURI("dc"), "modified"), subModel.createTypedLiteral(dateformat.format(new Date()), model.getNsPrefixURI("xsd") + "date" ))
        subModel.add(uri, subModel.createProperty(subModel.getNsPrefixURI("dc"), "issued"), subModel.createTypedLiteral(dateformat.format(new Date()), model.getNsPrefixURI("xsd") + "date" ))
        subModel.add(uri, subModel.createProperty(subModel.getNsPrefixURI("dc"), "hasVersion"), subModel.createLiteral(idVersion))
        subModel.add(uri, subModel.createProperty(subModel.getNsPrefixURI("dataid"), "hasAccessLevel"), subModel.createResource(subModel.getNsPrefixURI("dataid") + "PublicAccess"))
        subModel.add(uri, subModel.createProperty(subModel.getNsPrefixURI("dataid"), "latestVersion"), uri)
        subModel.add(uri, subModel.createProperty(subModel.getNsPrefixURI("dataid"), "associatedAgent"), creator)
        subModel.add(uri, subModel.createProperty(subModel.getNsPrefixURI("dataid"), "associatedAgent"), maintainer)
        subModel.add(uri, subModel.createProperty(subModel.getNsPrefixURI("dataid"), "associatedAgent"), contact)

        addDataset(subModel, lang, "dataset", creator, true)
        topset = dataset

        subModel.add(uri, subModel.createProperty(subModel.getNsPrefixURI("foaf"), "primaryTopic"), topset)
        subModel.add(topset, subModel.createProperty(subModel.getNsPrefixURI("void"), "vocabulary"), subModel.createResource(vocabulary))
        subModel.add(topset, subModel.createProperty(subModel.getNsPrefixURI("dc"), "description"), subModel.createLiteral(configMap.get("description").getAsString.value, "en"))

        if((configMap.get("addDmpProps").getAsBoolean.value()))
          addDmpStatements(subModel, topset)

        val distributions = dir.listFiles(filter).map(x => x.getName).toList.sorted
        var lastFile: String = null
        for(dis <- distributions)
        {
          if(lastFile != dis.substring(0, dis.lastIndexOf("_")))
          {
            lastFile = dis.substring(0, dis.lastIndexOf("_"))
            addDataset(model, lang, lastFile, creator)
            subModel.add(topset, model.createProperty(model.getNsPrefixURI("void"), "subset"), dataset)
          }
          addDistribution(model, lang, dis, creator)
        }

        subModel.write(new FileOutputStream(outfile), "TURTLE")
        val baos = new ByteArrayOutputStream()
        model.write(baos, "TURTLE")
        var outString = new String( baos.toByteArray(), Charset.defaultCharset())
        outString = outString.replaceAll("(@prefix).*\\n", "")
        val os = new FileOutputStream(outfile, true)
        val printStream = new PrintStream(os)
        printStream.print(outString)
        printStream.close()
        logger.log(Level.INFO, "finished DataId: " + outfile.getAbsolutePath)
      }
    }

    def addDmpStatements(model: Model, dataset: Resource): Unit =
    {
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dmp"), "usefulness"), model.createLiteral(configMap.get("dmpusefulness").getAsString.value, "en"))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dmp"), "similarData"), model.createLiteral(configMap.get("dmpsimilarData").getAsString.value, "en"))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dmp"), "reuseAndIntegration"), model.createLiteral(configMap.get("dmpreuseAndIntegration").getAsString.value, "en"))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dmp"), "additionalSoftware"), model.createLiteral(configMap.get("dmpadditionalSoftware").getAsString.value, "en"))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dmp"), "repositoryUrl"), model.createResource(configMap.get("dmprepositoryUrl").getAsString.value))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dmp"), "growth"), model.createLiteral(configMap.get("dmpgrowth").getAsString.value, "en"))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dmp"), "archiveLink"), model.createResource(configMap.get("dmparchiveLink").getAsString.value))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dmp"), "preservation"), model.createLiteral(configMap.get("dmppreservation").getAsString.value, "en"))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dmp"), "openness"), model.createLiteral(configMap.get("dmpopenness").getAsString.value, "en"))
    }

    def addAgent(model: Model, lang: String, agentMap: JsonObject): Resource =
    {
      val agent = model.createResource(agentMap.get("uri").getAsString.value())
      model.add(agent, RDF.`type`, model.createResource(model.getNsPrefixURI("dataid") + "Agent"))
      model.add(agent, model.createProperty(model.getNsPrefixURI("foaf"), "name"), model.createLiteral(agentMap.get("name").getAsString.value()))
      if(agentMap.get("homepage") != null)
        model.add(agent, model.createProperty(model.getNsPrefixURI("foaf"), "homepage"), model.createResource(agentMap.get("homepage").getAsString.value()))
      model.add(agent, model.createProperty(model.getNsPrefixURI("foaf"), "mbox"), model.createLiteral(agentMap.get("mbox").getAsString.value()))

      val context = model.createResource(webDir + lang + "/dataid.ttl?subj=" + agentMap.get("role").getAsString.value().toLowerCase + "Context")
      model.add(context, RDF.`type`, model.createResource(model.getNsPrefixURI("dataid") + "AuthorityEntityContext"))
      model.add(context, model.createProperty(model.getNsPrefixURI("dataid"), "authorizedAgent"), agent)
      model.add(context, model.createProperty(model.getNsPrefixURI("dataid"), "authorityAgentRole"), model.createResource(model.getNsPrefixURI("dataid") + agentMap.get("role").getAsString.value()))
      model.add(context, model.createProperty(model.getNsPrefixURI("dataid"), "isInheritable"), model.createTypedLiteral("true", model.getNsPrefixURI("xsd") + "boolean" ))
      model.add(context, model.createProperty(model.getNsPrefixURI("dataid"), "authorizedFor"), uri)

      agent
    }

    def addDataset(model: Model, lang: String, currentFile: String, associatedAgent: Resource, toplevelSet: Boolean = false): Unit =
    {
      dataset = model.createResource(uri.getURI + "?set=" + currentFile)
      model.add(dataset, RDF.`type`, model.createResource(model.getNsPrefixURI("dataid") + "Dataset"))
      if(!toplevelSet) //not!
      {
        model.add(dataset, model.createProperty(model.getNsPrefixURI("void"), "rootResource"), topset)
      }
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dc"), "title"), model.createLiteral("DBpedia " + dbpVersion + " " + currentFile.substring(currentFile.lastIndexOf("/") +1) + (if(lang != null) {" " + lang} else "") + " dump dataset", "en"))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("rdfs"), "label"), model.createLiteral(currentFile.substring(currentFile.lastIndexOf("/") +1) + (if(lang != null) {"_" + lang} else "") + "_" + dbpVersion, "en"))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dcat"), "landingPage"), model.createResource("http://dbpedia.org/"))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dc"), "hasVersion"), model.createLiteral(idVersion))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dataid"), "hasAccessLevel"), model.createResource(model.getNsPrefixURI("dataid") + "PublicAccess"))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dataid"), "associatedAgent"), associatedAgent)
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dc"), "modified"), model.createTypedLiteral(dateformat.format(new Date()), model.getNsPrefixURI("xsd") + "date") )
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dc"), "issued"), model.createTypedLiteral(dateformat.format(new Date()), model.getNsPrefixURI("xsd") + "date") )
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dc"), "license"), model.createResource(license))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dcat"), "keyword"), model.createLiteral("DBpedia", "en"))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dcat"), "keyword"), model.createLiteral(currentFile, "en"))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dc"), "language"), model.createResource("http://lexvo.org/id/iso639-3/" + lang))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dataid"), "latestVersion"), dataset)
    }

    def addDistribution(model: Model, lang: String, currentFile: String, associatedAgent: Resource): Unit =
    {
      val dist = model.createResource(uri.getURI + "?file=" + currentFile)
      model.add(dist, RDF.`type`, model.createResource(model.getNsPrefixURI("dataid") + "SingleFile"))
      model.add(dataset, model.createProperty(model.getNsPrefixURI("dcat"), "distribution"), dist)
      model.add(dist, model.createProperty(model.getNsPrefixURI("dc"), "title"), model.createLiteral("DBpedia " + dbpVersion + " " + currentFile.substring(currentFile.lastIndexOf("/") +1) + (if(lang != null) {" " + lang} else "") + " dump dataset", "en"))
      model.add(dist, model.createProperty(model.getNsPrefixURI("dc"), "description"), model.createLiteral("DBpedia dump file: " + currentFile.substring(currentFile.lastIndexOf("/") +1), "en"))
      model.add(dist, model.createProperty(model.getNsPrefixURI("rdfs"), "label"), model.createLiteral(currentFile.substring(currentFile.lastIndexOf("/") +1) + (if(lang != null) {"_" + lang} else "") + "_" + dbpVersion, "en"))
      model.add(dist, model.createProperty(model.getNsPrefixURI("dc"), "hasVersion"), model.createLiteral(idVersion))
      model.add(dist, model.createProperty(model.getNsPrefixURI("dataid"), "hasAccessLevel"), model.createResource(model.getNsPrefixURI("dataid") + "PublicAccess"))
      model.add(dist, model.createProperty(model.getNsPrefixURI("dataid"), "associatedAgent"), associatedAgent)
      model.add(dist, model.createProperty(model.getNsPrefixURI("dc"), "modified"), model.createTypedLiteral(dateformat.format(new Date()), model.getNsPrefixURI("xsd") + "date") )
      model.add(dist, model.createProperty(model.getNsPrefixURI("dc"), "issued"), model.createTypedLiteral(dateformat.format(new Date()), model.getNsPrefixURI("xsd") + "date") )
      model.add(dist, model.createProperty(model.getNsPrefixURI("dc"), "license"), model.createResource(license))
      model.add(dist, model.createProperty(model.getNsPrefixURI("dataid"), "latestVersion"), dist)
      val f = new File(dump + "\\" + lang + "\\" + currentFile)
      logger.log(Level.INFO, "file scanned: " + f.getAbsolutePath)
      model.add(dist, model.createProperty(model.getNsPrefixURI("dcat"), "byteSize"), model.createTypedLiteral(f.length.toString, model.getNsPrefixURI("xsd") + "integer") )
      model.add(dist, model.createProperty(model.getNsPrefixURI("dcat"), "downloadURL"), model.createResource(webDir + lang + "/" + currentFile))
      model.add(dist, model.createProperty(model.getNsPrefixURI("dcat"), "mediaType"), model.createLiteral(if(compression.contains("gz")) "application/x-gzip" else if(compression.contains("bz2")) "application/x-bzip2" else ""))
      val postfix = dist.getURI.substring(dist.getURI.lastIndexOf("_"))
      model.add(dist, model.createProperty(model.getNsPrefixURI("dcat"), "format"), model.createLiteral(if(postfix.contains(".ttl")) "text/turtle" else if(postfix.contains(".tql") || postfix.contains(".nq")) "application/n-quads" else if(postfix.contains(".nt")) "application/n-triples" else ""))
    }

    def addPrefixes(model: Model): Unit =
    {
      model.setNsPrefix("dataid", "http://dataid.dbpedia.org/ns/core#")
      model.setNsPrefix("dc", "http://purl.org/dc/terms/")
      model.setNsPrefix("dcat", "http://www.w3.org/ns/dcat#")
      model.setNsPrefix("void", "http://rdfs.org/ns/void#")
      model.setNsPrefix("prov", "http://www.w3.org/ns/prov#")
      model.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#")
      model.setNsPrefix("owl", "http://www.w3.org/2002/07/owl#")
      model.setNsPrefix("foaf", "http://xmlns.com/foaf/0.1/")
      model.setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#")
      model.setNsPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
      model.setNsPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#")
      model.setNsPrefix("dmp", "http://dataid.dbpedia.org/ns/dmp#")
    }
  }
}

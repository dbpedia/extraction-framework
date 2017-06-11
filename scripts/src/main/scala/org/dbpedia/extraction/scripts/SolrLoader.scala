package org.dbpedia.extraction.scripts

import java.io.File
import java.util.concurrent.ConcurrentHashMap

import aksw.org.sdw.kg.handler.solr.{KgSorlInputDocument, SolrHandler, SolrUriInputDocument}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.util.RichFile._

import scala.collection.concurrent
import scala.collection.mutable.ListBuffer
import scala.collection.convert.decorateAsScala._
import scala.collection.convert.decorateAsJava._

/**
  * Created by chile on 07.06.17.
  */
object SolrLoader {

  private val rdfsLabel = RdfNamespace.fullUri(RdfNamespace.RDFS, "label")
  private val rdfsComment = RdfNamespace.fullUri(RdfNamespace.RDFS, "comment")
  private val foafName = RdfNamespace.fullUri(RdfNamespace.FOAF, "name")      //=> altLabel
  private val foafNick = RdfNamespace.fullUri(RdfNamespace.FOAF, "nick")
  private val rdfType = RdfNamespace.fullUri(RdfNamespace.RDF, "type")
  private val dctSubject = RdfNamespace.fullUri(RdfNamespace.DCT, "subject")
  private val owlSameAs = RdfNamespace.fullUri(RdfNamespace.OWL, "sameAs")
  private val dboAltTitle = RdfNamespace.fullUri(RdfNamespace.DBO, "alternativeTitle")
  private val dboTitle = RdfNamespace.fullUri(RdfNamespace.DBO, "title")
  private val dboTag = RdfNamespace.fullUri(RdfNamespace.DBO, "tag")
  private val dboOrigTitle = RdfNamespace.fullUri(RdfNamespace.DBO, "originalTitle")
  private val dboSubTitle = RdfNamespace.fullUri(RdfNamespace.DBO, "subtitle")
  private val skosExact = RdfNamespace.fullUri(RdfNamespace.SKOS, "exactMatch")

  private val disambiguations: concurrent.Map[String, ListBuffer[String]] = new ConcurrentHashMap[String, ListBuffer[String]]().asScala
  private val redirects: concurrent.Map[String, ListBuffer[String]] = new ConcurrentHashMap[String, ListBuffer[String]]().asScala
  private var finder: DateFinder[File] = _
  private var suffix: String = _
  private var solrHandler: SolrHandler = _
  private var nonCommittedDocs: Int = 0

  private val docQueue = new ListBuffer[KgSorlInputDocument]()

  private def disambWorker = SimpleWorkers(1.5, 1.0) { language: Language =>
    finder.byName(DBpediaDatasets.DisambiguationLinks.filenameEncoded + suffix, auto = true) match{
      case Some(x) if x.exists => new QuadMapper().readQuads(language, x) { quad =>
        disambiguations.get(quad.value) match{
          case Some(l) => l.append(quad.subject)
          case None => {
            val zw = new ListBuffer[String]()
              zw.append(quad.subject  )
            disambiguations.put(quad.value, zw)
          }
        }
      }
      case _ => Console.err.println("No disambiguation dataset found for language: " + language.wikiCode)
    }
  }

  private def redirectWorker = SimpleWorkers(1.5, 1.0) { language: Language =>
    finder.byName(DBpediaDatasets.Redirects.filenameEncoded + suffix, auto = true) match{
      case Some(x) if x.exists => new QuadMapper().readQuads(language, x) { quad =>
        redirects.get(quad.value) match{
          case Some(l) => l.append(quad.subject)
          case None => {
            val zw = new ListBuffer[String]()
            zw.append(quad.subject  )
            disambiguations.put(quad.value, zw)
          }
        }
      }
      case _ => Console.err.println("No redirect dataset found for language: " + language.wikiCode)
    }
  }


  def main(args: Array[String]): Unit = {

    require(args != null && args.length == 1, "One arguments required, extraction config file")

    val config = new Config(args(0))

    val baseDir = config.dumpDir
    require(baseDir.isDirectory && baseDir.canRead && baseDir.canWrite, "Please specify a valid local base extraction directory - invalid path: " + baseDir)

    val language = config.languages(0)
    finder = new DateFinder(baseDir, language)
    suffix = config.inputSuffix match{
      case Some(x) => x
      case None => throw new IllegalArgumentException("no suffix provided!")
    }

    solrHandler = new SolrHandler(ConfigUtils.getString(config, "solr-url"))
    val leadFile = new RichFile(finder.byName(ConfigUtils.getString(config, "solr-lead-file") + suffix, auto = true).get)
    val sortedInputs = ConfigUtils.getValues(config, "solr-sorted-inputs",",")(x => new RichFile(finder.byName(x + suffix, auto = true).get))

    val logfile = config.logDir match{
      case Some(f) if f.exists() => {
        val file = new File(f, "solarImport.log")
        file.createNewFile()
        new RichFile(file)
      }
      case None => null
    }

    solrHandler.deleteAllDocuments()

    Workers.workInParallel[Language](Array(disambWorker, redirectWorker), List(language))

    new QuadReader(logfile, 2000, " Documents imported into Solr.").readSortedQuads(language, leadFile, sortedInputs){ quads =>
      val doc = new SolrUriInputDocument(quads.head.subject)
      val altlabels = new ListBuffer[String]()
      val sameass = new ListBuffer[String]()
      val subjects = new ListBuffer[String]()
      val types = new ListBuffer[String]()

      var labelAdded = false
      var commentAdded = false

      for(quad <- quads){
        quad.predicate match{
          case `rdfsLabel` => {
            if (!labelAdded){
              doc.addFieldData("label", quad.value)
              labelAdded = true
            }
            else
              altlabels.append(quad.value)
          }
          case `rdfsComment` if !commentAdded => {
            doc.addFieldData("comment", quad.value)
            commentAdded = true
          }
          case `rdfType` => doc.addFieldData("type", List(WikiUtil.wikiDecode(quad.value.substring(quad.value.lastIndexOf("/")+1))).asJava)  //TODO add other types?
          case `dctSubject` => subjects.append(WikiUtil.wikiDecode(quad.value.substring(quad.value.lastIndexOf("/")+1)))
          case `owlSameAs` | `skosExact` => sameass.append(WikiUtil.wikiDecode(quad.value.substring(quad.value.lastIndexOf("/")+1)))
          case `foafName` | `foafNick` | `dboSubTitle` | `dboOrigTitle` | `dboTag` | `dboTitle` | `dboAltTitle` => altlabels.append(quad.value)
          case _ => if(quad.predicate.startsWith(RdfNamespace.DBO.toString) && quad.predicate.toLowerCase().contains("name")) altlabels.append(quad.value)
        }
      }

      doc.addFieldData("id", doc.getId)
      doc.addFieldData("titel", WikiUtil.wikiDecode(doc.getId.substring(doc.getId.lastIndexOf("/")+1)))

      doc.addFieldData("altLabel", altlabels.distinct.toList.asJava)
      doc.addFieldData("sameAs", sameass.distinct.toList.asJava)
      doc.addFieldData("subjects", subjects.distinct.toList.asJava)

      redirects.get(doc.getId) match{
        case Some(x) => doc.addFieldData("redirects", x.map( y => WikiUtil.wikiDecode(y.substring(y.lastIndexOf("/")+1))).asJava)
        case None =>
      }

      disambiguations.get(doc.getId) match{
        case Some(x) => doc.addFieldData("disambiguations", x.map( y => WikiUtil.wikiDecode(y.substring(y.lastIndexOf("/")+1))).asJava)
        case None =>
      }

      nonCommittedDocs = nonCommittedDocs +1
      if(nonCommittedDocs % 1000 == 0){
        docQueue.append(doc)
        solrHandler.addSolrDocuments(docQueue.toList.asJava)
        docQueue.clear()
        System.out.println(nonCommittedDocs + " documents were successfully imported.")
      }
      else
        docQueue.append(doc)
    }
    System.out.println(nonCommittedDocs + " documents were successfully imported.")
  }
}

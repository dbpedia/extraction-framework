package org.dbpedia.extraction.scripts

import java.io.File
import java.util.regex.Matcher

import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.destinations.{CompositeDestination, Destination, WriterDestination}
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.IOUtils._
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util._

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/** Generates language links from the Wikidata sameAs dataset as created by the
  * [[org.dbpedia.extraction.mappings.WikidataSameAsExtractor]]. This code assumes the subjects to be
  * ordered, in particular, it assumes that there is *exactly* one continuous block for each subject.
  */
object WikidataSameAsToLanguageLinks_test {
  private val sameAs = RdfNamespace.OWL.append("sameAs")
  private val DBPEDIA_URI_PATTERN = "^http://([a-z-]+.)?dbpedia.org/resource/.*$".r.pattern


  def main(args: Array[String]): Unit = {
    require(args != null && args.length == 1 && args(0).nonEmpty, "missing required argument: config file name")

    val config = new Config(args(0))

    val baseDir = config.dumpDir
    if (!baseDir.exists) {
      throw error("dir " + baseDir + " does not exist")
    }

    val inputFinder = new Finder[File](baseDir, Language.Wikidata, "wiki")
    val date = inputFinder.dates().last

    val suffix = config.inputSuffix match{
      case Some(x) => x
      case None => throw new IllegalArgumentException("Please provide a 'suffix' attribute in your properties configuration")
    }

    val output = DBpediaDatasets.TestDataset.filenameEncoded
/*
      config.outputDataset match{
      case Some (l) => l
      case None => throw new IllegalArgumentException("Please provide an 'output' attribute for the output dataset file in the .properties configuration.")
    }*/

    val language = config.languages

    val policies = config.policies
    val formats = config.formats

    val input = config.inputDatasets.headOption.getOrElse(throw new IllegalArgumentException("Please provide an 'input' attribute for the wikidata input file in the .properties configuration."))

    // find the input wikidata file
    val wikiDataFile: RichFile = inputFinder.file(date, input + suffix).get

    val processor = new WikidataSameAsToLanguageLinks_test(baseDir, wikiDataFile, output, language, formats)
    processor.processLinks()
  }

  private def error(message: String, cause: Throwable = null): IllegalArgumentException = {
    new IllegalArgumentException(message, cause)
  }
}


class WikidataSameAsToLanguageLinks_test(val baseDir: File, val wikiDataFile: FileLike[_],
                                           val output: String, val languages: Array[Language],
                                           val formats: collection.Map[String, Formatter]) {
  import WikidataSameAsToLanguageLinks_test._

  private val relevantLanguages: Set[String] = languages.map(_.wikiCode).toSet
  private val destinations = setupDestinations()
  private var currentSameEntities = new mutable.HashMap[Language, EntityContext]()

  /**
    * Starts the generation of the inter-language links from sameAs information contained in the given
    * wikiDataFile.
    */
  def processLinks(): Unit = {
    destinations.foreach(_._2.open())
    // init workers
    val workers = SimpleWorkers(1.5, 1.5) { job : JobEntity =>
      job.sameEntities.get(job.language) match {
        case Some(currentEntity) =>
          // generate quads for the current language and prepend the sameAs statement quad to the
          // wikidata entity
          var quads = List[Quad]()
          quads :::= job.sameEntities.filterKeys(_ != job.language).toList.sortBy(_._1).map {
            case (language, context) => new Quad(language.wikiCode, null, currentEntity.entityUri, sameAs, context.entityUri, context.context, null: String)
          }
          quads ::= new Quad(job.language.wikiCode, null, currentEntity.entityUri, sameAs, job.wikiDataEntity, currentEntity.context, null: String)
          quads ::= new Quad(job.language.wikiCode, null, currentEntity.entityUri, sameAs, getWikidataUri(job.wikiDataEntity), currentEntity.context, null: String)
          destinations(job.language).write(quads)
        case _ => // do not write anything when there is no entity in the current language
      }
    }
    // start workers and process quads in sets
    workers.start()
    new QuadMapper().readSortedQuads(Language.Wikidata, wikiDataFile) { quads: Traversable[Quad] =>
      if(quads.nonEmpty){
        currentSameEntities = new mutable.HashMap[Language, EntityContext]()
        quads.foreach(extractLanguageAndStore)
        relevantLanguages.foreach(language =>
          workers.process(new JobEntity(Language(language), new String(quads.head.subject), currentSameEntities)))
      }
    }
    // close workers & destinations
    workers.stop()
    destinations.foreach(_._2.close())
    }


  /**
    * Ensures that the URI follows the dbpedia URI pattern and then extracts its language
    * Since we want to keep our set of currentSameEntities in the memory,
    * we cut the link to the quads by generating ne Strings for value and context to safe some memory.
    */
  def extractLanguageAndStore(quad: Quad): Unit = {
    val value = new String(quad.value)
    // since context is optional, we need to check for null values
    var context = ""
    if(quad.context != null){
      context = new String(quad.context)
    }
    val matcher: Matcher = DBPEDIA_URI_PATTERN.matcher(quad.value)
    if (!matcher.matches()) {
      error("Non-DBpedia URI found in sameAs statement of Wikidata sameAs links!")
    }
    else {
      val lang = matcher.group(1)
      if (lang == null) {
        // URI starts with http://dbpedia.org..
        currentSameEntities(Language.English) = new EntityContext(value, context)
      }
      else {
        // non-English URI ==> store entity and context in list
        if (relevantLanguages.contains(lang.replace(".", ""))) {
          currentSameEntities(Language(lang.replace(".", ""))) = new EntityContext(value, context)
        }
      }
    }
  }

  /**
    * Builds the wikidata.org URI for the given wikidata.dbpedia.org URI
    */
  def getWikidataUri(entity: String) : String = {
    val wikidataName = entity.split("/").last
    s"http://www.wikidata.org/entity/$wikidataName"
  }

  /**
    * Sets up the destinations for the relevant languages in all configured formats but does not yet open
    * the destinations.
    */
  private def setupDestinations(): Map[Language, Destination] = {
    var destinations = Map[Language, Destination]()
    for (currentLanguage <- languages) {
      val outputFinder = new Finder[File](baseDir, currentLanguage, "wiki")
      val outputDate = outputFinder.dates().last
      val formatDestinations = new ArrayBuffer[Destination]()
      for ((suffix, format) <- formats) {
        val file = outputFinder.file(outputDate, output + '.' + suffix).get
        formatDestinations += new WriterDestination(() => writer(file), format)
      }
      destinations += currentLanguage -> new CompositeDestination(formatDestinations: _*)
    }
    destinations
  }

  /**
    * Job-object for our workers.
    * @param language current language the worker should generate quads for
    * @param wikiDataEntity the Entity of our current set of quads
    * @param sameEntities Entities in owl:sameAs relation to the wikiDataEntity
    */
  private class JobEntity(val language: Language, val wikiDataEntity: String, val sameEntities: mutable.HashMap[Language, EntityContext])

  /**
    * Represents the combination of an entity URI which is assigned to some wikidata entity by means
    * of owl:sameAs and the context in which this statement is made.
    *
    * @param entityUri URI of the entity
    * @param context context in which this entity's sameAs statement is given
    */
  private class EntityContext(val entityUri: String, val context: String)

}

package org.dbpedia.extraction.scripts

import java.io.File
import java.util.regex.Matcher

import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.destinations.formatters.UriPolicy.parseFormats
import org.dbpedia.extraction.destinations.{CompositeDestination, Destination, Quad, WriterDestination}
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.scripts.WikidataSameAsToLanguageLinks.{DBPEDIA_URI_PATTERN, error, sameAs}
import org.dbpedia.extraction.util.ConfigUtils._
import org.dbpedia.extraction.util.IOUtils._
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util._

import scala.collection.mutable.ArrayBuffer

/**
  * ChangeLog (compared to original Class):
  * - different logic behind the worker setup
  *   -> instead of opening all and then passing the data to them,
  *      now we build new workers every time the QuadBuffer threshold is reached (size configurable, default 500)
  * - decluttered the processLinks Method
  *   -> put the language extraction and the dbpedia Pattern check into a separate method.
  */
object WikidataSameAsToLanguageLinks {
  private val sameAs = RdfNamespace.OWL.append("sameAs")
  private val DBPEDIA_URI_PATTERN = "^http://([a-z-]+.)?dbpedia.org/resource/.*$".r.pattern

  def main(args: Array[String]): Unit = {
    require(args != null && args.length == 1 && args(0).nonEmpty, "missing required argument: config file name")
    // Load Config
    val config = loadConfig(args(0), "UTF-8")

    val baseDir = getValue(config, "base-dir", required = true)(new File(_))
    if (!baseDir.exists) {
      throw error("dir " + baseDir + " does not exist")
    }

    // Get config contents
    val inputFinder = new Finder[File](baseDir, Language.Wikidata, "wiki")
    val date   = inputFinder.dates().last
    val input  = getString(config, "input", required = true)
    val suffix = getString(config, "suffix", required = true)
    val output = getString(config, "output", required = true)
    val language = parseLanguages(baseDir, getStrings(config, "languages", ',', required = true))
    val formats: collection.Map[String, Formatter] = parseFormats(config, "uri-policy", "format")
    var quadBuffer = 500
    Option(getString(config, "quadBufferSize", required = false)).map(Integer.parseInt(_)).foreach(quadBuffer = _)

    // find the input wikidata file
    val wikiDataFile: RichFile = inputFinder.file(date, input + suffix).get
    // Calling the working method
    val processor = new WikidataSameAsToLanguageLinks(baseDir, wikiDataFile, output, language, formats, quadBuffer)
    processor.processLinks()
  }
  
  private def error(message: String, cause: Throwable = null): IllegalArgumentException = {
    new IllegalArgumentException(message, cause)
  }
}


class WikidataSameAsToLanguageLinks(val baseDir: File, val wikiDataFile: FileLike[_],
                                           val output: String, val languages: Array[Language],
                                           val formats: collection.Map[String, Formatter],
                                           val JobListSize : Integer) {
  private val relevantLanguages: Set[String] = languages.map(_.wikiCode).toSet
  private val destinations = setupDestinations()
  private var openDestinations = List[String]()
  private val quadBufferSize = JobListSize

  /**
    * 
    */
  def processLinks(): Unit = {
    destinations.foreach(_._2.open())
    // init jobMap
    var jobMap : Map[String, List[Quad]] = Map()
    relevantLanguages.foreach(language => jobMap += language -> List[Quad]())

    // stores the currently processed wikidata entity to recognize when the current block is fully read
    var currentWikidataEntity: Option[String] = None
    // all entities assigned to the current wikidata entity by means of sameAs
    var currentSameEntities: Map[String, EntityContext] = Map()
    var quadsInBuffer = 0
    try {
      QuadReader.readQuads(Language.Wikidata.wikiCode, wikiDataFile) { quad =>
        val currentSubject = new String(quad.subject)

        currentWikidataEntity match {
          case None =>
            // we have not yet read any data, start from scratch
            currentWikidataEntity = Some(currentSubject)
            // add the Entity with its language to the list of entities that are the same as our current subject.
            currentSameEntities += extractLanguage(quad.value) -> new EntityContext(quad.value, quad.context)

          case Some(subject) if subject == currentSubject =>
            // still at the current subject => collect entity and language of the new quad
            currentSameEntities += extractLanguage(quad.value) -> new EntityContext(quad.value, quad.context)

          case Some(subject) =>
            // subject of the new quad is a new WikiDataEntity => 
            // 1.  generate quads for each language
            // 1b. write quads and clear the jobMap if the maximum quadBuffer size is reached
            // 2.  clear the list of same entities, and continue with the new entity

            // fill jobs for each language
            relevantLanguages.foreach { language =>
              val newQuads = (generateQuads(language, subject, currentSameEntities))
              jobMap += language -> (jobMap(language) ::: newQuads)
              quadsInBuffer += newQuads.length
            }

            // maximum QuadBuffer size reached => write all and clear the jobMap
            if (quadsInBuffer >= quadBufferSize) {
              writeJobs(jobMap)
              jobMap = Map()
              relevantLanguages.foreach(language => jobMap += language -> List[Quad]())
              quadsInBuffer = 0
            }

            // begin to fill the sameEntities List for the new subject
            currentWikidataEntity = Some(currentSubject)
            currentSameEntities = Map()
            currentSameEntities += extractLanguage(quad.value) -> new EntityContext(quad.value, quad.context)
        }
      }

      // Everything read => fill last set of Jobs and write them to their destinations
      if (currentWikidataEntity.isDefined) {
        relevantLanguages.foreach { language =>
          val newQuads = (generateQuads(language, currentWikidataEntity.get, currentSameEntities))
          jobMap(language) ::: newQuads
          quadsInBuffer += newQuads.length
        }
      }
      writeJobs(jobMap)
    }
    // make sure every destination is closed, even if we encounter an error
    finally destinations.foreach(_._2.close())
  }

  /**
    * Ensures that the URI follows the dbpedia URI pattern and then extracts its language
    */
  private def extractLanguage(uri : String): String = {
    var language = "en" // if no language is specified in the URI itself -> en
    val matcher: Matcher = DBPEDIA_URI_PATTERN.matcher(uri)
    if (!matcher.matches()) {
      error("Non-DBpedia URI found in sameAs statement of Wikidata sameAs links!")
    }
    else {
      val lang = matcher.group(1)
      if (lang != null) {
        if (relevantLanguages.contains(lang.replace(".", ""))) {
          language = lang.replace(".", "")
        }
      }
    }
    language
  }

  /**
    * Builds the wikidata.org URI for the given wikidata.dbpedia.org URI
    */
  def getWikidataUri(entity: String): String = {
    val wikidataName = entity.split("/").last
    s"http://www.wikidata.org/entity/$wikidataName"
  }

  /**
   * Create Workers for each relevant Language.
   * Each Worker gets its language-specific List of Quads and writes
   * the files to its language folder
   */
  def writeJobs(jobMap: Map[String, List[Quad]]): Unit = {
    Workers.work(SimpleWorkers(1.5, 1.5) {language : String =>
      destinations(language).write(jobMap(language))
    }, jobMap.keys.toList)
  }

  /**
    * Generates Quads that will be written for the respective language.
    * @param language
    * @param wikiDataEntity current base Entity
    * @param sameEntities Entities that are the same as the wikiDataEntity, just in a different language
    * @return
    */
  private def generateQuads(language : String, wikiDataEntity : String, sameEntities : Map[String, EntityContext]): List[Quad] = {
    var quads = List[Quad]()
    sameEntities.get(language) match {
      case Some(currentEntity) =>
        // generate quads for the current language and prepend the sameAs statement quad to the
        // wikidata entity
        quads :::= sameEntities.filterKeys(_ != language).toList.sortBy(_._1).map { case (language, context) =>
          new Quad(language, null, currentEntity.entityUri, sameAs, context.entityUri, context.context, null: String)
        }
        quads ::= new Quad(language, null, currentEntity.entityUri, sameAs, wikiDataEntity, currentEntity.context,
          null: String)
        quads ::= new Quad(language, null, currentEntity.entityUri, sameAs, getWikidataUri(wikiDataEntity),
          currentEntity.context, null: String)
      case _ => // do not write anything when there is no entity in the current language
    }
    quads
  }

  /**
    * Sets up the destinations for the relevant languages in all configured formats but does not yet open
    * the destinations.
    */
  private def setupDestinations(): Map[String, Destination] = {
    var destinations = Map[String, Destination]()
    for (currentLanguage <- languages) {
      val outputFinder = new Finder[File](baseDir, currentLanguage, "wiki")
      val outputDate = outputFinder.dates().last
      val formatDestinations = new ArrayBuffer[Destination]()
      for ((suffix, format) <- formats) {
        val file = outputFinder.file(outputDate, output + '.' + suffix).get
        formatDestinations += new WriterDestination(() => writer(file), format)
      }
      destinations += currentLanguage.wikiCode -> new CompositeDestination(formatDestinations.toSeq: _*)

    }
    destinations
  }

  private class EntityContext(val entityUri: String, val context: String)
}

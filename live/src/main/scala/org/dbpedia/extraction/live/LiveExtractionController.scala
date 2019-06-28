package org.dbpedia.extraction.live.extraction

import java.net.URL

import org.slf4j.{Logger, LoggerFactory}

import collection.immutable.ListMap
import java.util.Properties
import java.io.File

import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.util.{ExtractorUtils, Language}
import org.dbpedia.extraction.sources.{Source, WikiSource, XMLSource}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.destinations.formatters.UriPolicy
import org.dbpedia.extraction.destinations.formatters.UriPolicy.Policy
import org.dbpedia.extraction.live.config.LiveOptions
import org.dbpedia.extraction.live.config.extractors.{ExtractorStatus, LiveExtractorConfigReader}

import collection.mutable.ArrayBuffer
import org.dbpedia.extraction.live.storage.JSONCache
import org.dbpedia.extraction.live.queue.LiveQueueItem

import scala.xml._
import org.dbpedia.extraction.wikiparser.impl.json.JsonWikiParser
import org.dbpedia.extraction.live.extractor.LiveExtractor
import scala.collection.JavaConversions._

import scala.collection.mutable


/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: May 26, 2010
 * Time: 3:10:00 PM
 * This object loads the configuration information from the config file and also performs the actual step of extracting
 * the required triples from the wikipage.
 */

object LiveExtractionController
{
  private var multilanguageExtractors: mutable.HashMap[Language, List[Extractor[_]]] = new mutable.HashMap[Language, List[Extractor[_]]]()
  private var reloadOntologyAndMapping = true
  private var ontologyAndMappingsUpdateTime : Long = 0
  private var languages = LiveOptions.languages.map(Language.apply(_))

  //TODO Namespaces are buggy for commons, below is the previous code, but (language == Language.Commons) will always be false
  //private val namespaces = if (language == Language.Commons) ExtractorUtils.commonsNamespacesContainingMetadata
  //  else Set(Namespace.Main, Namespace.Template, Namespace.Category) //TODO implement multilanguage
  private val namespaces = Set(Namespace.Main, Namespace.Template, Namespace.Category)
  val logger: Logger = LoggerFactory.getLogger("LiveExtractionConfigLoader")

  /** Ontology source */
  val ontologySource: Source = WikiSource.fromNamespaces(
    namespaces = Set(Namespace.OntologyClass, Namespace.OntologyProperty),
    url = new URL(Language.Mappings.apiUri),
    language = Language.Mappings )

  /** Mappings source */
  val mappingsSource: Map[Language, Source] =  languages
    .map(language => language -> WikiSource.fromNamespaces(
    namespaces = Set(Namespace.mappings(language)),
    url = new URL(Language.Mappings.apiUri),
    language = Language.Mappings )).toMap

  println ("COMMONS SOURCE = " + LiveOptions.options.get("commonsDumpsPath"))

  val commonsSource = null

  val policies: Array[Policy] = {
    UriPolicy.parsePolicy(LiveOptions.options.get("uri-policy.main"))
  }

  def reload(t : Long): Unit =
  {
    if (t > ontologyAndMappingsUpdateTime)
      reloadOntologyAndMapping = true
  }

  /**
   * Retrieves the dump stream for a specific language edition.
   */
  private def getDumpFile(dumpDir : File, wikiPrefix : String) : File =    //wikiPrefix is language prefix (and can be 'commons')
  {
    val wikiDir = new File(dumpDir + "/" + wikiPrefix)
    if(!wikiDir.isDirectory) throw new Exception("Dump directory not found: " + wikiDir)

    //Find most recent dump date
    val date = wikiDir.list()
      .filter(_.matches("\\d{8}"))
      .sortWith(_.toInt > _.toInt)
      .headOption.getOrElse(throw new Exception("No dump found for Wiki: " + wikiPrefix))

    val articlesDump = new File(wikiDir + "/" + date + "/" + wikiPrefix.replace('-', '_') + "wiki-" + date + "-pages-articles.xml")
    if(!articlesDump.isFile) throw new Exception("Dump not found: " + articlesDump)

    articlesDump
  }

  def extractPage(item: LiveQueueItem, apiURL :String, landCode :String): Boolean =
  {
    val lang = Language.apply(landCode)
    val articlesSource : Source =
      if (item.getXML.isEmpty)
        WikiSource.fromPageIDs(List(item.getItemID), new URL(apiURL), lang)
      else {
        XMLSource.fromOAIXML(XML.loadString(item.getXML))
      }
    startExtraction(articlesSource,lang)
  }

  def extractPageFromTitle(item: LiveQueueItem, apiURL :String, landCode :String): Boolean =
  {
    val lang = Language.apply(landCode)
    val articlesSource : Source =
      if (item.getXML.isEmpty) {
        logger.info{"WikiTitle.parse(item.getItemName, lang)"+WikiTitle.parse(item.getItemName, lang)+""}
        WikiSource.fromTitles(List(WikiTitle.parse(item.getItemName, lang)), new URL(apiURL), lang)
      }
      else {
        XMLSource.fromOAIXML(XML.loadString(item.getXML))
      }
    startExtraction(articlesSource,lang)
  }


  /**
   * Loads the configuration and creates extraction jobs for all configured languages.
   *
   * @return Non-strict Traversable over all configured extraction jobs i.e. an extractions job will not be created until it is explicitly requested.
   */
  def startExtraction(articlesSource : Source, language : Language):Boolean =
  {
    // In case of single threading
    //Extractor

    if(! multilanguageExtractors.contains(language) || reloadOntologyAndMapping) {
      this.synchronized {
        if(! multilanguageExtractors.contains(language) || reloadOntologyAndMapping) {
          multilanguageExtractors  += (language -> LoadOntologyAndMappings(articlesSource, language))
          logger.info("Ontology and mappings reloaded")
          reloadOntologyAndMapping = false
        }
      }
    }

    //var liveDest : LiveUpdateDestination = null;
    var complete = false

    for (wikiPage <- articlesSource)
    {

      if(namespaces.contains(wikiPage.title.namespace))
      {


        val liveCache = new JSONCache(language.wikiCode, wikiPage.id, wikiPage.title.decoded) //
        //TODO option included here, but should be nicer
        liveCache.jsonCacheUpdateNthEdit =  Integer.parseInt(LiveOptions.options.get("cache.jsonCacheUpdateNthEdit"))

        var destList = new ArrayBuffer[LiveDestination]()  // List of all final destinations
        destList += new JSONCacheUpdateDestination(liveCache)
        destList += new PublisherDiffDestination(wikiPage.id, liveCache.performCleanUpdate(), if (liveCache.cacheObj != null) liveCache.cacheObj.subjects else new java.util.HashSet[String]())
        destList += new LoggerDestination(wikiPage.id, wikiPage.title.decoded,language.wikiCode) // Just to log extraction results

        val compositeDest: LiveDestination = new CompositeLiveDestination(destList: _*) // holds all main destinations

        val extractorDiffDest = new JSONCacheExtractorDestination(liveCache, compositeDest) // filters triples to add/remove/leave
        // TODO get liveconfigReader permanently
        val extractorRestrictDest = new ExtractorRestrictDestination ( LiveExtractorConfigReader.extractors.get(language), extractorDiffDest)

        // We don't know in advance what parsers we will need so we initialize them as lazy and will be computed onfirst run
        lazy val pageNode = {
          val pageNodeParser = WikiParser.getInstance()
          pageNodeParser(wikiPage)
        }
        lazy val jsonNode = {
          val jsonNodeParser = new JsonWikiParser()
          jsonNodeParser(wikiPage)
        }
        val uri = wikiPage.title.language.resourceUri.append(wikiPage.title.decodedWithNamespace)
        logger.info("uri: "+uri)
        logger.info("wikiPage.title.language.resourceUri.append(wikiPage.title.encodedWithNamespace)"+wikiPage.title.encodedWithNamespace)

        extractorRestrictDest.open

        //Get triples from each extractor separately
        multilanguageExtractors.get(language).foreach(extractors => {
          extractors.foreach(extractor => {
          //Try to get extractor contents, onError just return empty triples
          val RequiredGraph =
            try{
              // We try to distguish between different types of extractors
              // need to find a cleaner way of doing this
              extractor match {
                case pageNodeExtractor :PageNodeExtractor =>
                  pageNode match {
                    case Some(pageNodeValue) => pageNodeExtractor.extract(pageNodeValue, uri)
                    case _ => Seq.empty // in case the WikiParser returned None
                  }
                case jsonNodeExtractor :JsonNodeExtractor =>
                  jsonNode match {
                    case Some(jsonNodeValue) => jsonNodeExtractor.extract(jsonNodeValue, uri)
                    case _ => Seq.empty  // in case the jsonParser returned None
                  }
                case wikiPageExtractor :WikiPageExtractor =>  wikiPageExtractor.extract(wikiPage, uri)
                case _ => Seq.empty
              }

              }


            catch {
              case ex: Exception =>
                logger.error("Error in " + extractor.getClass.getName + "\nError Message: " + ex.getMessage, ex)
                Seq()
            }
          extractorRestrictDest.write(extractor.getClass.getName, "", RequiredGraph, Seq(), Seq())
        })})


        extractorRestrictDest.close
        complete = true

      }
      else
      {
        // If not in the allowed namespace, delete cache (if exists)
        JSONCache.deleteCacheOnlyItem(wikiPage.title.language.wikiCode, wikiPage.id) //TODO make sure the wikicode is whats needed here
      }


    }
    complete
  }

  /** This method loads the ontology and mappings
    * @param  articlesSource  The source of the wikipage article
    * @param  language  The required language
    */
  //    private def LoadOntologyAndMappings(articlesSource: Source, language: Language): Extractor = {
  private def LoadOntologyAndMappings(articlesSource: Source, language: Language): List[Extractor[_]] = {

    ontologyAndMappingsUpdateTime = System.currentTimeMillis
    val extractorClasses = convertExtractorListToScalaList(LiveExtractorConfigReader.getExtractors(language, ExtractorStatus.ACTIVE))
    LiveExtractor.load(ontologySource, mappingsSource.getOrElse(language, null), articlesSource, commonsSource,
            extractorClasses, language) //TODO make sure this doesn't have to be a class instance instead of an object
  }

  /**
   * Converts a givin java list to scala list
   * @ param  list  The required java list
   * @return  The newly generated scala list
   */
  private def convertExtractorListToScalaList(list : java.util.List[Class[_]]): List[Class[Extractor[_]]] =
  {
    var extractorList =  List[Class[Extractor[_]]]()

    val listiterator = list.iterator()
    while(listiterator.hasNext){
      try {
        extractorList = extractorList ::: List[Class[Extractor[_]]](listiterator.next().asInstanceOf[Class[Extractor[_]]])
      }
      catch {
        case e: Exception =>  logger.warn("Cannot instantiate Extractor List", e)
      }
    }
    extractorList
  }

}

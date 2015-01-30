package org.dbpedia.extraction.live.extraction

import java.net.URL
import collection.immutable.ListMap
import java.util.Properties
import java.io.File
import org.apache.log4j.Logger
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.sources.{WikiPage, WikiSource, Source, XMLSource}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.destinations.formatters.UriPolicy
import org.dbpedia.extraction.live.helper.{ExtractorStatus, LiveConfigReader}
import org.dbpedia.extraction.live.core.LiveOptions
import collection.mutable.ArrayBuffer
import org.dbpedia.extraction.live.storage.JSONCache
import org.dbpedia.extraction.live.queue.LiveQueueItem
import scala.xml._
import org.dbpedia.extraction.wikiparser.impl.WikiParserWrapper
import org.dbpedia.extraction.wikiparser.impl.json.JsonWikiParser
import org.dbpedia.extraction.live.extractor.LiveExtractor


/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: May 26, 2010
 * Time: 3:10:00 PM
 * This object loads the configuration information from the config file and also performs the actual step of extracting
 * the required triples from the wikipage.
 */

object LiveExtractionConfigLoader
{
  //    private var config : Config = null;
  private var extractors : List[Extractor[_]] = null;
  private var reloadOntologyAndMapping = true;
  private var ontologyAndMappingsUpdateTime : Long = 0;
  val logger = Logger.getLogger("LiveExtractionConfigLoader");

  /** Ontology source */
  val ontologySource = WikiSource.fromNamespaces(
    namespaces = Set(Namespace.OntologyClass, Namespace.OntologyProperty),
    url = new URL(Language.Mappings.apiUri),
    language = Language.Mappings );

  /** Mappings source */
  val mappingsSource =  WikiSource.fromNamespaces(
    namespaces = Set(Namespace.mappings(Language.apply(LiveOptions.options.get("language")))),
    url = new URL(Language.Mappings.apiUri),
    language = Language.Mappings );

  println ("COMMONS SOURCE = " + LiveOptions.options.get("commonsDumpsPath"));

  val commonsSource = null;

  val policies = {
    UriPolicy.parsePolicy(LiveOptions.options.get("uri-policy.main"))
  }

  def reload(t : Long) =
  {
    if (t > ontologyAndMappingsUpdateTime)
      reloadOntologyAndMapping = true;
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


  /**
   * Loads the configuration and creates extraction jobs for all configured languages.
   *
   * @param configFile The configuration file
   * @return Non-strict Traversable over all configured extraction jobs i.e. an extractions job will not be created until it is explicitly requested.
   */
  def startExtraction(articlesSource : Source, language : Language):Boolean =
  {
    // In case of single threading
    //Extractor
    if(extractors==null || reloadOntologyAndMapping) {
      this.synchronized {
        if(extractors==null || reloadOntologyAndMapping) {
          extractors = LoadOntologyAndMappings(articlesSource, language);
          logger.info("Ontology and mappings reloaded");
          reloadOntologyAndMapping = false;
        }
      }
    }

    //var liveDest : LiveUpdateDestination = null;
    var complete = false;

    for (wikiPage <- articlesSource)
    {

      if(wikiPage.title.namespace == Namespace.Main ||
        wikiPage.title.namespace == Namespace.Template ||
        wikiPage.title.namespace == Namespace.Category)
      {



        val liveCache = new JSONCache(wikiPage.id, wikiPage.title.decoded)

        var destList = new ArrayBuffer[LiveDestination]()  // List of all final destinations
        if (liveCache.performCleanUpdate) {
          destList += new SPARULDelAllDestination(liveCache.cacheObj.subjects, policies)
          destList += new SPARULAddAllDestination(policies)
        } else {
          // *Delete first* When a triple is deleted from one extractor and added from another extractor
          destList += new SPARULDestination(false, policies) // delete triples
          destList += new SPARULDestination(true, policies) // add triples
        }
        destList += new JSONCacheUpdateDestination(liveCache)
        destList += new PublisherDiffDestination(wikiPage.id, policies)
        destList += new LoggerDestination(wikiPage.id, wikiPage.title.decoded) // Just to log extraction results

        val compositeDest: LiveDestination = new CompositeLiveDestination(destList.toSeq: _*) // holds all main destinations

        val extractorDiffDest = new JSONCacheExtractorDestination(liveCache, compositeDest) // filters triples to add/remove/leave
        // TODO get liveconfigReader permanently
        val extractorRestrictDest = new ExtractorRestrictDestination ( LiveConfigReader.extractors.get(Language.apply(language.isoCode)), extractorDiffDest)

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
        val context = new PageContext()

        extractorRestrictDest.open

        //Get triples from each extractor separately
        extractors.foreach(extractor => {
          //Try to get extractor contents, onError just return empty triples
          val RequiredGraph =
            try{
              // We try to distguish between different types of extractors
              // need to find a cleaner way of doing this
              extractor match {
                case pageNodeExtractor :PageNodeExtractor =>  {
                  pageNode match {
                    case Some(pageNodeValue) => pageNodeExtractor.extract(pageNodeValue, uri, context)
                    case _ => Seq.empty // in case the WikiParser returned None
                  }
                }
                case jsonNodeExtractor :JsonNodeExtractor =>  {
                  jsonNode match {
                    case Some(jsonNodeValue) => jsonNodeExtractor.extract(jsonNodeValue, uri, context)
                    case _ => Seq.empty  // in case the jsonParser returned None
                  }
                }
                case wikiPageExtractor :WikiPageExtractor =>  wikiPageExtractor.extract(wikiPage, uri, context)
                case _ => Seq.empty
              }

            }
            catch {
              case ex: Exception => {
                logger.error("Error in " + extractor.getClass().getName() + "\nError Message: " + ex.getMessage, ex)
                Seq()
              }
            }
          extractorRestrictDest.write(extractor.getClass().getName(), "", RequiredGraph, Seq(), Seq())
        });

        extractorRestrictDest.close
        complete = true

      }
      else
      {
        // If not in the allowed namespace, delete cache (if exists)
        JSONCache.deleteCacheOnlyItem(wikiPage.id)
      }

    }
    complete
  }

  //This method loads the ontology and mappings
  //@param  articlesSource  The source of the wikipage article
  //@param  language  The required language
  //    private def LoadOntologyAndMappings(articlesSource: Source, language: Language): Extractor = {
  private def LoadOntologyAndMappings(articlesSource: Source, language: Language): List[Extractor[_]] = {

    ontologyAndMappingsUpdateTime = System.currentTimeMillis
    val extractorClasses = convertExtractorListToScalaList(LiveConfigReader.getExtractors(language, ExtractorStatus.ACTIVE))
    LiveExtractor.load(ontologySource, mappingsSource, articlesSource, commonsSource,
            extractorClasses, language)
  }

  /**
   * Converts a givin java list to scala list
   * @ param  list  The required java list
   * @return  The newly generated scala list
   */
  private def convertExtractorListToScalaList(list : java.util.List[Class[_]]): List[Class[Extractor[_]]] =
  {
    var extractorList =  List[Class[Extractor[_]]]();

    val listiterator = list.iterator();
    while(listiterator.hasNext){
      try {
        extractorList = extractorList ::: List[Class[Extractor[_]]](listiterator.next().asInstanceOf[Class[Extractor[_]]]);
      }
      catch {
        case e: Exception =>  logger.warn("Cannot instantiate Extractor List", e)
      }
    }
    extractorList;
  }

  /*
  private def convertExtractorMapToScalaMap(map: java.util.Map[Language, java.util.List[Class[Extractor[_]]]]):
  Map[Language, List[Class[Extractor[_]]]] =
  {
    var extractorMap =  Map[Language, List[Class[Extractor[_]]]]();

    val mapIterator = map.entrySet.iterator();
    while(mapIterator.hasNext){
      var pairs = mapIterator.next();
      extractorMap += pairs.getKey() -> convertExtractorListToScalaList(pairs.getValue());
    }
    println(extractorMap);
    extractorMap;

  } */

  private class Config(config : Properties)
  {

    /** Languages */
    if(config.getProperty("languages") == null)
      throw new IllegalArgumentException("Property 'languages' is not defined.")
    private val languages = config.getProperty("languages").split("\\s+").map(_.trim).toList
      .map(code => Language.getOrElse(code, throw new IllegalArgumentException("Invalid language: '" + code + "'")))

    /** The period of updating ontology and mappings**/
    if(config.getProperty("updateOntologyAndMappingsPeriod") == null)
      throw new IllegalArgumentException("Property 'updateOntologyAndMappingsPeriod' is not defined.");
    val updateOntologyAndMappingsPeriod = config.getProperty("updateOntologyAndMappingsPeriod").toInt;
    println("updateOntologyAndMappingsPeriod = " + updateOntologyAndMappingsPeriod);

    if(config.getProperty("multihreadingMode") == null) throw new IllegalArgumentException("Property 'multihreadingMode' is not defined.")
    val multihreadingMode = config.getProperty("multihreadingMode").toBoolean;

    /** Extractor classes */
    val extractors = loadExtractorClasses()

    /** Ontology source */
    val ontologySource = WikiSource.fromNamespaces(namespaces = Set(Namespace.OntologyClass, Namespace.OntologyProperty),
      url = new URL("http://mappings.dbpedia.org/api.php"),
      language = Language.apply(LiveOptions.options.get("language")) );

    /** Mappings source */
    val mappingsSource =  WikiSource.fromNamespaces(namespaces = Set(Namespace.mappings(Language.apply(LiveOptions.options.get("language")))),
      url = new URL("http://mappings.dbpedia.org/api.php"),
      language = Language.apply(LiveOptions.options.get("language")) );

    /**
     *  Loads the extractors classes from the configuration.
     *
     * @return A Map which contains the extractor classes for each language
     */
    private def loadExtractorClasses() : Map[Language, List[Class[PageNodeExtractor]]] =
    {
      //Load extractor classes
      if(config.getProperty("extractors") == null) throw new IllegalArgumentException("Property 'extractors' not defined.")
      val stdExtractors = loadExtractorConfig(config.getProperty("extractors"))

      //Create extractor map
      var extractors = ListMap[Language, List[Class[PageNodeExtractor]]]()
      for(language <- languages) extractors += ((language, stdExtractors))

      //Load language specific extractors
      val LanguageExtractor = "extractors\\.(.*)".r

      for(LanguageExtractor(code) <- config.stringPropertyNames.toArray;
          language = Language.getOrElse(code, throw new IllegalArgumentException("Invalid language: " + code));
          if extractors.contains(language))
      {
        extractors += ((language, stdExtractors ::: loadExtractorConfig(config.getProperty("extractors." + code))))
      }

      extractors
    }

    /**
     * Parses a enumeration of extractor classes.
     */
    private def loadExtractorConfig(configStr : String) : List[Class[PageNodeExtractor]] =
    {
      configStr.split("\\s+").map(_.trim).toList
        .map(className => ClassLoader.getSystemClassLoader().loadClass(className))
        .map(_.asInstanceOf[Class[PageNodeExtractor]])
    }
  }

  private class LiveConfig(){
    val xmlConfigTree = XML.loadFile("./live.config");
    var currentNode = xmlConfigTree \\ "extractor";
    println(currentNode.toString)
  }
}

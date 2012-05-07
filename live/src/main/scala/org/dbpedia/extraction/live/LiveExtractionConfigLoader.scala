package org.dbpedia.extraction.live.extraction

import org.dbpedia.extraction.destinations.formatters.TerseFormatter
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.mappings.CompositeExtractor
import java.net.URL
import collection.immutable.ListMap
import java.util.Properties
import javax.swing.Timer
import java.io.{FileReader, File}
import org.dbpedia.extraction.util.StringUtils._
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.sources.{MemorySource, WikiSource, XMLSource, Source}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.{StringDestination, FileDestination, CompositeDestination}
import java.util.logging.{Level, Logger}
import java.awt.event.{ActionListener, ActionEvent}
import org.dbpedia.extraction.live.destinations.LiveUpdateDestination
import org.dbpedia.extraction.live.job.LiveExtractionJob
import xml.XML
import org.dbpedia.extraction.live.helper.{ExtractorStatus, LiveConfigReader}
import org.dbpedia.extraction.live.statistics.RecentlyUpdatedInstance
import org.dbpedia.extraction.live.main.Main
import scala.collection.JavaConversions;


/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: May 26, 2010
 * Time: 3:10:00 PM
 * This object loads the configuration information from the config file and also performs the actual step of extracting
 * the required triples from the wikipage.
 */

object LiveExtractionConfigLoader extends ActionListener
{
    //This number is used for filling the recentlyUpdatedInstances array which is in Main class, this array is used
    //for statistics
    private var instanceNumber = 0;

//    private var config : Config = null;
    private var extractors : List[Extractor] = null;
    // private var CurrentJob : ExtractionJob = null;
    private var reloadOntologyAndMapping = true;
    val logger = Logger.getLogger("LiveExtractionConfigLoader");

   /** Ontology source */
    val ontologySource = WikiSource.fromNamespaces(namespaces = Set(Namespace.OntologyClass, Namespace.OntologyProperty),
                                                   url = new URL("http://mappings.dbpedia.org/api.php"),
                                                   language = Language.Default );

    /** Mappings source */
    val mappingsSource =  WikiSource.fromNamespaces(namespaces = Set(Namespace.mappings(Language.Default)),
                                                        url = new URL("http://mappings.dbpedia.org/api.php"),
                                                        language = Language.Default );
  

  def actionPerformed(e: ActionEvent) =
    {
       reloadOntologyAndMapping = true;
    }

//    //This function loads the configuration from the file passed
//    def loadConfig(configFile : File)
//      {
//        //Load properties from the passed config file
//        val properties = new Properties()
//        properties.load(new FileReader(configFile))
//
//        //Load configuration
//        config = new Config(properties)
//        //val MyTimer = new Timer(config.updateOntologyAndMappingsPeriod * 60 * 1000, this);
//        val MyTimer = new Timer(LiveConfigReader.updateOntologyAndMappingsPeriod * 60 * 1000, this);
//        MyTimer.start();
//
//      }


    /**
     * Loads the configuration and creates extraction jobs for all configured languages.
     *
     * @param configFile The configuration file
     * @return Non-strict Traversable over all configured extraction jobs i.e. an extractions job will not be created until it is explicitly requested.
     */
    def startExtraction(articlesSource : Source) : Unit =
    {
//        if(config.multihreadingMode)
      if(LiveConfigReader.multihreadingMode)
          {
            //Create a non-strict view of the extraction jobs

//            val extractionJobs = config.extractors.keySet.view.map(createExtractionJob(articlesSource))
            val extractionJobs = convertExtractorMapToScalaMap(LiveConfigReader.extractorClasses).keySet.view.map(createExtractionJob(articlesSource))
//            val extractionJobs = config.extractors.keySet.view.map(createExtractionJob(articlesSource))
            for(extractionJob <- extractionJobs)
            {
              extractionJob.start();
            }
          }
      else
          {
//            config.extractors.keySet.foreach(LangName => startSingleThreadExtraction(articlesSource)(LangName));
            //LiveConfigReader.extractors.keySet.foreach(LangName => startSingleThreadExtraction(articlesSource)(LangName));
            startSingleThreadExtraction(articlesSource)(Language.Default);
            //startSingleThreadExtraction(articlesSource)(Language.Default);
          }
    }

  def isMultithreading():Boolean =
  {
//    config.multihreadingMode;
    LiveConfigReader.multihreadingMode;
  }

    /**
     * Creates an extraction job for a specific language.
     */

  private def createExtractionJob(articlesSource : Source)(language : Language) : LiveExtractionJob =
    {
      /*// In case of single threading
        //Extractor
        if(extractor==null)
        {
          val emptySource = new MemorySource();

          LiveExtractor.loadRedirects(articlesSource);
          LiveExtractor.makeExtractionContext(config.mappingsSource, emptySource, articlesSource, language)
          extractor = LiveExtractor.startExtraction(config.ontologySource, config.mappingsSource, emptySource, articlesSource, config.extractors(language), language)

        }
        //Destination
        val tripleDestination = new FileDestination(new NTriplesFormatter(), config.outputDir, dataset => language.filePrefix + "/" + dataset.name + "_" + language.filePrefix + ".nt")

        val quadDestination = new FileDestination(new NQuadsFormatter(), config.outputDir, dataset => language.filePrefix + "/" + dataset.name + "_" + language.filePrefix + ".nq")
        val destination = new CompositeDestination(tripleDestination, quadDestination)
        //println("Article source = " + articlesSource.size);
        //if(CurrentJob == null)
          //CurrentJob = new ExtractionJob(extractor, articlesSource, tripleDestination, "Extraction Job for " + language.wikiCode + " Wikipedia")

      val parser = WikiParser()
        articlesSource.foreach(CurrentWikiPage =>
                {
                 if(CurrentWikiPage.title.namespace == Namespace.Main ||
                    CurrentWikiPage.title.namespace == Namespace.File ||
                    CurrentWikiPage.title.namespace == Namespace.Category)
                   {
                      val TempDest = new  StringDestination();
                      //println(CurrentWikiPage.title.namespace);
                      val CurrentPageNode = parser(CurrentWikiPage)
                      println("The Current page id = "+ CurrentPageNode.id)
                      //println(extractor);
                      val RequiredGraph = extractor(parser(CurrentWikiPage));

                      tripleDestination.write(RequiredGraph)
                      //println(TempDest);
                      //println("The end of the loop");
                   }

                })


      //else
          //{
            //CurrentJob.interrupt();
            //CurrentJob = new ExtractionJob(extractor, articlesSource, tripleDestination, "Extraction Job for " + language.wikiCode + " Wikipedia");
          //}

        //println("job ID = " + CurrentJob.getId());
        CurrentJob;

        //null;
      */
      // In case of multi-threading
      //Extractor
        if(extractors == null || reloadOntologyAndMapping)
        {
          extractors = LoadOntologyAndMappings(articlesSource, language);
          logger.log(Level.INFO, "Ontology and mappings reloaded");
          reloadOntologyAndMapping = false;
        }

        //Destination
//        val tripleDestination = new FileDestination(new NTriplesFormatter(), config.outputDir,
//          dataset => language.filePrefix + "/" + dataset.name + "_" + language.filePrefix + ".nt", true)
//
//        val quadDestination = new FileDestination(new NQuadsFormatter(), config.outputDir,
//          dataset => language.filePrefix + "/" + dataset.name + "_" + language.filePrefix + ".nq", true)
//        val destination = new CompositeDestination(tripleDestination, quadDestination)
        //println("Article source = " + articlesSource.size);
        val TempDest = new  StringDestination(TerseFormatter.NTriplesIris);
        //val liveDest = new LiveUpdateDestination();
      
        //new ExtractionJob(extractor, articlesSource, tripleDestination, "Extraction Job for " + language.wikiCode + " Wikipedia");
        new LiveExtractionJob(null, articlesSource, language, "Extraction Job for " + language.wikiCode + " Wikipedia");

    }


    private def startSingleThreadExtraction(articlesSource : Source)(language : Language):Unit =
    {
      // In case of single threading
      //Extractor
      if(extractors==null || reloadOntologyAndMapping)
      {
        /*//Load the ontology at the beginning because it is a very heavy step so it's better to perform it only once in the beginning
        LiveExtractor.loadOntology(config.ontologySource);
        
        val emptySource = new MemorySource();

        LiveExtractor.loadRedirects(articlesSource);
        LiveExtractor.makeExtractionContext(config.mappingsSource, emptySource, articlesSource, language)
        extractor = LiveExtractor.load(config.ontologySource, config.mappingsSource, emptySource, articlesSource, config.extractors(language), language)
        */
        extractors = LoadOntologyAndMappings(articlesSource, language);
        println(extractors.length);
        logger.log(Level.INFO, "Ontology and mappings reloaded");
        reloadOntologyAndMapping = false;
      }

      
      //Destination
//      val tripleDestination = new FileDestination(new NTriplesFormatter(), config.outputDir,
//        dataset => language.filePrefix + "/" + dataset.name + "_" + language.filePrefix + ".nt", true)
//
//
//      val quadDestination = new FileDestination(new NQuadsFormatter(), config.outputDir,
//        dataset => language.filePrefix + "/" + dataset.name + "_" + language.filePrefix + ".nq", true)
//      val destination = new CompositeDestination(tripleDestination, quadDestination)

      var liveDest : LiveUpdateDestination = null;
      val parser = WikiParser()
      def updateStatistics(wikipageTitle: String, wikipageURL: String): Unit = {
        //Increment the number of processed instances
        Main.instancesUpdatedInMinute = Main.instancesUpdatedInMinute + 1;
        Main.instancesUpdatedIn5Minutes = Main.instancesUpdatedIn5Minutes + 1;
        Main.instancesUpdatedInHour = Main.instancesUpdatedInHour + 1;
        Main.instancesUpdatedInDay = Main.instancesUpdatedInDay + 1;
        Main.totalNumberOfUpdatedInstances = Main.totalNumberOfUpdatedInstances + 1;

        val endingSlashPos = wikipageURL.lastIndexOf("/");
        //val dbpediaPageURL = "http://dbpedia.org/resource" + wikipageURL.substring(endingSlashPos);
        val dbpediaPageURL = "http://live.dbpedia.org/resource" + wikipageURL.substring(endingSlashPos);

        Main.recentlyUpdatedInstances(instanceNumber) = new RecentlyUpdatedInstance(wikipageTitle, dbpediaPageURL, wikipageURL);
        instanceNumber  = (instanceNumber + 1) % Main.recentlyUpdatedInstances.length;
      }
      articlesSource.foreach(CurrentWikiPage =>
            {
              /*println(CurrentWikiPage.title.namespace.toString)

              if(CurrentWikiPage.title.namespace == Namespace.UserTalk || CurrentWikiPage.title.namespace == Namespace.User){
                logger.info("User or user ctalk");
                return ;
              }*/

             if(CurrentWikiPage.title.namespace == Namespace.Main ||
                CurrentWikiPage.title.namespace == Namespace.File ||
                CurrentWikiPage.title.namespace == Namespace.Category)
               {
                 val TempDest = new StringDestination(TerseFormatter.NTriplesIris);
                 val CurrentPageNode = parser(CurrentWikiPage)



//                 extractor.foreach(ex => println(ex(parser(CurrentWikiPage))));

//                 println(extractor.map(ex => ex(parser(CurrentWikiPage)))
//                  .reduceLeft(_ merge _))

//                 val RequiredGraph = extractor(parser(CurrentWikiPage));
//
                 /*println(CurrentPageNode.title);
                 println(CurrentPageNode.title.encodedWithNamespace);*/


                 //As the page title always starts with "en:", as it is the language of the page, and we are working only on
                 // English language, then we should remove that part as it will repeated without any advantage.
                 val coloPosition = CurrentPageNode.title.toString.indexOf(":");
                 val pageNodeTitleWithoutLanguage = CurrentPageNode.title.toString.substring(coloPosition+1);
                 val strWikipage = "http://" + CurrentPageNode.title.language + ".wikipedia.org/wiki/" + CurrentPageNode.title.encodedWithNamespace ;
                 liveDest = new LiveUpdateDestination(pageNodeTitleWithoutLanguage, language.locale.getLanguage(),
                                      CurrentPageNode.id.toString)
//
//                 println(LiveConfigReader.extractors.get(language));
//
//
                 liveDest.setPageID(CurrentPageNode.id);
                 liveDest.setOAIID(LiveExtractionManager.oaiID);

                 //Add triples generated from active extractors
                 extractors.foreach(ext => {
                    var RequiredGraph = ext(parser(CurrentWikiPage));
                   liveDest.write(RequiredGraph, ext.getClass().getName());
                 });

                 //Remove triples generated from purge extractors
                 liveDest.removeTriplesForPurgeExtractors();

                 //Keep triples generated from keep extractors
                 liveDest.retainTriplesForKeepExtractors();

                 liveDest.close();

//                 tripleDestination.write(RequiredGraph);
//                 tripleDestination.close();
                 logger.log(Level.INFO, "page number " + CurrentPageNode.id + " has been processed");

                 //Updating information needed for statistics

                 updateStatistics(CurrentWikiPage.title.decoded, strWikipage)
               }
//              else{
//               val CurrentPageNode = parser(CurrentWikiPage)
//               logger.log(Level.INFO, "page number " + CurrentPageNode.id + " cannot be processed");
//             }


            });
    }

    //This method loads the ontology and mappings
    //@param  articlesSource  The source of the wikipage article
    //@param  language  The required language
//    private def LoadOntologyAndMappings(articlesSource: Source, language: Language): Extractor = {
  private def LoadOntologyAndMappings(articlesSource: Source, language: Language): List[Extractor] = {
      //Load the ontology at the beginning because it is a very heavy step so it's better to perform it only once in the beginning
//      LiveExtractor.loadOntology(config.ontologySource);
      //org.dbpedia.extraction.live.extractor.LiveExtractor.loadOntology(this.ontologySource)

      //val emptySource = new MemorySource();

      //org.dbpedia.extraction.live.extractor.LiveExtractor.loadRedirects(articlesSource);
//      LiveExtractor.makeExtractionContext(config.mappingsSource, emptySource, articlesSource, language)
      //org.dbpedia.extraction.live.extractor.LiveExtractor.makeExtractionContext(this.mappingsSource, emptySource, articlesSource, language)
      //println(config.extractors(language))
//      LiveExtractor.load(config.ontologySource, config.mappingsSource, emptySource, articlesSource, config.extractors(language), language)

//      LiveExtractor.load(ontologySource, mappingsSource, emptySource, articlesSource,
//        convertExtractorListToScalaList(LiveConfigReader.extractorClasses.get(language)), language)

//        org.dbpedia.extraction.live.extractor.LiveExtractor.load(ontologySource, mappingsSource, emptySource, articlesSource,
//          convertExtractorListToScalaList(LiveConfigReader.getExtractors(language,ExtractorStatus.ACTIVE)), language)

      val extractorClasses = convertExtractorListToScalaList(LiveConfigReader.getExtractors(language, ExtractorStatus.ACTIVE))
      org.dbpedia.extraction.live.extractor.LiveExtractor.load(ontologySource, mappingsSource, articlesSource, extractorClasses, language)
    }

  /**
   * Converts a givin java list to scala list
   * @ param  list  The required java list
   * @return  The newly generated scala list
   */
  private def convertExtractorListToScalaList(list : java.util.List[Class[_ <: Extractor]]): List[Class[_ <: Extractor]] =
    {
      var extractorList =  List[Class[_ <: Extractor]]();

      val listiterator = list.iterator();
      while(listiterator.hasNext){
        extractorList = extractorList ::: List[Class[_ <: Extractor]](listiterator.next());
      }
      println(extractorList);
      extractorList;
    }

  private def convertExtractorMapToScalaMap(map: java.util.Map[Language, java.util.List[Class[_ <: Extractor]]]):
                    Map[Language, List[Class[_ <: Extractor]]] =
    {
      var extractorMap =  Map[Language, List[Class[_ <: Extractor]]]();

      val mapIterator = map.entrySet.iterator();
      while(mapIterator.hasNext){
        var pairs = mapIterator.next();
        extractorMap += pairs.getKey() -> convertExtractorListToScalaList(pairs.getValue());
      }
      println(extractorMap);
      extractorMap;

    }

    private class Config(config : Properties)
    {
        /** Resources directory */
        //TODO remove?
//        private val resourcesDir = new File("./src/main/resources")
//        if(!resourcesDir.exists) throw new IllegalArgumentException("Resource directory not found in " + resourcesDir.getCanonicalPath)

        /** Dump directory */
//        if(config.getProperty("dumpDir") == null)
//          throw new IllegalArgumentException("Property 'dumpDir' is not defined.")
//        val dumpDir = new File(config.getProperty("dumpDir"))

        /** Output directory */
//        if(config.getProperty("outputDir") == null)
//          throw new IllegalArgumentException("Property 'outputDir' is not defined.")
//        val outputDir = new File(config.getProperty("outputDir"))

        /** Languages */
        if(config.getProperty("languages") == null)
          throw new IllegalArgumentException("Property 'languages' is not defined.")
        private val languages = config.getProperty("languages").split("\\s+").map(_.trim).toList.map(Language)

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
                                                       language = Language.Default );

        /** Mappings source */
        val mappingsSource =  WikiSource.fromNamespaces(namespaces = Set(Namespace.mappings(Language.Default)),
                                                        url = new URL("http://mappings.dbpedia.org/api.php"),
                                                        language = Language.Default );
//        var pagecount :Long = 0;
//        mappingsSource.foreach(page =>
//          {
//            println(page.toXML);
//            println(page.title.namespace);
//            pagecount = pagecount + 1;
//          });
//      println(pagecount);
//      pagecount = 1;
//
//      ontologySource.foreach(page =>
//          {
//            println(page.title);
//            pagecount = pagecount + 1;
//          });
//      println(pagecount);

        /** Commons source */
//        val commonsSource = XMLSource.fromFile(getDumpFile("commons"), _.namespace == Namespace.File)

        /**
         * Retrieves the dump stream for a specific language edition.
         */

        /**
         *  Loads the extractors classes from the configuration.
         *
         * @return A Map which contains the extractor classes for each language
         */
        private def loadExtractorClasses() : Map[Language, List[Class[_ <: Extractor]]] =
        {
            //Load extractor classes
            if(config.getProperty("extractors") == null) throw new IllegalArgumentException("Property 'extractors' not defined.")
            val stdExtractors = loadExtractorConfig(config.getProperty("extractors"))

            //Create extractor map
            var extractors = ListMap[Language, List[Class[_ <: Extractor]]]()
            for(language <- languages) extractors += ((language, stdExtractors))

            //Load language specific extractors
            val LanguageExtractor = "extractors\\.(.*)".r

            for(LanguageExtractor(code) <- config.stringPropertyNames.toArray;
                language = Language(code);
                if extractors.contains(language))
            {
                extractors += ((language, stdExtractors ::: loadExtractorConfig(config.getProperty("extractors." + code))))
            }
             
            extractors
        }

        /**
         * Parses a enumeration of extractor classes.
         */
        private def loadExtractorConfig(configStr : String) : List[Class[_ <: Extractor]] =
        {
            configStr.split("\\s+").map(_.trim).toList
            .map(className => ClassLoader.getSystemClassLoader().loadClass(className).asSubclass(classOf[Extractor]))
        }
    }

    private class LiveConfig(){
      val xmlConfigTree = XML.loadFile("D:/Leipzig University/DBpediaExtraction/live/live.config");
      var currentNode = xmlConfigTree \\ "extractor";
      println(currentNode.toString)
    }
}

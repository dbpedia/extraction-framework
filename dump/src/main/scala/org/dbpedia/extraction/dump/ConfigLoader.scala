package org.dbpedia.extraction.dump

import _root_.org.dbpedia.extraction.destinations.formatters.{NTriplesFormatter, NQuadsFormatter}
import _root_.org.dbpedia.extraction.destinations.{FileDestination, CompositeDestination}
import _root_.org.dbpedia.extraction.mappings._
import collection.immutable.ListMap
import java.util.Properties
import java.io.{FileInputStream, InputStreamReader, File}
import _root_.org.dbpedia.extraction.util.StringUtils._
import _root_.org.dbpedia.extraction.util.Language
import java.net.URL
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.sources.{MemorySource, Source, XMLSource, WikiSource}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.Dataset

/**
 * Loads the dump extraction configuration.
 */
object ConfigLoader
{
    private var config : Config = null

    /**
     * Loads the configuration and creates extraction jobs for all configured languages.
     *
     * @param configFile The configuration file
     * @return Non-strict Traversable over all configured extraction jobs i.e. an extractions job will not be created until it is explicitly requested.
     */
    def load(configFile : File) : Traversable[ExtractionJob] =
    {
        //Load properties
        val properties = new Properties()
        properties.load(new InputStreamReader(new FileInputStream(configFile), "UTF-8"))

        //Load configuration
        config = new Config(properties)

        //Create a non-strict view of the extraction jobs
        // TODO: why non-strict?
        config.extractors.keySet.view.map(createExtractionJob)
    }
    
    private var ontologyFile : File = null

    private var mappingsDir : File = null

    private class Config(config : Properties)
    {
        /** Dump directory */
        if(config.getProperty("dumpDir") == null) throw new IllegalArgumentException("Property 'dumpDir' not defined.")
        val dumpDir = new File(config.getProperty("dumpDir"))
        if (! dumpDir.exists) throw new IllegalArgumentException("dump dir "+dumpDir+" does not exist")

        /** Output directory */
        if(config.getProperty("outputDir") == null) throw new IllegalArgumentException("Property 'outputDir' not defined.")
        val outputDir = new File(config.getProperty("outputDir"))
        if (! outputDir.exists) throw new IllegalArgumentException("output dir "+outputDir+" does not exist")

        /** Local ontology file, downloaded for speed and reproducibility */
        if(config.getProperty("ontologyFile") != null)
          ontologyFile = new File(config.getProperty("ontologyFile"))

        /** Local mappings files, downloaded for speed and reproducibility */
        if(config.getProperty("mappingsDir") != null)
          mappingsDir = new File(config.getProperty("mappingsDir"))

        /** Languages */
        if(config.getProperty("languages") == null) throw new IllegalArgumentException("Property 'languages' not defined.")
        val languages = config.getProperty("languages").split("[,\\s]+").map(_.trim).toList.map(Language)

        /** Extractor classes */
        val extractors = loadExtractorClasses()

        /**
         * Loads the extractors classes from the configuration.
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
            val LanguageExtractor = """extractors\.(.*)""".r

            for(LanguageExtractor(code) <- config.stringPropertyNames.toArray)
            {
                val language = Language(code)
                if (extractors.contains(language))
                {
                    extractors += ((language, stdExtractors ::: loadExtractorConfig(config.getProperty("extractors." + code))))
                }
            }

            extractors
        }

        /**
         * Parses a enumeration of extractor classes.
         */
        private def loadExtractorConfig(configStr : String) : List[Class[_ <: Extractor]] =
        {
            configStr.split("[,\\s]+").map(_.trim).toList
            .map(className => ClassLoader.getSystemClassLoader.loadClass(className).asSubclass(classOf[Extractor]))
        }
    }


    /**
     * Creates ab extraction job for a specific language.
     */
    private def createExtractionJob(lang : Language) : ExtractionJob =
    {
        //Extraction Context
        val context : DumpExtractionContext = extractionContext(lang)

        //Extractors
        val extractors = config.extractors(lang)
        val compositeExtractor = Extractor.load(extractors, context)

        //Destination
        val tripleDestination = new FileDestination(new NTriplesFormatter(), targetFile(lang, dumpDate(lang), "nt"))
        val quadDestination = new FileDestination(new NQuadsFormatter(), targetFile(lang, dumpDate(lang), "nq"))
        val destination = new CompositeDestination(tripleDestination, quadDestination)

        // Note: label is also used as file name, but space is replaced by underscores
        val jobLabel = "extraction job "+lang.wikiCode+" with "+extractors.size+" extractors"
        new ExtractionJob(compositeExtractor, context.articlesSource, destination, jobLabel)
    }

    private val parser = WikiParser()
    /**
     * Make an object that will be injected into the extractors.
     */
    private def extractionContext(lang : Language) : DumpExtractionContext = new DumpExtractionContext
    {
        def ontology : Ontology = _ontology

        def commonsSource : Source = _commonsSource

        def language : Language = lang

        private lazy val _mappingPageSource =
        {
            Namespace.mappingNamespace(language) match
            {
                case Some(namespace) =>
                  if (mappingsDir != null && mappingsDir.isDirectory)
                  {
                      val file = new File(mappingsDir, namespace.toString+".xml")
                      XMLSource.fromFile(file, language = language).map(parser)
                  }
                  else
                  {
                      val namespaces = Set(namespace)
                      val url = new URL("http://mappings.dbpedia.org/api.php")
                      val language = Language.Default
                      WikiSource.fromNamespaces(namespaces,url,language).map(parser)
                  }
                case None => new MemorySource().map(parser)
            }
        }
        
        def mappingPageSource : Traversable[PageNode] = _mappingPageSource

        private lazy val _mappings =
        {
            MappingsLoader.load(this)
        }
        def mappings : Mappings = _mappings

        private val _articlesSource =
        {
            XMLSource.fromFile(dumpFile(language),
                title => title.namespace == Namespace.Main || title.namespace == Namespace.File ||
                         title.namespace == Namespace.Category || title.namespace == Namespace.Template)
        }
        def articlesSource : Source = _articlesSource

        private val _redirects =
        {
            val cache = targetFile(language, dumpDate(language), "obj")(new Dataset("template-redirects"))
            Redirects.load(articlesSource, cache, language)
        }
        def redirects : Redirects = _redirects
    }

    //language-independent val
    private lazy val _ontology =
    {
        val ontologySource = if (ontologyFile != null && ontologyFile.isFile) 
        {
          XMLSource.fromFile(ontologyFile, language = Language.Default)
        } 
        else 
        {
          val namespaces = Set(Namespace.OntologyClass, Namespace.OntologyProperty)
          val url = new URL("http://mappings.dbpedia.org/api.php")
          val language = Language.Default
          WikiSource.fromNamespaces(namespaces, url, language)
        }
      
        new OntologyReader().read(ontologySource)
    }

    //language-independent val
    private lazy val _commonsSource =
    {
        XMLSource.fromFile(dumpFile(Language("commons")), _.namespace == Namespace.File)
    }

    /**
     * Retrieves the dump stream for a specific language edition.
     * FIXME: the same algorithm is used by the download code, mapping stats creator and in other 
     * places. We should share the code. Use a configurable strategy object that returns the paths etc.
     */
    private def dumpFile(language : Language) : File =
    {
        //Find most recent dump date
        val date = dumpDate(language)
        val dateDir = new File(wikiDir(language), date)
        val articlesDump = new File(dateDir, language.filePrefix + "wiki-" + date + "-pages-articles.xml")
        if(!articlesDump.isFile) throw new Exception("Dump not found: " + articlesDump)

        articlesDump
    }
    
    /**
     * Download directory for a language, e.g. "download/enwiki"
     * FIXME: the same algorithm is used by the download code, mapping stats creator and in other 
     * places. We should share the code. Use a configurable strategy object that returns the paths etc.
     */
    private def wikiDir(language : Language) : File =
    {
        val wiki = language.filePrefix+"wiki"
        val wikiDir = new File(config.dumpDir, wiki)
        if(! wikiDir.isDirectory) throw new Exception("Dump directory not found: " + wikiDir)
        wikiDir
    }
    
    /**
     * Finds the name (which is a date in format YYYYMMDD) of the latest wikipedia dump 
     * directory for the given language.
     * FIXME: the same algorithm is used by the download code, mapping stats creator and in other 
     * places. We should share the code. Use a configurable strategy object that returns the paths etc.
     */
    private def dumpDate(language : Language) : String = 
    {
        val dir = wikiDir(language)
        //Find most recent dump date
        dir.list().filter(_.matches("\\d{8}")).sortBy(_.toInt).lastOption.getOrElse(throw new Exception("No dump found in " +dir))
    }
    
    /**
     * Get target file path in config.outputDir. Note that this function should be fast and not 
     * access the file system - it is called not only in this class, but later during the 
     * extraction process for each dataset.
     * FIXME: the same algorithm is used by the download code, mapping stats creator and in other 
     * places. We should share the code. Use a configurable strategy object that returns the paths etc.
     */
    private def targetFile(language : Language, date : String, suffix : String)(dataset : Dataset) : File = {
        val wiki = language.filePrefix+"wiki"
        new File(config.outputDir, wiki+"/"+date+"/"+wiki+"-"+date+"-"+dataset.name.replace('_','-')+"."+suffix)
    }

}
package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.destinations.formatters._
import org.dbpedia.extraction.destinations.{FileDestination, CompositeDestination}
import org.dbpedia.extraction.mappings._
import collection.immutable.ListMap
import java.util.Properties
import java.io.{FileInputStream, InputStreamReader, File}
import org.dbpedia.extraction.util.StringUtils._
import org.dbpedia.extraction.util.{Language,Finder}
import org.dbpedia.extraction.util.RichFile.toRichFile
import java.net.URL
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.sources.{MemorySource, Source, XMLSource, WikiSource}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.destinations.Dataset
import org.dbpedia.extraction.dump.download.Download

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
    
    private var requireComplete = true

    private class Config(config : Properties)
    {
        /** Dump directory */
        if(config.getProperty("dumpDir") == null) throw new IllegalArgumentException("Property 'dumpDir' not defined.")
        val dumpDir = new File(config.getProperty("dumpDir"))
        if (! dumpDir.exists) throw new IllegalArgumentException("dump dir "+dumpDir+" does not exist")
        
        if(config.getProperty("require-download-complete") != null)
          requireComplete = config.getProperty("require-download-complete").toBoolean

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


    private val parser = WikiParser()
    
    /**
     * Creates ab extraction job for a specific language.
     */
    private def createExtractionJob(lang : Language) : ExtractionJob =
    {
        val finder = new Finder[File](config.dumpDir, lang)

        val date = latestDate(finder)
        
        //Extraction Context
        val context = new DumpExtractionContext
        {
            def ontology : Ontology = _ontology
    
            def commonsSource : Source = _commonsSource
    
            def language : Language = lang
    
            private lazy val _mappingPageSource =
            {
                val namespace = Namespace.mappings.getOrElse(language, throw new NoSuchElementException("no mapping namespace for language "+language.wikiCode))
                
                if (mappingsDir != null && mappingsDir.isDirectory)
                {
                    val file = new File(mappingsDir, namespace.getName(Language.Default).replace(' ','_')+".xml")
                    XMLSource.fromFile(file, language = language).map(parser)
                }
                else
                {
                    val namespaces = Set(namespace)
                    val url = new URL("http://mappings.dbpedia.org/api.php")
                    val language = Language.Default
                    WikiSource.fromNamespaces(namespaces,url,language).map(parser)
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
                XMLSource.fromFile(finder.file(date, "pages-articles.xml"),
                    title => title.namespace == Namespace.Main || title.namespace == Namespace.File ||
                             title.namespace == Namespace.Category || title.namespace == Namespace.Template)
            }
            
            def articlesSource : Source = _articlesSource
    
            private val _redirects =
            {
              val cache = finder.file(date, "template-redirects.obj")
              Redirects.load(articlesSource, cache, language)
            }
            
            def redirects : Redirects = _redirects
        }

        //Extractors
        val extractors = config.extractors(lang)
        val compositeExtractor = Extractor.load(extractors, context)
        
        /**
         * Get target file path in config.dumpDir. Note that this function should be fast and not 
         * access the file system - it is called not only in this class, but later during the 
         * extraction process for each dataset.
         */
        def targetFile(suffix : String)(dataset : Dataset) : File = {
          finder.file(date, dataset.name.replace('_','-')+"."+suffix)
        }

        //Destination
        val ntDest = new FileDestination(new NTriplesFormatter(), targetFile("nt"))
        val nqDest = new FileDestination(new NQuadsFormatter(), targetFile("nq"))
        val ttlDest = new FileDestination(new TurtleTriplesFormatter(), targetFile("ttl"))
        val tqlDest = new FileDestination(new TurtleQuadsFormatter(), targetFile("tql"))
        val destination = new CompositeDestination(ntDest, nqDest, ttlDest, tqlDest)

        // Note: label is also used as file name, but space is replaced by underscores
        val jobLabel = "extraction job "+lang.wikiCode+" with "+extractors.size+" extractors"
        new ExtractionJob(compositeExtractor, context.articlesSource, destination, jobLabel)
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
      val finder = new Finder[File](config.dumpDir, Language("commons"))
      val date = latestDate(finder)
      val file = finder.file(date, "pages-articles.xml")
      XMLSource.fromFile(file, _.namespace == Namespace.File)
    }
    
    private def latestDate(finder: Finder[_]): String = {
      val fileName = if (requireComplete) Download.Complete else "pages-articles.xml"
      val dates = finder.dates(fileName)
      if (dates.isEmpty) throw new IllegalArgumentException("found no directory with file '"+finder.wikiName+"-[YYYYMMDD]-"+fileName+"'")
      dates.last
    }
    
}
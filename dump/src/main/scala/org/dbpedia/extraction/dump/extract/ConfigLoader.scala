package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.destinations.formatters.{TerseFormatter,TriXFormatter}
import org.dbpedia.extraction.destinations.{Formatter,FileDestination, CompositeDestination}
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
 * 
 * TODO: clean up. The relations between the objects, classes and methods have become a bit chaotic.
 * There is no clean separation of concerns.
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
    
    private var formats: List[String] = null
    
    private var requireComplete = false

    private class Config(config : Properties)
    {
        // TODO: rewrite this, similar to download stuff:
        // - Don't use java.util.Properties, allow multiple values for one key
        // - Resolve config file names and load them as well
        // - Use pattern matching to parse arguments
        // - allow multiple config files, given on command line
      
        /** Dump directory */
        val dumpDir = getFile("dir")
        if (dumpDir == null) throw new IllegalArgumentException("property 'dir' not defined.")
        if (! dumpDir.exists) throw new IllegalArgumentException("dir "+dumpDir+" does not exist")
        
        if(config.getProperty("require-download-complete") != null)
          requireComplete = config.getProperty("require-download-complete").toBoolean

        /** Local ontology file, downloaded for speed and reproducibility */
        ontologyFile = getFile("ontology")

        /** Local mappings files, downloaded for speed and reproducibility */
        mappingsDir = getFile("mappings")

        /** Result formats */
        formats = getValues("formats")

        /** Languages */
        // TODO: add special parameters, similar to download: 
        // extract=10000-:InfoboxExtractor,PageIdExtractor means all languages with at least 10000 articles
        // extract=mapped:MappingExtractor means all languages with a mapping namespace
        var languages = getValues("languages").map(Language)
        if (languages.isEmpty) languages = Namespace.mappings.keySet.toList
        languages = languages.sorted(Language.wikiCodeOrdering)

        /** Extractor classes */
        val extractors = loadExtractorClasses()
        
        private def getFile(key: String): File = {
          val value = config.getProperty(key)
          if (value == null) null else new File(value)
        }
        
        private def getValues(key: String): List[String] = {
          val values = config.getProperty(key)
          if (values == null) List.empty
          else values.split("[,\\s]+", -1).map(_.trim).filter(_.nonEmpty).toList
        }

        /**
         * Loads the extractors classes from the configuration.
         *
         * @return A Map which contains the extractor classes for each language
         */
        private def loadExtractorClasses() : Map[Language, List[Class[_ <: Extractor]]] =
        {
            //Load extractor classes
            if(config.getProperty("extractors") == null) throw new IllegalArgumentException("Property 'extractors' not defined.")
            val stdExtractors = getValues("extractors").map(loadExtractorClass)

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
                    extractors += language -> (stdExtractors ::: getValues("extractors."+code).map(loadExtractorClass))
                }
            }

            extractors
        }

        private def loadExtractorClass(name: String): Class[_ <: Extractor] = {
          val className = if (! name.contains(".")) classOf[Extractor].getPackage.getName+'.'+name else name
          // TODO: class loader of Extractor.class is probably wrong for some users.
          classOf[Extractor].getClassLoader.loadClass(className).asSubclass(classOf[Extractor])
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
                val namespace = Namespace.mappings(language)
                
                if (mappingsDir != null && mappingsDir.isDirectory)
                {
                    val file = new File(mappingsDir, namespace.getName(Language.Mappings).replace(' ','_')+".xml")
                    XMLSource.fromFile(file, Language.Mappings).map(parser)
                }
                else
                {
                    val namespaces = Set(namespace)
                    val url = new URL(Language.Mappings.apiUri)
                    WikiSource.fromNamespaces(namespaces,url,Language.Mappings).map(parser)
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
                XMLSource.fromFile(finder.file(date, "pages-articles.xml"), language,                    
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
        val extractorClasses = config.extractors(lang)
        val extractor = new RootExtractor(CompositeExtractor.load(extractorClasses, context))
        
        def getFormatter(suffix: String): Formatter = {
          TerseFormatter.forSuffix(suffix).getOrElse( 
            TriXFormatter.forSuffix(suffix).getOrElse(
              throw new IllegalArgumentException("unknown file format suffix '"+suffix+"'."+
                " Known formats: "+(TerseFormatter.suffixes++TriXFormatter.suffixes).mkString(","))
            )
          )
        }
        
        /**
         * Get target file path in config.dumpDir. Note that this function should be fast and not 
         * access the file system - it is called not only in this class, but later during the 
         * extraction process for each dataset.
         */
        def targetFile(suffix : String)(dataset: Dataset): File = {
          finder.file(date, dataset.name.replace('_','-')+'.'+suffix)
        }

        val formatters =
          if (formats.nonEmpty) formats.map(getFormatter)
          // TODO: use two to four default formats instead of all twelve?
          else TerseFormatter.all ++ TriXFormatter.all
        val destinations = for (formatter <- formatters) yield new FileDestination(formatter, targetFile(formatter.fileSuffix))
        val destination = new CompositeDestination(destinations: _*)

        // Note: label is also used as file name, but space is replaced by underscores
        val jobLabel = "extraction job "+lang.wikiCode+" with "+extractorClasses.size+" extractors"
        new ExtractionJob(extractor, context.articlesSource, destination, jobLabel)
    }

    //language-independent val
    private lazy val _ontology =
    {
        val ontologySource = if (ontologyFile != null && ontologyFile.isFile) 
        {
          XMLSource.fromFile(ontologyFile, Language.Mappings)
        } 
        else 
        {
          val namespaces = Set(Namespace.OntologyClass, Namespace.OntologyProperty)
          val url = new URL(Language.Mappings.apiUri)
          val language = Language.Mappings
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
      XMLSource.fromFile(file, Language.Commons, _.namespace == Namespace.File)
    }
    
    private def latestDate(finder: Finder[_]): String = {
      val fileName = if (requireComplete) Download.Complete else "pages-articles.xml"
      val dates = finder.dates(fileName)
      if (dates.isEmpty) throw new IllegalArgumentException("found no directory with file '"+finder.wikiName+"-[YYYYMMDD]-"+fileName+"'")
      dates.last
    }
    
}
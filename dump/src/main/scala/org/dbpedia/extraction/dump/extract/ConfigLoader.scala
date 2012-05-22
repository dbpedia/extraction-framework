package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.destinations.{Dataset,Destination,FileDestination,CompositeDestination,MarkerDestination}
import org.dbpedia.extraction.mappings.{Mappings,MappingsLoader,Redirects,RootExtractor,CompositeExtractor}
import scala.collection.mutable.ArrayBuffer
import java.util.Properties
import java.io.{FileInputStream, InputStreamReader, File}
import org.dbpedia.extraction.util.{Language,Finder}
import org.dbpedia.extraction.util.RichFile.toRichFile
import java.net.URL
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.{XMLSource,WikiSource}
import org.dbpedia.extraction.wikiparser.{Namespace,PageNode,WikiParser}
import org.dbpedia.extraction.dump.download.Download

/**
 * Loads the dump extraction configuration.
 * 
 * TODO: clean up. The relations between the objects, classes and methods have become a bit chaotic.
 * There is no clean separation of concerns.
 */
object ConfigLoader
{
    private var config: Config = null

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
        config.extractorClasses.keySet.view.map(createExtractionJob)
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
            def ontology = _ontology
    
            def commonsSource = _commonsSource
    
            def language = lang
    
            private lazy val _mappingPageSource =
            {
                val namespace = Namespace.mappings(language)
                
                if (config.mappingsDir != null && config.mappingsDir.isDirectory)
                {
                    val file = new File(config.mappingsDir, namespace.getName(Language.Mappings).replace(' ','_')+".xml")
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
            
            def articlesSource = _articlesSource
    
            private val _redirects =
            {
              val cache = finder.file(date, "template-redirects.obj")
              Redirects.load(articlesSource, cache, language)
            }
            
            def redirects : Redirects = _redirects
        }

        //Extractors
        val extractorClasses = config.extractorClasses(lang)
        val extractor = new RootExtractor(CompositeExtractor.load(extractorClasses, context))
        
        /**
         * Get target file path in config.dumpDir. Note that this function should be fast and not 
         * access the file system - it is called not only in this class, but later during the 
         * extraction process for each dataset.
         */
        def targetFile(suffix : String)(dataset: Dataset) =
          finder.file(date, dataset.fileName+'.'+suffix)

        var destinations = new ArrayBuffer[Destination]()
        for ((suffix, format) <- config.formats) { 
          destinations += new FileDestination(format, targetFile(suffix)) 
        }
        
        destinations += new MarkerDestination(finder.file(date, Extraction.Complete), false, true)
        
        val jobLabel = "extraction job "+lang.wikiCode+" with "+extractorClasses.size+" extractors"
        new ExtractionJob(extractor, context.articlesSource, new CompositeDestination(destinations.toSeq: _*), jobLabel)
    }

    //language-independent val
    private lazy val _ontology =
    {
        val ontologySource = if (config.ontologyFile != null && config.ontologyFile.isFile)
        {
          XMLSource.fromFile(config.ontologyFile, Language.Mappings)
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
      val fileName = if (config.requireComplete) Download.Complete else "pages-articles.xml"
      val dates = finder.dates(fileName)
      if (dates.isEmpty) throw new IllegalArgumentException("found no directory with file '"+finder.wikiName+"-[YYYYMMDD]-"+fileName+"'")
      dates.last
    }
    
}
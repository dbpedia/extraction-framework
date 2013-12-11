package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.{XMLSource,WikiSource,Source}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.dump.download.Download
import org.dbpedia.extraction.util.{Language,Finder}
import org.dbpedia.extraction.util.RichFile.wrapFile
import scala.collection.mutable.{ArrayBuffer,HashMap}
import java.io._
import java.net.URL
import scala.io.Codec.UTF8
import java.util.logging.Logger
import org.dbpedia.extraction.util.IOUtils

/**
 * Loads the dump extraction configuration.
 * 
 * TODO: clean up. The relations between the objects, classes and methods have become a bit chaotic.
 * There is no clean separation of concerns.
 * 
 * TODO: get rid of all config file parsers, use Spring
 */
class ConfigLoader(config: Config)
{
    private val logger = Logger.getLogger(classOf[ConfigLoader].getName)

    /**
     * Loads the configuration and creates extraction jobs for all configured languages.
     *
     * @param configFile The configuration file
     * @return Non-strict Traversable over all configured extraction jobs i.e. an extractions job will not be created until it is explicitly requested.
     */
    def getExtractionJobs(): Traversable[ExtractionJob] =
    {
      // Create a non-strict view of the extraction jobs
      // non-strict because we want to create the extraction job when it is needed, not earlier
      config.extractorClasses.view.map(e => createExtractionJob(e._1, e._2, parser))
    }
    
    private val parser = WikiParser.getInstance(config.parser)
    
    /**
     * Creates ab extraction job for a specific language.
     */
    private def createExtractionJob(lang : Language, extractorClasses: List[Class[_ <: Extractor[_]]], parser : WikiParser) : ExtractionJob =
    {
        val finder = new Finder[File](config.dumpDir, lang, config.wikiName)

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
                    val file = new File(config.mappingsDir, namespace.name(Language.Mappings).replace(' ','_')+".xml")
                    XMLSource.fromFile(file, Language.Mappings).map(parser).flatten
                }
                else
                {
                    val namespaces = Set(namespace)
                    val url = new URL(Language.Mappings.apiUri)
                    WikiSource.fromNamespaces(namespaces,url,Language.Mappings).map(parser).flatten
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
              val articlesReaders = readers(config.source, finder, date)

              XMLSource.fromReaders(articlesReaders, language,
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

            private val _disambiguations =
            {
              val cache = finder.file(date, "disambiguations-ids.obj")
              try {
                Disambiguations.load(reader(finder.file(date, config.disambiguations)), cache, language)
              } catch {
                case ex: Exception =>
                  logger.info("Could not load disambiguations - error: " + ex.getMessage)
                  null
              }
            }

            def disambiguations : Disambiguations = if (_disambiguations != null) _disambiguations else new Disambiguations(Set[Long]())
        }

        //Extractors
        //val extractor = CompositeExtractor.load[PageNode](extractorClasses, context)
        val extractor = CompositeExtractor.loadToParsers(extractorClasses, context)
        val datasets = extractor.datasets
        
        val formatDestinations = new ArrayBuffer[Destination]()
        for ((suffix, format) <- config.formats) {
          
          val datasetDestinations = new HashMap[String, Destination]()
          for (dataset <- datasets) {
            val file = finder.file(date, dataset.name.replace('_', '-')+'.'+suffix)
            datasetDestinations(dataset.name) = new DeduplicatingDestination(new WriterDestination(writer(file), format))
          }
          
          formatDestinations += new DatasetDestination(datasetDestinations)
        }
        
        val destination = new MarkerDestination(new CompositeDestination(formatDestinations.toSeq: _*), finder.file(date, Extraction.Complete), false)
        
        val description = lang.wikiCode+": "+extractorClasses.size+" extractors ("+extractorClasses.map(_.getSimpleName).mkString(",")+"), "+datasets.size+" datasets ("+datasets.mkString(",")+")"
        new ExtractionJob(new RootExtractor(extractor), context.articlesSource, config.namespaces, destination, lang.wikiCode, description, parser)
    }
    
    private def writer(file: File): () => Writer = {
      () => IOUtils.writer(file)
    }

    private def reader(file: File): () => Reader = {
      () => IOUtils.reader(file)
    }

    private def readers(source: String, finder: Finder[File], date: String): List[() => Reader] = {

      files(source, finder, date).map(reader(_))
    }

    private def files(source: String, finder: Finder[File], date: String): List[File] = {

      val files = if (source.startsWith("@")) { // the articles source is a regex - we want to match multiple files
        finder.matchFiles(date, source.substring(1))
      } else List(finder.file(date, source))

      logger.info(s"Source is ${source} - ${files.size} file(s) matched")

      files
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
      val finder = new Finder[File](config.dumpDir, Language("commons"), config.wikiName)
      val date = latestDate(finder)
      XMLSource.fromReaders(readers(config.source, finder, date), Language.Commons, _.namespace == Namespace.File)
    }

    private def latestDate(finder: Finder[_]): String = {
      val isSourceRegex = config.source.startsWith("@")
      val source = if (isSourceRegex) config.source.substring(1) else config.source
      val fileName = if (config.requireComplete) Download.Complete else source
      finder.dates(fileName, isSuffixRegex = isSourceRegex).last
    }
}


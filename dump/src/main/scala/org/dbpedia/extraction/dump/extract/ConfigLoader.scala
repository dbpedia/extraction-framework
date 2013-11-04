package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.{XMLSource,WikiSource,Source}
import org.dbpedia.extraction.wikiparser.{Namespace,PageNode,WikiParser}
import org.dbpedia.extraction.dump.download.Download
import org.dbpedia.extraction.util.{Language,Finder}
import org.dbpedia.extraction.util.RichFile.wrapFile
import scala.collection.mutable.{ArrayBuffer,HashMap}
import java.io._
import java.net.URL
import org.apache.commons.compress.compressors.bzip2._
import java.util.zip._
import scala.io.Codec.UTF8
import java.util.logging.Logger

/**
 * Loads the dump extraction configuration.
 * 
 * TODO: clean up. The relations between the objects, classes and methods have become a bit chaotic.
 * There is no clean separation of concerns.
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
    private def createExtractionJob(lang : Language, extractorClasses: List[Class[_ <: Extractor]], parser : WikiParser) : ExtractionJob =
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
              // Sort by file name. In case of multistream dump chunks, the last file already has an ending root tag
              // Multistream chunks are NOT valid MediaWiki dump files:
              // - only the last chunk has a closing </mediawiki> tag
              // - chunks do not have a MediaWiki XML header
              // We must compose a valid XML stream to make it parseable by the DEF WikiParser
              val articlesFiles = files(config.source, finder, date).sorted

              // if config.xmlHeader != null we must wrap the content
              val streams = articlesFiles.zipWithIndex.map { case (file, i) =>

                if (config.xmlHeader == null) {
                  stream(file)
                }
                else {

                  val s = new SequenceInputStream(
                    stream(finder.file(date,config.xmlHeader)),
                    stream(file))

                  // Add a closing root tag if this is not the last chunk
                  if (i != (articlesFiles.size - 1)) new SequenceInputStream(s, new ByteArrayInputStream("</mediawiki>".getBytes()))
                  else s

                }
              }

              val articlesReaders = streams.map(stream => () => new InputStreamReader(stream, UTF8))

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
        val extractor = CompositeExtractor.load(extractorClasses, context)
        val datasets = extractor.datasets
        
        var formats = new ArrayBuffer[Destination]()
        for ((suffix, format) <- config.formats) {
          
          val destinations = new HashMap[String, Destination]()
          for (dataset <- datasets) {
            val file = finder.file(date, dataset.name.replace('_', '-')+'.'+suffix)
            destinations(dataset.name) = new WriterDestination(writer(file), format)
          }
          
          formats += new DatasetDestination(destinations)
        }
        
        var destination: Destination = new CompositeDestination(formats.toSeq: _*)
        destination = new MarkerDestination(destination, finder.file(date, Extraction.Complete), false)
        
        val description = lang.wikiCode+": "+extractorClasses.size+" extractors ("+extractorClasses.map(_.getSimpleName).mkString(",")+"), "+datasets.size+" datasets ("+datasets.mkString(",")+")"
        new ExtractionJob(new RootExtractor(extractor), context.articlesSource, config.namespaces, destination, lang.wikiCode, description, parser)
    }
    
    private def writer(file: File): () => Writer = {
      val zip = zipper(file.getName)
      () => new OutputStreamWriter(zip(new FileOutputStream(file)), UTF8)
    }

    private def stream(file: File): InputStream = {
      val unzip = unzipper(file.getName)
      unzip(new FileInputStream(file))
    }

    private def reader(file: File): () => Reader = {
      val unzip = unzipper(file.getName)
      () => new InputStreamReader(unzip(new FileInputStream(file)), UTF8)
    }

    private def readers(source: String, finder: Finder[File], date: String): List[() => Reader] = {

      files(source, finder, date).map(reader(_))
    }

    private def files(source: String, finder: Finder[File], date: String): List[File] = {

      if (config.source.startsWith("@")) { // the articles source is a regex - we want to match multiple files
        finder.matchFiles(date, config.source.substring(1))
      } else List(finder.file(date, config.source))
    }

    /**
     * @return stream zipper function
     */
    private def zipper(name: String): OutputStream => OutputStream = {
      zippers.getOrElse(suffix(name), identity)
    }
    
    /**
     * @return stream zipper function
     */
    private def unzipper(name: String): InputStream => InputStream = {
      unzippers.getOrElse(suffix(name), identity)
    }
    
    /**
     * @return file suffix
     */
    private def suffix(name: String): String = {
      name.substring(name.lastIndexOf('.') + 1)
    }
    
    private val zippers = Map[String, OutputStream => OutputStream] (
      "gz" -> { new GZIPOutputStream(_) }, 
      "bz2" -> { new BZip2CompressorOutputStream(_) } 
    )
    
    private val unzippers = Map[String, InputStream => InputStream] (
      "gz" -> { new GZIPInputStream(_) }, 
      "bz2" -> { new BZip2CompressorInputStream(_, true) } 
    )
    
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


package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.{XMLSource,WikiSource}
import org.dbpedia.extraction.wikiparser.{Namespace,PageNode,WikiParser}
import org.dbpedia.extraction.dump.download.Download
import org.dbpedia.extraction.util.{Language,Finder}
import org.dbpedia.extraction.util.RichFile.toRichFile
import scala.collection.mutable.{ArrayBuffer,HashMap}
import java.util.Properties
import java.io._
import java.nio.charset.Charset
import java.net.URL
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream
import java.util.zip.GZIPOutputStream

/**
 * Loads the dump extraction configuration.
 * 
 * TODO: clean up. The relations between the objects, classes and methods have become a bit chaotic.
 * There is no clean separation of concerns.
 */
class ConfigLoader(config: Config)
{
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
      config.extractorClasses.view.map(e => createExtractionJob(e._1, e._2))
    }
    
    private val parser = WikiParser()
    
    /**
     * Creates ab extraction job for a specific language.
     */
    private def createExtractionJob(lang : Language, extractorClasses: List[Class[_ <: Extractor]]) : ExtractionJob =
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
                XMLSource.fromFile(finder.file(date, config.source), language,                    
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
        val extractor = CompositeExtractor.load(extractorClasses, context)
        val datasets = extractor.datasets
        
        val charset = Charset.forName("UTF-8")
        
        var formats = new ArrayBuffer[Destination]()
        for ((suffix, format) <- config.formats) {
          
          val destinations = new HashMap[Dataset, Destination]()
          for (dataset <- datasets) {
            val file = finder.file(date, dataset.fileName+'.'+suffix)
            val zip = zipper(suffix)
            val open = () => new OutputStreamWriter(zip(new FileOutputStream(file)), charset)
            destinations(dataset) = new WriterDestination(open, format)
          }
          
          formats += new DatasetDestination(destinations)
        }
        
        var destination: Destination = new CompositeDestination(formats.toSeq: _*)
        destination = new MarkerDestination(destination, finder.file(date, Extraction.Complete), false)
        
        val jobLabel = lang.wikiCode+" ("+extractorClasses.size+" extractors, "+datasets.size+" datasets)"
        new ExtractionJob(new RootExtractor(extractor), context.articlesSource, destination, jobLabel)
    }

    /**
     * @return stream zipper function
     */
    private def zipper(suffix: String): OutputStream => OutputStream = {
      val dot = suffix.lastIndexOf('.')
      val ext = suffix.substring(dot + 1)
      zippers.get(ext) match {
        case Some(zipper) => zipper
        case None => identity
      }
    }
    
    private val zippers = Map[String, OutputStream => OutputStream] (
      "gz" -> { new GZIPOutputStream(_) }, 
      "bz2" -> { new BZip2CompressorOutputStream(_) } 
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
      val finder = new Finder[File](config.dumpDir, Language("commons"))
      val date = latestDate(finder)
      val file = finder.file(date, config.source)
      XMLSource.fromFile(file, Language.Commons, _.namespace == Namespace.File)
    }
    
    private def latestDate(finder: Finder[_]): String = {
      val fileName = if (config.requireComplete) Download.Complete else config.source
      val dates = finder.dates(fileName)
      if (dates.isEmpty) throw new IllegalArgumentException("found no directory with file '"+finder.wikiName+"-[YYYYMMDD]-"+fileName+"'")
      dates.last
    }
    
}
package org.dbpedia.extraction.dump.extract

import java.io._
import java.net.URL
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

import org.dbpedia.extraction.destinations._
import org.dbpedia.extraction.dump.download.Download
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.sources.{WikiSource, XMLSource}
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.wikiparser._

import scala.collection.convert.decorateAsScala._
import scala.collection.mutable.{ArrayBuffer, HashMap}

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

  private val extractionJobs = new ConcurrentHashMap[Language, ExtractionJob]().asScala

  private var extractionRecorder: ExtractionRecorder[WikiPage] = null

  def getExtractionRecorder: ExtractionRecorder[WikiPage] =
    if(extractionRecorder != null)
      extractionRecorder
    else {
      extractionRecorder = config.logDir match {
      case Some(p) => {
        var logname = config.configPath.replace("\\", "/")
        if (logname.indexOf("/") >= 0)
          logname = logname.substring(logname.lastIndexOf("/") + 1) + "." + Calendar.getInstance.getTimeInMillis + ".log"
        val logFile = new File(p, logname)
        logFile.createNewFile()
        val logStream = new FileOutputStream(logFile)

        //TODO val preamble = input._1.wikiCode+": "+input._2.size+" extractors ("+
        //  input._2.map(_.getSimpleName).mkString(",")+"), "+
        //  datasets.size+" datasets ("+datasets.mkString(",")+")"

        new ExtractionRecorder[WikiPage](new OutputStreamWriter(logStream), 2000, null)
      }
      case None => new ExtractionRecorder[WikiPage]()
    }
    extractionRecorder
  }

  /**
    * Creates ab extraction job for a specific language.
    */
  val extractionJobWorker = SimpleWorkers(config.parallelProcesses, config.parallelProcesses) { input: (Language,  Seq[Class[_ <: Extractor[_]]]) =>

    val finder = new Finder[File](config.dumpDir, input._1, config.wikiName)

    val date = latestDate(finder)

    //Extraction Context
    val context = new DumpExtractionContext
    {
      def ontology = _ontology

      def commonsSource = _commonsSource

      def language = input._1

      private lazy val _mappingPageSource =
      {
        val namespace = Namespace.mappings(language)

        if (config.mappingsDir != null && config.mappingsDir.isDirectory)
        {
          val file = new File(config.mappingsDir, namespace.name(Language.Mappings).replace(' ','_')+".xml")
          XMLSource.fromFile(file, Language.Mappings)
        }
        else
        {
          val namespaces = Set(namespace)
          val url = new URL(Language.Mappings.apiUri)
          WikiSource.fromNamespaces(namespaces,url,Language.Mappings)
        }
      }

      def mappingPageSource : Traversable[WikiPage] = _mappingPageSource

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
            title.namespace == Namespace.Category || title.namespace == Namespace.Template ||
            title.namespace == Namespace.WikidataProperty || ExtractorUtils.titleContainsCommonsMetadata(title))
      }

      def articlesSource = _articlesSource

      private val _redirects =
      {
        finder.file(date, "template-redirects.obj") match{
          case Some(cache) => Redirects.load(articlesSource, cache, language)
          case None => new Redirects(Map())
        }

      }

      def redirects : Redirects = _redirects

      private val _disambiguations =
      {
        try {
          Disambiguations.load(reader(finder.file(date, config.disambiguations).get), finder.file(date, "disambiguations-ids.obj").get, language)
        } catch {
          case ex: Exception =>
            logger.info("Could not load disambiguations - error: " + ex.getMessage)
            null
        }
      }

      def disambiguations : Disambiguations =
        if (_disambiguations != null)
          _disambiguations
        else
          new Disambiguations(Set[Long]())

      def configFile: Config = config
    }

    //Extractors
    val extractor = CompositeParseExtractor.load(input._2, context)
    val datasets = extractor.datasets

    val formatDestinations = new ArrayBuffer[Destination]()

    for ((suffix, format) <- config.formats) {
      val datasetDestinations = new HashMap[String, Destination]()
      for (dataset <- datasets) {
        finder.file(date, dataset.encoded.replace('_', '-')+'.'+suffix) match{
          case Some(file)=> datasetDestinations(dataset.encoded) = new DeduplicatingDestination(new WriterDestination(writer(file), format))
          case None =>
        }
      }
      formatDestinations += new DatasetDestination(datasetDestinations)
    }

    val destination = new MarkerDestination(
      new CompositeDestination(formatDestinations.toSeq: _*),
      finder.file(date, Extraction.Complete).get,
      false
    )

    val extractionJobNS = if(input._1 == Language.Commons) ExtractorUtils.commonsNamespacesContainingMetadata else config.namespaces

    extractionJobs.put(input._1, new ExtractionJob(extractor, context.articlesSource, extractionJobNS, destination, input._1, config.retryFailedPages, getExtractionRecorder))
  }

    /**
     * Loads the configuration and creates extraction jobs for all configured languages.
      *
      * @return Non-strict Traversable over all configured extraction jobs i.e. an extractions job will not be created until it is explicitly requested.
     */
    def getExtractionJobs: Traversable[ExtractionJob] =
    {
      // Create a non-strict view of the extraction jobs
      // non-strict because we want to create the extraction job when it is needed, not earlier
      val zw = config.extractorClasses.view.map(e => (e._1, e._2)).toList
      Workers.work[(Language,  Seq[Class[_ <: Extractor[_]]])](extractionJobWorker, zw)
      extractionJobs.values
    }

    private def writer(file: File): () => Writer = {
      () => IOUtils.writer(file)
    }

    private def reader(file: File): () => Reader = {
      () => IOUtils.reader(file)
    }

    private def readers(source: String, finder: Finder[File], date: String): List[() => Reader] = {
      files(source, finder, date).map(reader)
    }

    private def files(source: String, finder: Finder[File], date: String): List[File] = {

      val files = if (source.startsWith("@")) { // the articles source is a regex - we want to match multiple files
        finder.matchFiles(date, source.substring(1))
      } else List(finder.file(date, source)).collect{case Some(x) => x}

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


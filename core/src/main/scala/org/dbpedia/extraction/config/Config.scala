package org.dbpedia.extraction.config

import java.io.{File, FileOutputStream, OutputStreamWriter, Writer}
import java.net.URL
import java.text.SimpleDateFormat
import java.util.{Calendar, Properties}
import java.util.logging.{Level, Logger}

import org.dbpedia.extraction.config.Config.{AbstractParameters, MediaWikiConnection, NifParameters, SlackCredentials}
import org.dbpedia.extraction.config.ConfigUtils._
import org.dbpedia.extraction.config.provenance.Dataset
import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.destinations.formatters.UriPolicy._
import org.dbpedia.extraction.mappings.{ExtractionMonitor, Extractor}
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.wikiparser.Namespace

import scala.collection.Map
import scala.collection.mutable.ListBuffer
import scala.io.Codec
import scala.util.{Failure, Success, Try}

/**
  * Documentation of config values
  *
  * TODO universal.properties is loaded and then overwritten by the job specific config, however,
  * we are working on removing universal properties by setting (and documenting) sensible default values
  * here, that CAN be overwritten in job sepcific config
  *
  * Guideline:
  * 1. Use Java/Scaladoc always
  * 2. Parameters (lazy val) MUST be documented in the following manner:
  *   1. provide info about how the parameter works
  *   2. describe all checks done, i.e. fail on null or >300
  *   3. state the default value
  * 3. Parameters MUST follow this pattern:
  *   1. if the config param is called "base-dir" then the param MUST be "baseDir"
  *   2. i.e. replace - by CamelCase, since "-" can not be used in scala
  *
  *
  * TODO @Fabian please:
  * * go through universal properties and other configs and move all comments here
  * * after removing place a comment in the property file refering to http://temporary.dbpedia.org/scaladocs/#org.dbpedia.extraction.config.Config
  * * local extraction-framework/core/target/site/scaladocs after mvn install in core
  * * set default values according to universal.properties
  * * try to FOLLOW THE GUIDELINES above, add TODO if unclear
  * * if possible, move all def functions to ConfigUtils
  * * check the classes using the params for validation checks and move them here
  *
  *
  * TODO:
  * parallelprocesses was 4 before, changed to 8 now -> ok?
  *
  * dumpdir: changed defaultvalue from 'wikidumps' to 'home/marvin/.../basedir' -> ok?
  *
  * dbpediaversion: unclear how to solve
  *
  * how to treat ontology and mappings?
  *
  * all of Extraction Monitor: compare-dataset-ids... ?
  *
  * how does it set the policies?
  * If a uri policy (like uri-policy.iri) is not found in parsePolicies it returns an exception?
  * In universal.properties many uri-policies are commented out, but it still doesnt throw an exception, why?
  * */
class Config(val configPath: String) extends
  Properties(Config.universalProperties) with java.io.Serializable
{

  /**
    * Merge and overrides all universal property values
    * TODO can configPath ever be null?
    */
  if(configPath != null)
    this.putAll(ConfigUtils.loadConfig(configPath))

  @transient private val logger = Logger.getLogger(getClass.getName)

  /**
    * get all universal properties, check if there is an override in the provided config file
    */

  /**
   * Source file. If source file name ends with .gz or .bz2, it is unzipped on the fly.
   * Must exist in the directory xxwiki/yyyymmdd and have the prefix xxwiki-yyyymmdd-
   * where xx is the wiki code and yyyymmdd is the dump date.
   *
   * Extract from part files:
   * Please make sure that the regex actually matches the format used by ALL the wikis you are going to extract from!!!!
   * One that should work in all cases is
   * source=@pages-articles\\d*\\.xml(-p\\d+p\\d+)?\\.bz2
   *
   * NOTE: when using the above regex you should make sure you do not have part files AND regular dump files together
   * for the same wiki, e.g. frwiki-20131120-pages-articles1.xml.bz2 and frwiki-20131120-pages-articles.xml.bz2, as they
   * BOTH will match and that will result in duplicate output data
   *
   * Example:
   * enwiki => enwiki-latest-pages-articles1.xml-p000000010p000010000.bz2 hence @pages-articles\\d+\\.xml-p\\d+p\\d+\\.bz2 matches
   * frwiki => frwiki-latest-pages-articles1.xml.bz2 hence @pages-articles\\d+\\.xml\\.bz2 matches (the previous regex does not!)
   * commonswiki => it does not have part files! This is true for other wikis as well.
   *
   * source=@pages-articles\\d+\\.xml-p\\d+p\\d+\\.bz2
   *
   * In case of multistream chunks
   * source=@pages-articles-multistream\\.xml\\.\\d+\\.bz2
   *
   *
   *@example source=pages-articles-multistream.xml.bz2
   */
  lazy val source: Seq[String] = {
    val temp = getStrings(this, "source", ",")
    if (temp.isEmpty) Seq("pages-articles-multistream.xml.bz2")
    else temp
  }
//  lazy val source: Seq[String] = getStrings(this, "source", ",", required = true)


  /**
   * wiki suffix
   * @example wiki-name=wiki
   */
  lazy val wikiName: String = Try(getString(this, "wiki-name", required = true).trim).getOrElse("wiki")

  /**
   * If we need to Exclude Non-Free Images in this Extraction, set this to true
   */
  lazy val copyrightCheck: Boolean = Try(this.getProperty("copyrightCheck", "false").toBoolean).getOrElse(false)

  /**
    * baseDir gives either an absolute path or a relative path to where all data is stored, normally wikidumps are downloaded here and extracted data is saved next to it, created folder structure is {{lang}}wiki/$date
    * Replace with your Wikipedia dump download directory (should not change over the course of a release)
   *
    * DEV NOTE:
    * 1. this must stay lazy as it might not be used or creatable in the SPARK extraction
    * 2. Download.scala in core does the creation
    *
    * TODO rename dumpDir to baseDir
    *
    * @example base-dir=./wikidumps
   */
  lazy val dumpDir: File = {
    var x:File = getValue[File](this, "base-dir", required = true){ x => new File(x)}
    if (x==null.asInstanceOf[File]){
       x = new File ("/home/marvin/workspace/active/basedir")
    }
    x
  }

  /**
    * Parallel Disc Processes: indicates how many parallel extraction processes can be executed.
    * Depends on the number of cores, type of disk and IO speed
    *
   * @example parallel-processes=8
    */
  lazy val parallelProcesses: Int = this.getProperty("parallel-processes", "8").trim.toInt

  /**
   * spark master for the SparkExtraction
   * @example spark-master=local[*]
   */
  lazy val sparkMaster: String = Option(getString(this, "spark-master")).getOrElse("local[*]")

  /**
   * spark local dir for the SparkExtraction
   * @example spark-local-dir=/home/extractor/data/
   */
  lazy val sparkLocalDir: String = Option(getString(this, "spark-local-dir")).getOrElse("/home/extractor/data/")

  /**
    * Normally extraction jobs are run sequentially (one language after the other), but for some jobs it makes sense to run these in parallel.
    * This only should be used if a single extraction job does not take up the available computing power.
    */
  val runJobsInParallel: Boolean = this.getProperty("run-jobs-in-parallel", "false").trim.toBoolean

  /**
    * The version string of the DBpedia version being extracted (in this format: YYYY-MM)
   *  NOTE will use current YYYY-MM if not set
   */
  lazy val dbPediaVersion: String = parseVersionString(getString(this, "dbpedia-version").trim) match{
    case Success(s) => s
    case Failure(e) =>  {
      val version = new SimpleDateFormat("yyyy-MM").format(Calendar.getInstance().getTime)
      logger.info(s"dbpedia-version option in universal.properties was not defined using $version")
      version
    }
     // throw new IllegalArgumentException("dbpedia-version option in universal.properties was not defined or in a wrong format", e)
  }


  /**
   * Wikidata mapping file created by the WikidataSubClassOf script
   */
  lazy val wikidataMappingsFile: File = {
    val name = this.getProperty("wikidata-property-mappings-file", "wikidata-property-mappings.json").trim
    new File(dumpDir, name)
  }

  /**
    * log file directory: used to store all log files created in the course of all extractions
    */
  lazy val logDir: Option[File] = Option(getString(this, "log-dir")) match {
    case Some(x) => Some(new File(x))
    case None => Some(new File("/home/marvin/workspace/active/logdir"))
  }

  /**
   *
   * @param lang
   * @param interval
   * @param preamble
   * @param writer
   * @param datasets
   * @param monitor
   * @tparam T
   * @return
   */
  def getDefaultExtractionRecorder[T](lang: Language, interval: Int = 100000, preamble: String = null, writer: Writer = null, datasets : ListBuffer[Dataset] =  ListBuffer[Dataset](), monitor : ExtractionMonitor = null): ExtractionRecorder[T] ={
    val w = if(writer != null) writer
      else openLogFile(lang.wikiCode) match{
        case Some(s) => new OutputStreamWriter(s)
        case None => null
      }
    new ExtractionRecorder[T](w, interval, preamble, slackCredentials.getOrElse(null), datasets, lang, monitor)
  }

  /**
   *
   * @param langWikiCode
   * @return
   */
  private def openLogFile(langWikiCode: String): Option[FileOutputStream] ={
    logDir match{
      case Some(p) if p.exists() =>
        var logname = configPath.replace("\\", "/")
        logname = logname.substring(logname.lastIndexOf("/") + 1)
        logname = logname + "_" + langWikiCode + ".log"
        val logFile = new File(p, logname)
        logFile.createNewFile()
        Option(new FileOutputStream(logFile))
      case _ => None
    }
  }

  /**
    * If set, extraction summaries are forwarded via the API of Slack, displaying messages on a dedicated channel.
    * The URL of the slack webhook to be used
    * the username under which all messages are posted (has to be registered for this webhook?)
    * Threshold of extracted pages over which a summary of the current extraction is posted
    * Threshold of exceptions over which an exception report is posted
    *
    * {{{
    * Default values:
    * slack-webhook=https://hooks.slack.com/services/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
    * slack-username=username
    * slack-summary-threshold=1000000
    * slack-exception-threshold=10
    * }}}
    */
  lazy val slackCredentials = Try{
      SlackCredentials(
        webhook = new URL(Option(getString(this, "slack-webhook")).getOrElse("https://hooks.slack.com/services/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").trim),
        username = Option(this.getProperty("slack-username")).getOrElse("username").trim,
        summaryThreshold = Option(this.getProperty("slack-summary-threshold")).getOrElse("1000000").trim.toInt,
        exceptionThreshold = Option(this.getProperty("slack-exception-threshold")).getOrElse("10").trim.toInt
      )}

  /**
    * Local ontology file, downloaded for speed and reproducibility
    * Note: This is lazy to defer initialization until actually called (eg. this class is not used
    * directly in the distributed extraction framework - DistConfig.ExtractionConfig extends Config
    * and overrides this val to null because it is not needed)
   *
   * # if ontology and mapping files are not given or do not exist,
   * # download info from mappings.dbpedia.org
   * # by default both should be in the root folder ../
    *
    * @example ontology=../ontology.xml
    */
  lazy val ontologyFile: File = getValue(this, "ontology")(new File(_))


  /**
    * Local mappings files, downloaded for speed and reproducibility
    * Note: This is lazy to defer initialization until actually called (eg. this class is not used
    * directly in the distributed extraction framework - DistConfig.ExtractionConfig extends Config
    * and overrides this val to null because it is not needed)
    *
    * @example mappings=../mappings
    */
  lazy val mappingsDir: File = getValue(this, "mappings")(new File(_))

  /**
   * Serialization URI policies and file formats. Quick guide:
   * uriPolicy keys: uri, generic, xml-safe, reject-long
   * uriPolicy position modifiers: -subjects, -predicates, -objects, -datatypes, -contexts
   * uriPolicy values: comma-separated languages or '*' for all languages
   * format values: n-triples, n-quads, turtle-triples, turtle-quads, trix-triples, trix-quads
   * See http://git.io/DBpedia-serialization-format-properties for details.
   *
   * For backwards compatibility, en uses generic URIs. All others use local IRIs.
   * However, in the input files English already uses the generic domain, thus URI encoding gets enabled for the generic domain
   * uri-policy.uri=uri:en; generic:en; xml-safe-predicates:*; reject-long:*
   */
  lazy val policies: Map[String, Array[Policy]] = parsePolicies(this, "uri-policy")

  /**
   * format values: n-triples, n-quads, turtle-triples, turtle-quads, trix-triples, trix-quads
   */
  lazy val formats: Map[String, Formatter] = parseFormats(this, "format", policies).toMap

  /**
   * disambiguations file (default: "page_props.sql.gz")
   */
  lazy val disambiguations: String = this.getProperty("disambiguations", "page_props.sql.gz")


  /**
    * all non universal properties...
    */
  /**
    * An array of input dataset names (e.g. 'instance-types' or 'mappingbased-literals') (separated by a ',')
    */
  lazy val inputDatasets: Seq[String] = getStrings(this, "input", ",").distinct   //unique collection of names!!!
  /**
    * A dataset name for the output file generated (e.g. 'instance-types' or 'mappingbased-literals')
    */
  lazy val outputDataset: Option[String] = Option(getString(this, "output"))
  /**
    * the suffix of the files representing the input dataset (usually a combination of RDF serialization extension and compression used - e.g. .ttl.bz2 when using the TURTLE triples compressed with bzip2)
    */
  lazy val inputSuffix: Option[String] = Option(getString(this, "suffix"))
  /**
    * same as for inputSuffix (for the output dataset)
    */
  lazy val outputSuffix: Option[String] = Option(getString(this, "output-suffix"))
  /**
    * instead of a defined output dataset name, one can specify a name extension turncated at the end of the input dataset name (e.g. '-transitive' -> instance-types-transitive)
    */
  lazy val datasetnameExtension: Option[String] = Option(getString(this, "name-extension"))

  /**
    * An array of languages specified by the exact enumeration of language wiki codes (e.g. en,de,fr...)
    * or article count ranges ('10000-20000' or '10000-' -> all wiki languages having that much articles...)
    * or '@mappings', '@chapters' when only mapping/chapter languages are of concern
    * or '@downloaded' if all downloaded languages are to be processed (containing the download.complete file)
    * or '@abstracts' to only process languages which provide human readable abstracts (thus not 'wikidata' and the like...)
    */
  lazy val languages: Array[Language] = parseLanguages(dumpDir, getStrings(this, "languages", ","), wikiName)

  /**
    * before processing a given language, check if the download.complete file is present
    */
  lazy val requireComplete: Boolean = this.getProperty("require-download-complete", "false").trim.toBoolean



  /**
    * TODO experimental, ignore for now
    */
  lazy val retryFailedPages: Boolean = this.getProperty("retry-failed-pages", "false").trim.toBoolean

  /**
    * the extractor classes to be used when extracting the XML dumps
    */
  lazy val extractorClasses: Map[Language, Seq[Class[_ <: Extractor[_]]]] = ExtractorUtils.loadExtractorsMapFromConfig(languages, this)

  /**
    * namespaces loaded defined by the languages in use (see languages)
    */
  lazy val namespaces: Set[Namespace] = loadNamespaces()

  /**
   * load namespaces defined by the languages in use
   *
   * @return set of namespaces
   */
  private def loadNamespaces(): Set[Namespace] = {
    val names = getStrings(this, "namespaces", ",")
    if (names.isEmpty) Set(Namespace.Main, Namespace.File, Namespace.Category, Namespace.Template, Namespace.WikidataProperty)
    // Special case for namespace "Main" - its Wikipedia name is the empty string ""
    else names.map(name => if (name.toLowerCase(Language.English.locale) == "main") Namespace.Main else Namespace(Language.English, name)).toSet
  }

  /**
   *
   */
  lazy val mediawikiConnection: MediaWikiConnection = Try {
    MediaWikiConnection(
      apiUrl = this.getProperty("mwc-apiUrl", "").trim,
      maxRetries = this.getProperty("mwc-maxRetries", "4").trim.toInt,
      connectMs = this.getProperty("mwc-connectMs", "2000").trim.toInt,
      readMs = this.getProperty("mwc-readMs", "5000").trim.toInt,
      sleepFactor = this.getProperty("mwc-sleepFactor", "1000").trim.toInt
    )
  } match{
    case Success(s) => s
    case Failure(f) => throw new IllegalArgumentException("Not all necessary parameters for the 'MediaWikiConnection' class were provided or could not be parsed to the expected type.", f)
  }

  /**
   * parameters specific for the abstract extraction
   *
   * abstractQuery query for the abstract extraction
   * abstractTags is the path of the XML tags under which the result is expected
   * shortAbstractsProperty is the property used to specify short abstracts (should not change)
   * longAbstractsProperty is the property used to specify long abstracts (should not change)
   * shortAbstractMinLength specifies the minimal length of the short abstract
   */
  lazy val abstractParameters: AbstractParameters = Try {
    AbstractParameters(
      abstractQuery = this.getProperty("abstract-query", "&format=xml&action=query&prop=extracts&exintro=&explaintext=&titles=%s").trim,
      shortAbstractsProperty = this.getProperty("short-abstracts-property", "rdfs:comment").trim,
      longAbstractsProperty = this.getProperty("long-abstracts-property", "abstract").trim,
      shortAbstractMinLength = this.getProperty("short-abstract-min-length", "200").trim.toInt,
      abstractTags = this.getProperty("abstract-tags", "query,pages,page,extract").trim
    )
  } match{
    case Success(s) => s
    case Failure(f) => throw new IllegalArgumentException("Not all necessary parameters for the 'AbstractParameters' class were provided or could not be parsed to the expected type.", f)
  }


  /**
   * Parameters specific to the nif extraction
   *
   * nifQuery
   * nifTags the xml path of the response
   * nifIsTestRun will leave out the long and short abstract datasets
   * nifWriteAnchor will write all anchor texts for each nif instance
   * nifWriteLinkAnchor write only the anchor text for link instances
   * nifExtractAbstractOnly only extract abstract (not the whole page)
    *
   */
  lazy val nifParameters: NifParameters = Try {
    NifParameters(
      nifQuery = this.getProperty("nif-query", "&format=xml&action=parse&prop=text&page=%s&pageid=%d").trim,
      nifTags = this.getProperty("nif-tags", "parse,text").trim,
      isTestRun = this.getProperty("nif-isTestRun", "false").trim.toBoolean,
      writeAnchor = this.getProperty("nif-write-anchor", "false").trim.toBoolean,
      writeLinkAnchor = this.getProperty("nif-write-link-anchor", "true").trim.toBoolean,
      abstractsOnly = this.getProperty("nif-extract-abstract-only", "true").trim.toBoolean,
      cssSelectorMap = this.getClass.getClassLoader.getResource("nifextractionconfig.json") //static config file in core/src/main/resources
    )
  } match{
    case Success(s) => s
    case Failure(f) => throw new IllegalArgumentException("Not all necessary parameters for the 'NifParameters' class were provided or could not be parsed to the expected type.", f)
  }

  /* HELPER FUNCTIONS */


  /**
   * getter of an arbitrary string property
   *
   * @param key property key
   * @return property
   */
  def getArbitraryStringProperty(key: String): Option[String] = {
    Option(getString(this, key))
  }

  /**
   * checks if a property in the provided .property file is missing or has an invalid format
   *
   * @param property property of .properties file
   * @param required property is required
   */
  def throwMissingPropertyException(property: String, required: Boolean): Unit ={
    if(required)
      throw new IllegalArgumentException("The following required property is missing from the provided .properties file (or has an invalid format): '" + property + "'")
    else
      logger.log(Level.WARNING, "The following property is missing from the provided .properties file (or has an invalid format): '" + property + "'. It will not factor in.")
  }


  /**
    * determines if 1. the download has to be completed and if so 2. looks for the download-complete file
    * @param lang - the language for which to check
    * @return
    */
  def isDownloadComplete(lang:Language): Boolean ={
    if(requireComplete){
      val finder = new Finder[File](dumpDir, lang, wikiName)
      val date = finder.dates(source.head).last
      finder.file(date, Config.Complete) match {
        case None => false
        case Some(x) => x.exists()
      }
    }
    else
      true
  }


}

object Config{

  /** name of marker file in wiki date directory */
  val Complete = "download-complete"

  /**
   * @param nifQuery the request query string
   * @param nifTags the xml path of the response
   * @param isTestRun will leave out the long and short abstract datasets
   * @param writeAnchor will write all anchor texts for each nif instance
   * @param writeLinkAnchor write only the anchor text for link instances
   * @param abstractsOnly only extract abstract (not the whole page)
   * @param cssSelectorMap
   */
  case class NifParameters(
    nifQuery: String,
    nifTags: String,
    isTestRun: Boolean,
    writeAnchor: Boolean,
    writeLinkAnchor: Boolean,
    abstractsOnly: Boolean,
    cssSelectorMap: URL
  )

  /**
    * test
    * @constructor test
    * @param apiUrl test
    * @param maxRetries
    * @param connectMs
    * @param readMs
    * @param sleepFactor
    */
  case class MediaWikiConnection(
    apiUrl: String,
    maxRetries: Int,
    connectMs: Int,
    readMs: Int,
    sleepFactor: Int
  )

  /**
   *
   * @param abstractQuery query for the abstract extraction
   * @param shortAbstractsProperty the properties used to specify short abstracts (should not change)
   * @param longAbstractsProperty the properties used to specify long abstracts (should not change)
   * @param shortAbstractMinLength the short abstract is at least this long
   * @param abstractTags the tag path of the XML tags under which the result is expected
   */
  case class AbstractParameters(
    abstractQuery: String = "foo",
    shortAbstractsProperty: String,
    longAbstractsProperty: String,
    shortAbstractMinLength: Int,
    abstractTags: String
  )

  /**
   * @param webhook URL of the slack webhook to be used
   * @param username username under which all messages are posted (has to be registered for this webhook?)
   * @param summaryThreshold Threshold of extracted pages over which a summary of the current extraction is posted
   * @param exceptionThreshold Threshold of exceptions over which an exception report is posted
   */
  case class SlackCredentials(
     webhook: URL,
     username: String,
     summaryThreshold: Int,
     exceptionThreshold: Int
   )

  /**
   * loads universal properties
   */
  private val universalProperties: Properties = loadConfig(this.getClass.getClassLoader.getResource("universal.properties")).asInstanceOf[Properties]

  /**
   * creates universal config
   */
  val universalConfig: Config = new Config(null)


  /**
    * The content of the wikipedias.csv in the base-dir (needs to be static)
    *
    * @example base-dir=/home/marvin/workspace/active/basedir
    */
  private val wikisinfoFile = new File(Option(getString(universalProperties , "base-dir")).getOrElse("/home/marvin/workspace/active/basedir"), WikiInfo.FileName)

  private lazy val wikiinfo = if(wikisinfoFile.exists()) WikiInfo.fromFile(wikisinfoFile, Codec.UTF8) else Seq()

  def wikiInfos: Seq[WikiInfo] = wikiinfo
}

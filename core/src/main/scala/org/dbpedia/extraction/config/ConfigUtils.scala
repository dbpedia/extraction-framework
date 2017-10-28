package org.dbpedia.extraction.config

import java.io.{File, FileInputStream, InputStream, InputStreamReader}
import java.net.URL
import java.util.Properties

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper, ObjectReader}
import org.dbpedia.extraction.config.mappings.ImageExtractorConfig
import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.util.Language.wikiCodeOrdering
import org.dbpedia.extraction.util.RichString.wrapString
import org.dbpedia.extraction.util.{Language, RichFile}
import org.dbpedia.extraction.wikiparser.{Namespace, PageNode, WikiPage}

import scala.collection.immutable.SortedSet
import scala.collection.mutable
import scala.util.{Failure, Success, Try}
import scala.util.matching.Regex


object ConfigUtils {

  /**
    * Simple regex matching Wikipedia language codes.
    * Language codes have at least two characters, start with a lower-case letter and contain only
    * lower-case letters and dash, but there are also dumps for "wikimania2005wiki" etc.
    */
  val LanguageRegex: Regex = """([a-z][a-z0-9-]+)""".r

  /**
    * Regex used for excluding languages from the import.
    */
  val ExcludedLanguageRegex: Regex = """!([a-z][a-z0-9-]+)""".r

  /**
    * Regex for numeric range, both limits optional
    */
  val RangeRegex: Regex = """(\d*)-(\d*)""".r

  private [config] val propertiesFileKey = "properties-filename-9642"

  def loadConfig(filePath: String): Properties = {
    val file = new File(filePath)
    loadConfig(file.toURI.toURL)
  }

  def loadConfig(url: URL): Properties = {
    val props = loadFromStream(url.openStream())
    addPropertiesName(url.toString, props)
    props
  }

  private def addPropertiesName(uri: String, props: Properties)={
    var logname = uri.replace("\\", "/").trim
    logname = logname.substring(logname.lastIndexOf("/") + 1)
    if(logname.contains("."))
      logname = logname.substring(0, logname.indexOf("."))
    props.put(propertiesFileKey, logname)
  }

  def loadJsonComfig(url: URL): JsonNode ={
    val objectMapper = new ObjectMapper(new JsonFactory())
    val objectReader: ObjectReader = objectMapper.reader()
    val inputStream = url.openStream()
    val res = objectReader.readTree(inputStream)
    inputStream.close()
    res
  }

  private def loadFromStream(file: InputStream, charset: String = "UTF-8"): Properties ={
    val config = new Properties()
    try config.load(new InputStreamReader(file, charset))
    finally file.close()
    config
  }


  def getValues[T](config: Properties, key: String, sep: String, required: Boolean = false)(map: String => T): Seq[T] = {
    getStrings(config, key, sep, required).map(map(_))
  }

  def getStrings(config: Properties, key: String, sep: String, required: Boolean = false): Seq[String] = {
    val string = getString(config, key, required)
    if (string == null) Seq.empty
    else string.trimSplit(sep)
  }

  def getStringMap(config: Properties, key: String, sep: String, required: Boolean = false): Map[String, String] = {
    getStrings(config, key, sep, required).map(x => x.split("->")).map( y => y(0) -> y(1)).toMap
  }

  def getValue[T](config: Properties, key: String, required: Boolean = false)(map: String => T): T = {
    val string = getString(config, key, required)
    if (string == null) null.asInstanceOf[T]
    else map(string)
  }
  
  def getString(config: Properties, key: String, required: Boolean = false): String = {
    val string = config.getProperty(key)
    if (string != null) string
    else if (! required) null
    else throw new IllegalArgumentException("property '"+key+"' not defined.")
  }
  
  /**
   * @param baseDir directory of wikipedia.csv, needed to resolve article count ranges
   * @param args array of space- or comma-separated language codes or article count ranges
   * @return languages, sorted by language code
   */
  def parseLanguages(baseDir: File, args: Seq[String], wikiPostFix: String = "wiki"): Array[Language] = {
    if(!baseDir.exists())
      throw new IllegalArgumentException("Base directory does not exist yet: " + baseDir)
    
    val keys = for(arg <- args; key <- arg.split("[,\\s]"); if key.nonEmpty) yield key
        
    var languages = SortedSet[Language]()
    var excludedLanguages = SortedSet[Language]()
    
    val ranges = new mutable.HashSet[(Int,Int)]
  
    for (key <- keys) key match {
      case "@mappings" => languages ++= Namespace.mappingLanguages
      case "@chapters" => languages ++= Namespace.chapterLanguages
      case "@downloaded" => languages ++= downloadedLanguages(baseDir, wikiPostFix)
      case "@all" => languages ++= Language.map.values
      case "@abstracts" =>
        //@downloaded - Commons & Wikidata
        languages ++= downloadedLanguages(baseDir, wikiPostFix)
        excludedLanguages += Language.Commons
        excludedLanguages += Language.Wikidata
      case RangeRegex(from, to) => ranges += toRange(from, to)
      case LanguageRegex(language) => languages += Language(language)
      case ExcludedLanguageRegex(language) => excludedLanguages += Language(language)
      case other => throw new IllegalArgumentException("Invalid language / range '"+other+"'")
    }
    
    // resolve page count ranges to languages
    if (ranges.nonEmpty)
    {
      // Note: the file is in ASCII, any non-ASCII chars are XML-encoded like '&#231;'. 
      // There is no Codec.ASCII, but UTF-8 also works for ASCII. Luckily we don't use 
      // these non-ASCII chars anyway, so we don't have to unescape them.
      
      // for all wikis in one of the desired ranges...
      languages ++= (for ((from, to) <- ranges; lang <- Language.map.values; if from <= lang.pages && lang.pages <= to) yield lang)
    }

    languages --= excludedLanguages
    languages.toArray
  }

  private def downloadedLanguages(baseDir: File, wikiPostFix: String = "wiki"): Array[Language] = {
    (for (file <- baseDir.listFiles().filter(x => x.isDirectory)) yield
      Language.get(file.getName.replaceAll(wikiPostFix + "$", "").replace("_", "-")) match{
        case Some(l) => l
        case None => null
      }).filter(x => x != null)
  }

  def toRange(from: String, to: String): (Int, Int) = {
    val lo: Int = if (from.isEmpty) 0 else from.toInt
    val hi: Int = if (to.isEmpty) Int.MaxValue else to.toInt
    if (lo > hi) throw new NumberFormatException
    (lo, hi)
  }

  def parseVersionString(str: String): Try[String] =Try {
    Option(str) match {
      case Some(v) => "2\\d{3}-\\d{2}".r.findFirstMatchIn(v.trim) match {
        case Some(y) => if (y.end == 7) v.trim else throw new IllegalArgumentException("Provided version string did not match 2\\d{3}-\\d{2}")
        case None => throw new IllegalArgumentException("Provided version string did not match 2\\d{3}-\\d{2}")
      }
      case None => throw new IllegalArgumentException("No version string was provided.")
    }
  }

  /**
    * This function was extracted from the ImageExtractor object, since
    *  the free & nonfree images are now extracted before starting the extraction jobs
    * @param source pages_articles of a given language
    * @param wikiCode the wikicode of a given language
    * @return two lists: ._1: list of free images, ._2: list of nonfree images
    */
  def loadImages(source: Source, wikiCode: String, extractionRecorder: ExtractionRecorder[PageNode] = null): (Seq[String], Seq[String]) =
  {
    val freeImages = new mutable.HashSet[String]()
    val nonFreeImages = new mutable.HashSet[String]()

    for(page <- source if page.title.namespace == Namespace.File;
        ImageExtractorConfig.ImageLinkRegex() <- List(page.title.encoded) )
    {
      if(extractionRecorder != null) {
        val records = page.recordEntries
        //forward all records to the recorder
        extractionRecorder.record(records:_*)
      }
      ImageExtractorConfig.NonFreeRegex(wikiCode).findFirstIn(page.source) match
      {
        case Some(_) => nonFreeImages += page.title.encoded
        case None => if (freeImages != null) freeImages += page.title.encoded
      }
    }

    (freeImages.toSeq, nonFreeImages.toSeq)
  }


  def getBaseDir(properties: Properties): RichFile ={
    Try{new File(getString(properties , "base-dir", required = true))} match{
      case Success(d) => if (! d.exists)
        throw sys.error("dir "+d+" does not exist")
      else
        d
      case Failure(f) => throw f
    }
  }

  private def loadRichFile(properties: Properties, key: String, suffix: String = null, exists: Boolean = false): RichFile ={
    val baseDir = getBaseDir(properties)
    val fileStump = getString(properties, key, required = true)
    val fs = baseDir.getFile.getAbsoluteFile + "/" + fileStump + (if(suffix != null) suffix else "")
    loadRichFile(fs, exists)
  }

  def loadRichFile(file: String, exists: Boolean): RichFile ={
    Try{new File(file)} match{
      case Success(f) if !exists || exists && f.exists() => f
      case _ => sys.error(file + " does not exist!")
    }
  }

  def loadInputFile(properties: Properties, key: String, suffix: String = null): RichFile ={
    val suf = if(suffix != null) suffix else getString(properties, "suffix")
    loadRichFile(properties, key, suf, exists = true)
  }

  def loadOutputFile(properties: Properties, key: String, suffix: String = null): RichFile ={
    val suf = if(suffix != null) suffix else getString(properties, "output-suffix")
    loadRichFile(properties, key, suf)
  }
}
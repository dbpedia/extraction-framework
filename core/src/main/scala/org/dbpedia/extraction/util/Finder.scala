package org.dbpedia.extraction.util

import java.util.regex.Pattern
import Finder._

private object Finder {
  val datePattern = Pattern.compile("\\d{8}")
  def dateFilter(name: String) = datePattern.matcher(name).matches 
}

/**
 * Helps to find files and directories in a directory structure as used by the Wikipedia
 * dump download site, for example baseDir/enwiki/20120403/enwiki-20120403-pages-articles.xml.bz2
 */
class Finder[T](val baseDir: T, val language: Language)(implicit wrap: T => FileLike[T]) {
  
  def this(baseDir: String, language: Language)(implicit parse: String => T, wrap: T => FileLike[T]) = 
    this(parse(baseDir), language)
  
  /**
   * directory name/file prefix for language, e.g. "en" -> "enwiki"
   */
  val wikiName = language.filePrefix+"wiki"

  /**
   * Directory for language, e.g. "baseDir/enwiki"
   */
  val wikiDir = baseDir.resolve(wikiName)
  
  /**
   * date directory for language, e.g. "20120403" -> "baseDir/enwiki/20120403"
   */
  def directory(date: String) = wikiDir.resolve(date)
  
  /**
   * Finds the names (which are dates in format YYYYMMDD) of dump directories for the language.
   * @suffix Return only directories that contain a file with this suffix, 
   * e.g. "download-complete" -> "baseDir/enwiki/20120403/enwiki-20120403-download-complete". 
   * May be null, in which case we just look for date directories.
   * @return dates in ascending order
   */
  def dates(suffix: String = null, required: Boolean = true): List[String] = {
    
    val suffixFilter = 
      if (suffix == null) {date: String => true} 
      else {date: String => file(date, suffix).exists}
    
    val dates = wikiDir.names.filter(dateFilter).filter(suffixFilter).sortBy(_.toInt)
    
    if (required && dates.isEmpty) {
      var msg = "found no directory "+wikiDir+"/[YYYYMMDD]"
      if (suffix != null) msg += " containing file "+wikiName+"-[YYYYMMDD]-"+suffix
      throw new IllegalArgumentException(msg)
    }
    
    dates
  }
    
  /**
   * @return files in ascending date order
   */
  def files(suffix: String, required: Boolean = true): List[T] = {
    dates(suffix, required).map(file(_, suffix))
  }
    
  /**
   * File with given name suffix in main directory for language, e.g. "download-running" ->
   * "baseDir/enwiki/enwiki-download-running"
   */
  def file(suffix: String) = wikiDir.resolve(wikiName+'-'+suffix)
  
  /**
   * File with given name suffix in date directory for language, e.g. "pages-articles.xml" ->
   * "baseDir/enwiki/20120403/enwiki-20120403-pages-articles.xml"
   */
  def file(date: String, suffix: String) = directory(date).resolve(wikiName+'-'+date+'-'+suffix)
}
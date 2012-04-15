package org.dbpedia.extraction.util

import java.nio.file.{Path,Files,FileSystems}
import java.util.regex.Pattern
import scala.collection.JavaConversions.iterableAsScalaIterable
import PathFinder._

object PathFinder {
  val datePattern = Pattern.compile("\\d{8}")
  def dateFilter(name: String) = datePattern.matcher(name).matches 
}

class PathFinder(baseDir : Path, language : Language) {
  
  /**
   * 
   */
  val wikiName = language.filePrefix+"wiki"

  /**
   * Directory for language, e.g. "base/enwiki"
   */
  val wikiDir = baseDir.resolve(wikiName)
  
  /**
   * Finds the names (which is a date in format YYYYMMDD) of dump directories for the language.
   * @return dates in ascending order
   */
  def dates : List[String] = {
      val stream = Files.newDirectoryStream(wikiDir)
      try stream.toList.map(_.getFileName.toString).filter(dateFilter).sortBy(_.toInt) 
      finally stream.close
  }
    
  /**
   * File with given name suffix in latest directory for language, e.g. "pages-articles.xml" ->
   * "base/enwiki/20120403/enwiki-20120403-pages-articles.xml"
   */
  def directory(date: String) = wikiDir.resolve(date)
  
  /**
   * File with given name suffix in latest directory for language, e.g. "pages-articles.xml" ->
   * "base/enwiki/20120403/enwiki-20120403-pages-articles.xml"
   */
  def file(date: String, suffix: String) = directory(date).resolve(wikiName+'-'+date+'-'+suffix)
}


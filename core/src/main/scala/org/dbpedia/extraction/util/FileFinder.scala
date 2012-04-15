package org.dbpedia.extraction.util

import java.io.{File,IOException}

class FileFinder(baseDir : File, language : Language) {
  
  /**
   * 
   */
  val wikiName = language.filePrefix+"wiki"

  /**
   * Directory for language, e.g. "base/enwiki"
   */
  val wikiDir = new File(baseDir, wikiName)
  
  /**
   * Finds the names (which is a date in format YYYYMMDD) of dump directories for the language.
   * @return dates in ascending order
   */
  def dates : Array[String] = {
      val list = wikiDir.list
      if (list == null) throw new IOException("failed to list files in ["+wikiDir+"]")
      list.filter(_.matches("\\d{8}")).sortBy(_.toInt)
  }
    
  /**
   * File with given name suffix in latest directory for language, e.g. "pages-articles.xml" ->
   * "base/enwiki/20120403/enwiki-20120403-pages-articles.xml"
   */
  def directory(date: String) = new File(wikiDir, date)
  
  /**
   * File with given name suffix in latest directory for language, e.g. "pages-articles.xml" ->
   * "base/enwiki/20120403/enwiki-20120403-pages-articles.xml"
   */
  def file(date: String, suffix: String) = new File(directory(date), wikiName+'-'+date+'-'+suffix)
}
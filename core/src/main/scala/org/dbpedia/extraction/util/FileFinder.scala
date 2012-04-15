package org.dbpedia.extraction.util

import java.io.{File,IOException}

class FileFinder(baseDir : File) {
  
  /**
   * 
   */
  def wikiName(language : Language) : String = language.filePrefix+"wiki"

  /**
   * Directory for language, e.g. "base/enwiki"
   */
  def wikiDir(language : Language) : File = new File(baseDir, wikiName(language))
  
  /**
   * Latest directory for language, e.g. "base/enwiki/20120403"
   */
  def latestDir(language : Language) : File = new File(wikiDir(language), latestDate(language))
  
  /**
   * File with given name suffix in latest directory for language, e.g. "pages-articles.xml" ->
   * "base/enwiki/20120403/enwiki-20120403-pages-articles.xml"
   */
  def latestFile(language : Language, suffix: String) : File = {
    val name = wikiName(language)
    val date = latestDate(language)
    new File(baseDir, name+'/'+date+'/'+name+'-'+date+'-'+suffix)
  }
  
  /**
   * Finds the name (which is a date in format YYYYMMDD) of the latest wikipedia dump 
   * directory for the given language.
   */
  def latestDate(language : Language) : String = {
      val dir = wikiDir(language)
      val list = dir.list
      if (list == null) throw new IOException("failed to list files in ["+dir+"]")
      list.filter(_.matches("\\d{8}")).sortBy(_.toInt).lastOption.getOrElse(throw new IOException("no date directory found in ["+dir+"]"))
  }
    
}
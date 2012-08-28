package org.dbpedia.extraction.util

import java.net.URL
import scala.io.{Source, Codec}
import java.io.File
import scala.collection.mutable.ArrayBuffer

/**
 * Information about a Wikipedia.
 */
class WikiInfo(val language: Language, val pages: Int)

/**
 * Helper methods to create WikiInfo objects.
 */
object WikiInfo
{
  // hard-coded - there probably is no mirror, and the format is very specific.
  // TODO: user might want to use a local file...
  val URL = new URL("http://s23.org/wikistats/wikipedias_csv.php")
  
  // Most browsers would save the file with this name, because s23.org returns it in a http header.
  val FileName = "wikipedias.csv"
  
  def fromFile(file: File, codec: Codec): Seq[WikiInfo] = {
    val source = Source.fromFile(file)(codec)
    try fromSource(source) finally source.close
  }
  
  def fromURL(url: URL, codec: Codec): Seq[WikiInfo] = {
    val source = Source.fromURL(url)(codec)
    try fromSource(source) finally source.close
  }
  
  def fromSource(source: Source): Seq[WikiInfo] = { 
    fromLines(source.getLines)
  }
  
  /**
  * Retrieves a list of all available Wikipedias from a CSV file like http://s23.org/wikistats/wikipedias_csv.php
  * 
  */
  def fromLines(lines: Iterator[String]): Seq[WikiInfo] = {    
    val info = new ArrayBuffer[WikiInfo]
    
    if (! lines.hasNext) throw new Exception("empty file")
    lines.next // skip first line (headers)
    
    for (line <- lines) if (line.nonEmpty) info += fromLine(line)
    
    info
  }
  
  /**
   * Reads a WikiInfo object from a single CSV line.
   */
  def fromLine(line: String): WikiInfo = {
      val fields = line.split(",", -1)
      
      if (fields.length != 15) throw new Exception("expected [15] fields, found ["+fields.length+"] in line ["+line+"]")
      
      val pages = try fields(5).toInt
      catch { case nfe: NumberFormatException => throw new Exception("expected page count in field with index [5], found line ["+line+"]") }
      
      val wikiCode = fields(2)
      if (! ConfigUtils.LanguageRegex.pattern.matcher(fields(2)).matches) throw new Exception("expected language code in field with index [2], found line ["+line+"]")
      
      new WikiInfo(Language(wikiCode), pages)
  }
}

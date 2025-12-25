package org.dbpedia.extraction.util

import com.github.tototoshi.csv.CSVReader

import java.io.File
import java.net.URL
import java.util.logging.Logger
import java.io.StringReader
import java.io.StringReader

import org.dbpedia.extraction.config.ConfigUtils

import scala.collection.mutable.ArrayBuffer
import scala.io.{Codec, Source}

/**
 * Information about a Wikipedia.
 */
class WikiInfo(val wikicode: String, val pages: Int)

/**
 * Helper methods to create WikiInfo objects.
 */
object WikiInfo
{
  val logger = Logger.getLogger(WikiInfo.getClass.getName)
  // hard-coded - there probably is no mirror, and the format is very specific.
  // TODO: user might want to use a local file...
  // TODO: mayby change this to XML serialization
  val URL = new URL("http://wikistats.wmflabs.org/api.php?action=dump&table=wikipedias&format=csv")
  
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

    // Join all lines back into a single string for proper CSV parsing  
    val content = lines.mkString("\n")  
    val reader = CSVReader.open(new StringReader(content))  

    try {  
      val allRows = reader.iterator.toSeq  

      if (allRows.isEmpty) throw new Exception("empty file")  

      // Skip header row  
      for (row <- allRows.tail) {  
        if (row.nonEmpty && row.length >= 15) {  
          val pages = try row(4).toInt catch { case _: NumberFormatException => 0 }  
          val wikiCode = row(2)  

          if (ConfigUtils.LanguageRegex.pattern.matcher(wikiCode).matches) {  
            info += new WikiInfo(wikiCode, pages)  
          }  
        }  
      }  
    } finally {  
      reader.close()  
    }
    
    info
  }
  
  /**
   * Reads a WikiInfo object from a single CSV line.
   */
  def fromLine(line: String): Option[WikiInfo] = {
       val reader = CSVReader.open(new StringReader(line))  
    try {  
      val fields = reader.iterator.toSeq.headOption.getOrElse(Seq.empty)  

      if (fields.length < 15) {  
        logger.warning(s"expected [15] fields, found [${fields.length}] in line [${line.take(100)}...]")  
        return None  
      }  

      val pages = try fields(4).toInt  
      catch { case nfe: NumberFormatException => 0 }  

      val wikiCode = fields(2)  
      if (!ConfigUtils.LanguageRegex.pattern.matcher(wikiCode).matches) {  
        logger.warning(s"expected language code in field with index [2], found line [${line.take(100)}...]")  
        return None  
      }  

      Option(new WikiInfo(wikiCode, pages))  
    } finally {  
      reader.close()  
    } 

  }
}
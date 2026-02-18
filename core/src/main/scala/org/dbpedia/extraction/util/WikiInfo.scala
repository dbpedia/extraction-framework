package org.dbpedia.extraction.util

import java.io.File
import java.net.URL
import java.util.logging.Logger

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
  val URL = new URL("http://wikistats.wmflabs.org/api.php?action=dump&table=wikipedias&format=csv")

  // Most browsers would save the file with this name
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
   * Retrieves a list of all available Wikipedias from a CSV file.
   */
  def fromLines(lines: Iterator[String]): Seq[WikiInfo] = {
    val info = new ArrayBuffer[WikiInfo]

    if (!lines.hasNext) {
      logger.warning("wikipedias.csv is empty")
      return info
    }

    lines.next() // skip header

    for (line <- lines) {
      if (line.nonEmpty) {
        fromLine(line) match {
          case Some(wikiInfo) => info += wikiInfo
          case None => // skip malformed line
        }
      }
    }

    info
  }

  /**
   * Reads a WikiInfo object from a single CSV line.
   * Malformed lines are logged and skipped.
   */
  def fromLine(line: String): Option[WikiInfo] = {

    val fields = line.split(",", -1)

    // 1️⃣ Validate field count
    if (fields.length < 15) {
      logger.warning(
        s"Skipping malformed CSV line: expected 15 fields, found ${fields.length}. Line: [$line]"
      )
      return None
    }

    // 2️⃣ Parse pages safely
    val pages =
      try fields(4).toInt
      catch {
        case _: NumberFormatException =>
          logger.warning(
            s"Invalid page count in CSV line, defaulting to 0. Line: [$line]"
          )
          0
      }

    // 3️⃣ Validate language code
    val wikiCode = fields(2)
    if (!ConfigUtils.LanguageRegex.pattern.matcher(wikiCode).matches) {
      logger.warning(
        s"Invalid language code [$wikiCode] in CSV line, skipping. Line: [$line]"
      )
      return None
    }

    // 4️⃣ Valid line → create WikiInfo
    Some(new WikiInfo(wikiCode, pages))
  }
}

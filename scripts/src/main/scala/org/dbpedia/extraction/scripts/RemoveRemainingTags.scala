package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.util.ConfigUtils._
import org.dbpedia.extraction.util.DateFinder

import scala.Console._
import scala.util.matching.Regex
import org.dbpedia.extraction.util.RichFile.wrapFile
import java.io.File

/**
 * Removes HTML tags from the property values of generated triples. This might help if there are some tags left after
 * the abstract extraction.
 *
 * Example call:
 *   ../run DecodeHtmlText /data/dbpedia short-abstracts,long-abstracts -tagstripped .nt.gz 10000-
 *
 * @author Daniel Fleischhacker
 */
object RemoveRemainingTags {
  private def split(arg: String): Array[String] = {
    arg.split(",").map(_.trim).filter(_.nonEmpty)
  }

  def main(args: Array[String]): Unit = {

    require(args != null && args.length >= 5,
      "need at least six args: " +
        /*0*/ "base dir, " +
        /*1*/ "comma-separated names of input datasets (e.g. 'labels,short-abstracts,long-abstracts'), " +
        /*2*/ "output dataset name extension (e.g. '-fixed'), " +
        /*3*/ "comma-separated input/output file suffixes (e.g. '.nt.gz,.nq.bz2', '.ttl', '.ttl.bz2'), " +
        /*4*/ "languages or article count ranges (e.g. 'en,fr' or '10000-')")

    val baseDir = new File(args(0))

    val inputs = split(args(1))
    require(inputs.nonEmpty, "no input datasets")

    val extension = args(2)
    require(extension.nonEmpty, "no output name extension")

    // Suffixes of input/output files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val suffixes = split(args(3))
    require(suffixes.nonEmpty, "no input/output file suffixes")

    val languages = parseLanguages(baseDir, args.drop(4))
    require(languages.nonEmpty, "no languages")

    val patternsToRemove = List(
      """<div style=[^/]*/>""".r -> " ",
      """</div>""".r -> " "
    )

    val clean: (String => Option[String]) = cleanWithPatterns(patternsToRemove, _)

    for (language <- languages) {
      var fixCounter = 0
      val finder = new DateFinder(baseDir, language)
      // use first input file to find date. TODO: breaks if first file doesn't exist. is there a better way?
      var first = true
      for (input <- inputs; suffix <- suffixes) {
        QuadMapper.mapQuads(finder, input + suffix, input + extension + suffix, auto = first, required = false) { quad =>
          if (quad.datatype == null) throw new IllegalArgumentException("expected object literal, found object uri: "+quad)
          val cleaned = clean(quad.value)

          cleaned match {
            case Some (v) => fixCounter += 1; List(quad.copy(value = cleaned.get.trim))
            case None => List(quad)
          }
        }
        first = false
      }

      println(s"${language.wikiCode}: $fixCounter fixes to apply")
    }

  }

  /**
   * Iterates over the given list of (pattern, replacement) tuples and replaces each occurrence of pattern in value
   * by the given replacement.
   *
   * @param patternsToRemove list of tuples of regex pattern and replacement string
   * @param value value to apply replacements to
   * @return Returns an option containing the new text if changes were performed, None if no changes occurred
   */
  def cleanWithPatterns(patternsToRemove: List[(Regex, String)], value: String) : Option[String] = {
    var changed = false
    var currentValue = value
    for ((regex, replacement) <- patternsToRemove) {
      val matches = regex.pattern.matcher(currentValue)
      if (matches.find()) {
        changed = true
        currentValue = matches.replaceAll(replacement)
      }
    }

    if (changed) {
      Some(currentValue)
    }
    else {
      None
    }
  }
}

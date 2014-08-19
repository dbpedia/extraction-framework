package org.dbpedia.extraction.scripts

import java.io.{FileWriter, BufferedWriter, File}

import org.dbpedia.extraction.util.ConfigUtils._
import org.dbpedia.extraction.util.{SimpleWorkers, Language}
import org.dbpedia.extraction.util.RichFile.wrapFile

import scala.Console._
import scala.collection.mutable.HashMap

/**
 * Generates list of existing abstracts as a TSV file.
 *
 * @author Daniel Fleischhacker (daniel@informatik.uni-mannheim.de)
 */
object GenerateListOfExistingAbstracts {
  def main(args: Array[String]) {
    require(args != null && args.length >= 3,
      "need at least three args: " +
        /*0*/ "base dir, " +
        /*1*/ "input suffix (e.g., .nt.gz), " +
        /*2*/ "output file name (e.g., existing-abtracts.tsv), " +
        /*3*/ "languages or article count ranges (e.g. 'en,fr' or '10000-')")

    val baseDir = new File(args(0))

    val suffix = args(1)

    val outputFile = new File(args(2))

    val languages = parseLanguages(baseDir, args.drop(3))
    require(languages.nonEmpty, "no languages")

    val writer = new BufferedWriter(new FileWriter(outputFile))
    writer.write(s"language\tsubject\n")

    for (language <- languages) {
      val finder = new DateFinder[File](baseDir, language)
      QuadReader.readQuads(finder, "long-abstracts" + suffix, auto=true) { quad =>
        writer.write(s"${language.wikiCode}\t${quad.subject}\n")
      }
    }

    writer.close()
  }
}

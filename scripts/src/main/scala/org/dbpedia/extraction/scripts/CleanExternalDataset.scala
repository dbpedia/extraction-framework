package org.dbpedia.extraction.scripts

import java.io.File

import org.apache.commons.lang3.StringEscapeUtils
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.{Language, SimpleWorkers, Workers}
import org.dbpedia.iri.UriUtils

import scala.Console._

/**
  * Created by Chile on 10/4/2016.
  * This script should be applied to external datasets (datasets which do not originate from an extraction process of this framework).
  * It will transform uris to iris for dbpedia resources and UTF-8 encode any literal to be consistent with all DBpedia datasets.
  */
object CleanExternalDataset {

  def main(args: Array[String]): Unit = {

    require(args != null && args.length >= 3,
      "need at least three args: " +
        /*0*/ "base directory, " +
        /*1*/ "input file suffix, " +
        /*2*/ "output file suffix, " +
        /*3*/ "comma- or space-separated names of input files (e.g. 'bbcwildlife,bookmashup')"
    )

    val baseDir = new File(args(0))
    require(baseDir.canRead, "specify valid baseDir")

    // Suffix of input/output files, for example ".nt.gz"
    val inSuffix = args(1).trim
    require(inSuffix.nonEmpty, "no input file suffix")

    val outSuffix = args(2).trim
    require(outSuffix.nonEmpty, "no output file suffix")

    // Use all remaining args as comma or whitespace separated paths
    val inputs = args.drop(3).flatMap(_.split("[,\\s]")).map(f => new File(baseDir, f.trim + inSuffix)).toList
    require(inputs.nonEmpty, "no input file names")
    require(inputs.forall(_.canRead), "Make sure that every input file exists and is readable. Provide only file names without path and suffix (extension).")

    Workers.work(SimpleWorkers(1.5, 1.0) { input:File =>
      var changeCount = 0
      val outFile = new File(baseDir, input.name.substring(0, input.name.length - inSuffix.length) + outSuffix)
      new QuadMapper().mapQuads(Language.Core, input, outFile) { quad =>
        try {
          var changed = false
          val subj = UriUtils.uriToDbpediaIri(quad.subject).toString
          changed = changed || subj != quad.subject
          val pred = UriUtils.uriToDbpediaIri(quad.predicate).toString
          changed = changed || pred != quad.predicate
          val obj = if (quad.datatype == null)
            UriUtils.uriToDbpediaIri(quad.value).toString
          else if (quad.language != null || quad.datatype == "http://www.w3.org/2001/XMLSchema#string")
            StringEscapeUtils.unescapeJava(quad.value) //revert numeric and %- escape sequences
          else
            quad.value
          changed = changed || obj != quad.value
          val cont = if (quad.context != null)
            UriUtils.uriToDbpediaIri(quad.context).toString
          else
            quad.context
          changed = changed || cont != quad.context
          if (changed)
            changeCount += 1
          List(quad.copy(subject = subj, predicate = pred, value = obj, context = cont))
        }
        catch{
          case e: Throwable => err.println(input.name + ": error at: " + quad.toString())
            List()
        }
      }
      err.println(input.name + ": cleaned "+changeCount+" quads")
    }, inputs)
  }
}

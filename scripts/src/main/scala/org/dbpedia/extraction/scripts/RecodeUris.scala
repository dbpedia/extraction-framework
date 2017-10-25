package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.util.{Language, SimpleWorkers, Workers}
import org.dbpedia.extraction.util.RichFile.wrapFile
import java.io.File

import org.dbpedia.iri.UriUtils

import scala.Console.err
import scala.util.{Failure, Success}

/**
 * Decodes DBpedia URIs that percent-encode too many characters and encodes them following our
 * new rules.
 *  
 * Example call:
 * ../run RecodeUris /data/dbpedia/links .nt.gz _fixed.nt.gz bbcwildlife.nt.gz,bookmashup.nt.gz 
 */
object RecodeUris {
  
  def main(args: Array[String]): Unit = {
    
    require(args != null && args.length >= 3, 
      "need at least three args: "+
      /*0*/ "base directory, "+
      /*1*/ "input file suffix, "+
      /*2*/ "output file suffix, "+
      /*3*/ "recode to IRIs (default = false)" +
      /*4*/ "comma- or space-separated names of input files (e.g. 'bbcwildlife,bookmashup')"
    )

    val baseDir = new File(args(0))
    require(baseDir.canRead, "specify valid baseDir")

    // Suffix of input/output files, for example ".nt.gz"
    val inSuffix = args(1).trim
    require(inSuffix.nonEmpty, "no input file suffix")
    
    val outSuffix = args(2).trim
    require(outSuffix.nonEmpty, "no output file suffix")

    val recodeToIris = try{ args(3).trim.toBoolean } catch{ case _: Throwable => false}
    
    // Use all remaining args as comma or whitespace separated paths
    val inputs = args.drop(4).flatMap(_.split("[,\\s]")).map(f => new File(baseDir, f.trim + inSuffix)).toList
    require(inputs.nonEmpty, "no input file names")
    require(inputs.forall(_.canRead), "Make sure that every input file exists and is readable. Provide only file names without path and suffix (extension).")

    Workers.work(SimpleWorkers(1.5, 1.0) { input:File =>
      var changeCount = 0
      val outFile = new File(input.getParent, input.name.substring(0, input.name.length - inSuffix.length) + outSuffix)
      new QuadMapper().mapQuads(Language.Core, input, outFile) { quad =>
        try {
          var changed = false
          val subj = fixUri(quad.subject)
          changed = changed || subj != quad.subject
          val pred = fixUri(quad.predicate)
          changed = changed || pred != quad.predicate
          val obj = if (quad.datatype == null) fixUri(quad.value) else quad.value
          changed = changed || obj != quad.value
          val cont = if (quad.context != null) fixUri(quad.context) else quad.context
          changed = changed || cont != quad.context
          if (changed)
            changeCount += 1
          List(quad.copy(subject = subj, predicate = pred, value = obj, context = cont))
        }
        catch{
          //TODO remove this catch, this is covered by the ExtarctionRecorder
          case e : Throwable => {
            err.println(input.name + inSuffix + ": a quad has produced an error: " + e.getMessage)
            List()
          }
        }
      }
      err.println(input.name + inSuffix + ": changed "+changeCount+" quads")
    }, inputs)

    def fixUri(uri: String): String =
      if(recodeToIris)
        UriUtils.uriToDbpediaIri(uri).toString
      else
        UriUtils.createURI(uri) match{
          case Success(s) => s.toASCIIString
          case Failure(f) => null
        }
  }
}

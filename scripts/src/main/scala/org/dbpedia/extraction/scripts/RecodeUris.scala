package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.util.UriUtils
import org.dbpedia.extraction.util.RichFile.wrapFile
import java.io.File
import scala.Console.err

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
      /*0*/ "input file suffix, "+
      /*1*/ "output file suffix, "+
      /*2*/ "comma- or space-separated names of input files (e.g. 'bbcwildlife,bookmashup')" +
      /*3*/ "recode to IRIs (default = false)"
    )
    
    // Suffix of input/output files, for example ".nt.gz"
    
    val inSuffix = args(0)
    require(inSuffix.nonEmpty, "no input file suffix")
    
    val outSuffix = args(1)
    require(outSuffix.nonEmpty, "no output file suffix")
    
    // Use all remaining args as comma or whitespace separated paths
    val inputs = args(2).split(",").map(_.trim).filter(_.nonEmpty)
    require(inputs.nonEmpty, "no input file names")
    require(inputs.forall(_.endsWith(inSuffix)), "input file names must end with input file suffix")

    val recodeToIris = try{ args.last.trim.toBoolean } catch{ case _ => false}
    
    for (input <- inputs) {
      var changeCount = 0
      val inFile = new File(input)
      val outFile = new File(input.substring(0, input.length - inSuffix.length) + outSuffix)
      QuadMapper.mapQuads(input, inFile, outFile, required = true) { quad =>
        var changed = false
        val subj = fixUri(quad.subject)
        changed = changed || subj != quad.subject
        val pred = fixUri(quad.predicate)
        changed = changed || pred != quad.predicate
        val obj = if (quad.datatype == null) fixUri(quad.value) else quad.value
        changed = changed || obj != quad.value
        val cont = if(quad.context != null) fixUri(quad.context) else quad.context
        changed = changed || cont != quad.context
        if (changed)
          changeCount += 1
        List(quad.copy(subject = subj, predicate = pred, value = obj, context = cont))
      }
      err.println(input+": changed "+changeCount+" quads")
    }

    def fixUri(uri: String): String =
      if(recodeToIris)
        UriUtils.uriToIri(uri)
      else
        UriUtils.createUri(uri).toASCIIString
    
  }
}

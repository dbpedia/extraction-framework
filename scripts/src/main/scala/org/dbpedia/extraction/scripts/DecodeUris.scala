package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import org.dbpedia.extraction.util.TurtleUtils.escapeTurtle
import org.dbpedia.extraction.util.WikiUtil
import java.io.File
import java.lang.StringBuilder
import java.net.URLDecoder

/**
 * Example call:
 * ../run DecodeUris /data/dbpedia/links bbcwildlife,bookmashup _fixed _links.nt.gz false false
 */
object DecodeUris {
  
  private def split(arg: String): Array[String] = { 
    arg.split(",").map(_.trim).filter(_.nonEmpty)
  }
  
  def main(args: Array[String]): Unit = {
    
    require(args != null && args.length >= 6, 
      "need at least six args: "+
      /*0*/ "directory, "+
      /*1*/ "comma-separated names of input files (e.g. 'bbcwildlife,bookmashup'), "+
      /*2*/ "output dataset name extension (e.g. '_fixed'), "+
      /*3*/ "file extension (e.g. '_links.nt.gz'), "+
      /*4*/ "boolean encoding flag: true for Turtle, false for N-Triples, "+
      /*4*/ "boolean encoding flag: true for IRIs, false for URIs"
    )
    
    val dir = new File(args(0))
    
    val inputs = split(args(1))
    require(inputs.nonEmpty, "no input file names")
    
    val extension = args(2)
    require(extension.nonEmpty, "no output name extension")
    
    // Suffix of input/output files, for example "_links.nt.gz"
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val suffix = args(3)
    require(suffix.nonEmpty, "no input/output file suffixes")
    
    // turtle encoding?
    val turtle = args(4).toBoolean
    
    // do the files use IRIs or URIs?
    val iris = args(5).toBoolean
    
    for (input <- inputs) {
      val inFile = new File(dir, input + suffix)
      val outFile = new File(dir, input + extension + suffix)
      QuadMapper.mapQuads(input, inFile, outFile, required = true) { quad =>
        val subj = fixUri(quad.subject)
        val pred = fixUri(quad.predicate)
        if (quad.datatype == null) {
          val obj = fixUri(quad.value)
          List(quad.copy(subject = subj, predicate = pred, value = obj))
        }
        else {
          List(quad.copy(subject = subj, predicate = pred))
        }
      }
    }
    
  }
  
  def fixUri(uri: String): String = WikiUtil.wikiEncode(URLDecoder.decode(uri, "UTF-8"))
  
}

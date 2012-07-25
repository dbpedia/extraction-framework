package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.util.WikiUtil.{wikiEncode,cleanSpace}
import java.io.File
import java.net.URI
import org.dbpedia.util.text.uri.UriDecoder
import scala.Console.err

/**
 * Decodes DBpedia URIs that percent-encode too many characters and encodes them following our
 * new rules.
 *  
 * Example call:
 * ../run RecodeUris /data/dbpedia/links bbcwildlife,bookmashup _fixed _links.nt.gz
 */
object RecodeUris {
  
  private def split(arg: String): Array[String] = { 
    arg.split(",").map(_.trim).filter(_.nonEmpty)
  }
  
  def main(args: Array[String]): Unit = {
    
    require(args != null && args.length == 4, 
      "need four args: "+
      /*0*/ "directory, "+
      /*1*/ "comma-separated names of input files (e.g. 'bbcwildlife,bookmashup'), "+
      /*2*/ "output dataset name extension (e.g. '_fixed'), "+
      /*3*/ "file extension (e.g. '_links.nt.gz')"
    )
    
    val dir = new File(args(0))
    
    val inputs = split(args(1))
    require(inputs.nonEmpty, "no input file names")
    
    val extension = args(2)
    require(extension.nonEmpty, "no output name extension")
    
    // Suffix of input/output files, for example "_links.nt.gz"
    // This script works with .nt or .nq files using URIs, NOT with .ttl or .tql files and NOT with IRIs.
    val suffix = args(3)
    require(suffix.nonEmpty, "no input/output file suffix")
    
    for (input <- inputs) {
      var changeCount = 0
      val inFile = new File(dir, input + suffix)
      val outFile = new File(dir, input + extension + suffix)
      QuadMapper.mapQuads(input, inFile, outFile, required = true) { quad =>
        var changed = false
        val subj = fixUri(quad.subject)
        changed = changed || subj != quad.subject
        val pred = fixUri(quad.predicate)
        changed = changed || pred != quad.predicate
        if (quad.datatype == null) {
          val obj = fixUri(quad.value)
          changed = changed || obj != quad.value
          if (changed) changeCount += 1
          List(quad.copy(subject = subj, predicate = pred, value = obj))
        }
        else {
          if (changed) changeCount += 1
          List(quad.copy(subject = subj, predicate = pred))
        }
      }
      err.println(input+": changed "+changeCount+" quads")
    }
    
  }
  
  def fixUri(uri: String): String = {
    
    if (uri.startsWith("http://dbpedia.org/")) {
      
      var input = uri
      
      // Here's the list of characters that we re-encode (see WikiUtil.iriReplacements):
      // "#%<>?[\]^`{|}
      
      // we re-encode backslashes and we currently can't decode Turtle, so we disallow it
      if (uri.contains("\\")) throw new IllegalArgumentException("URI contains backslash: ["+uri+"]")
      
      // we can't handle queries, we re-encode question marks
      if (uri.contains("?")) throw new IllegalArgumentException("URI contains query: ["+uri+"]")
      
      // we can't handle fragments, we re-encode hash signs
      if (uri.contains("#")) {
        err.println("URI contains fragment: ["+uri+"]")
        input = uri.substring(0, uri.indexOf('#'))
      }
      
      // The other characters that we re-encode are extremely unlikely to occur:
      // "<>[]^`{|}
      
      // decoding the whole URI is ugly, but should work for us.
      var decoded = UriDecoder.decode(input)
      
      decoded = cleanSpace(decoded)
      decoded = decoded.replace('\n', ' ')
      decoded = decoded.replace('\t', ' ')
      
      // re-encode URI according to our own rules
      val encoded = wikiEncode(decoded)
      // we may have decoded non-ASCII characters, so we have to re-encode them
      new URI(encoded).toASCIIString
    }
    else {
      // just copy non-DBpedia URIs
      uri
    }
  }

}

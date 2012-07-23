package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import org.dbpedia.extraction.util.TurtleUtils.escapeTurtle
import scala.collection.mutable.{Set,HashMap,MultiMap,ArrayBuffer}
import java.io.File
import org.dbpedia.util.text.html.{HtmlCoder,XmlCodes}
import org.dbpedia.util.text.{ParseExceptionCounter,Appender}
import java.lang.StringBuilder

/**
 */
object DecodeHtmlCharacterReferences {
  
  private def split(arg: String): Array[String] = { 
    arg.split(",").map(_.trim).filter(_.nonEmpty)
  }
  
  def main(args: Array[String]): Unit = {
    
    require(args != null && args.length >= 6, 
      "need at least six args: "+
      /*0*/ "base dir, "+
      /*1*/ "comma-separated names of input datasets (e.g. 'labels,short-abstracts,long-abstracts'), "+
      /*2*/ "output dataset name extension (e.g. '-fixed'), "+
      /*3*/ "comma-separated input/output file suffixes (e.g. '.nt.gz,.nq.bz2', '.ttl', '.ttl.bz2'), " +
      /*4*/ "boolean encoding flag: true for Turtle, false for N-Triples, "+ 
      /*5*/ "languages or article count ranges (e.g. 'en,fr' or '10000-')")
    
    val baseDir = new File(args(0))
    
    val inputs = split(args(1))
    require(inputs.nonEmpty, "no input datasets")
    
    val extension = args(2)
    require(extension.nonEmpty, "no result name extension")
    
    // Suffixes of input/output files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on.
    // This script works with .nt, .ttl, .nq or .tql files, using IRIs or URIs.
    val fileSuffixes = split(args(3))
    require(fileSuffixes.nonEmpty, "no input/output file suffixes")
    
    // turtle encoding?
    val turtle = args(4).toBoolean
    
    val languages = parseLanguages(baseDir, args.drop(5))
    require(languages.nonEmpty, "no languages")
    
    // the decoded HTML references may not be allowed in Turtle or N-Triples, so we have to escape them
    val appender = new Appender {
      override def append(sb: StringBuilder, str: String): Unit = escapeTurtle(sb, str, turtle)
      override def append(sb: StringBuilder, code: Int): Unit = escapeTurtle(sb, code, turtle)
    }
    
    val counter = new ParseExceptionCounter
    val coder = new HtmlCoder(XmlCodes.NONE)
    coder.setErrorHandler(counter)
    coder.setAppender(appender)
    
    for (language <- languages) {
      
      // use first input file to find date. TODO: rather brittle. is there a better way?
      var finder = new DateFinder(baseDir, language, fileSuffixes(0))
      finder.find(inputs(0), auto = true)
      val date = finder.date
      
      for (fileSuffix <- fileSuffixes) {
        new DateFinder(baseDir, language, fileSuffix, date)
        // use first input file to find date. TODO: rather brittle. is there a better way?
        finder.find(inputs(0), auto = true)
        
        val mapper = new QuadMapper(finder)
        for (input <- inputs) {
          val file = finder.find(input)
          mapper.mapQuads(input, input + extension, required = false) { quad =>
            if (quad.datatype == null) throw new IllegalArgumentException("expected object literal, found object uri: "+quad)
            val decoded = coder.code(quad.value)
            List(quad.copy(value = decoded))
          }
          println(language.wikiCode+": "+file+" : found "+counter.errors()+" HTML character reference errors")
          counter.reset()
        }
      }
      
    }
    
  }
  
}

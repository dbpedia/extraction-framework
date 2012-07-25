package org.dbpedia.extraction.scripts

import java.io.File
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.StringUtils.prettyMillis
import IOUtils._
import java.lang.StringBuilder
import org.dbpedia.util.text.html.{HtmlCoder,XmlCodes}
import org.dbpedia.util.text.{ParseExceptionCounter}
import java.nio.charset.Charset


/**
 * Encodes non-ASCII chars in N-Triples files.
 *  
 * Example call:
 * ../run DecodeHtmlEntities /data/dbpedia/links gutenberg _fixed _links.nt.gz
 */
object DecodeHtmlEntities {
  
  private def split(arg: String): Array[String] = { 
    arg.split(",").map(_.trim).filter(_.nonEmpty)
  }
  
  def main(args: Array[String]): Unit = {
    
    require(args != null && args.length == 5, 
      "need five args: "+
      /*0*/ "directory, "+
      /*1*/ "comma-separated names of input files (e.g. 'gutenberg'), "+
      /*2*/ "output dataset name extension (e.g. '_fixed'), "+
      /*3*/ "file extension (e.g. '_links.nt.gz'), "+
      /*4*/ "output file encoding"
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
    
    val charset = Charset.forName(args(4))
    
    val counter = new ParseExceptionCounter
    val coder = new HtmlCoder(XmlCodes.NONE)
    coder.setErrorHandler(counter)
    
    for (input <- inputs) {
      val inFile = new File(dir, input + suffix)
      val outFile = new File(dir, input + extension + suffix)
      printerrln("reading "+inFile+" ...")
      printerrln("writing "+outFile+" ...")
      var lineCount = 0
      var changeCount = 0
      val start = System.nanoTime
      val writer = write(outFile, charset)
      try {
        readLines(inFile) { line =>
          val decoded = coder.code(line)
          writer.write(decoded)
          writer.write('\n')
          if (! line.eq(decoded)) changeCount += 1
          lineCount += 1
          if (lineCount % 1000000 == 0) log(lineCount, changeCount, start)
        }
      }
      finally writer.close()
      log(lineCount, changeCount, start)
      printerrln("found "+counter.errors()+" HTML character reference errors")
      counter.reset()
    }
    
  }

  private def log(lines: Int, changed: Int, start: Long): Unit = {
    val micros = (System.nanoTime - start) / 1000
    printerrln("read "+lines+" lines, changed "+changed+" lines in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / lines)+" micros per line)")
  }
  
}

package org.dbpedia.extraction.scripts

import java.io.File
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.util.StringUtils.prettyMillis
import org.dbpedia.extraction.util.NumberUtils
import IOUtils._
import java.lang.StringBuilder


/**
 * Encodes non-ASCII chars in N-Triples files.
 * DOES NOT ESCAPE DOUBLE QUOTES (") AND BACKSLASHES (\) - we assume that the file is mostly
 * in correct N-Triples format and just contains a few non-ASCII chars.
 *  
 * Example call:
 * ../run FixNTriplesEncoding /data/dbpedia/links bbcwildlife,italian-public-schools _fixed _links.nt.gz
 */
object FixNTriplesEncoding {
  
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
      val inFile = new File(dir, input + suffix)
      val outFile = new File(dir, input + extension + suffix)
      printerrln("reading "+inFile+" ...")
      printerrln("writing "+outFile+" ...")
      var lineCount = 0
      var changeCount = 0
      val start = System.nanoTime
      val writer = write(outFile)
      try {
        readLines(inFile) { line =>
          val escaped = new TurtleEscaper().escapeTurtle(line)
          writer.write(escaped)
          writer.write('\n')
          if (! line.eq(escaped)) changeCount += 1
          lineCount += 1
          if (lineCount % 1000000 == 0) log(lineCount, changeCount, start)
        }
      }
      finally writer.close()
      log(lineCount, changeCount, start)
    }
    
  }

  private def log(lines: Int, changed: Int, start: Long): Unit = {
    val micros = (System.nanoTime - start) / 1000
    printerrln("read "+lines+" lines, changed "+changed+" lines in "+prettyMillis(micros / 1000)+" ("+(micros.toFloat / lines)+" micros per line)")
  }
  
}

/**
 * Escapes a Unicode string according to Turtle / N-Triples format. 
 * DOES NOT ESCAPE DOUBLE QUOTES (") AND BACKSLASHES (\) - we assume that the file is mostly
 * in correct N-Triples format and just contains a few non-ASCII chars.
 * @param builder may be null
 * @param input may be null
 * @param turtle if true, non-ASCII characters are not escaped (allowed by Turtle); 
 * if false, non-ASCII characters are escaped (required by N-Triples / N-Quads).
 */
class TurtleEscaper {
  
  private var builder: StringBuilder = null
  
  private var input: String = null
  
  private var last = 0
  
  /**
   * Escapes a Unicode string according to Turtle / N-Triples format.
   * @param input must not be null
   */
  def escapeTurtle(str: String): String = {
    input = str
    last = 0
    var index = 0
    while (index < input.length)
    {
      val code = input.codePointAt(index)
      val replaced = escapeTurtle(index, code)
      index += Character.charCount(code)
      if (replaced) last = index
    }
    if (builder == null) input else builder.append(input, last, index).toString
  }
  
  /**
   * Escapes a Unicode code point according to Turtle / N-Triples format.
   * @param code Unicode code point
   */
  private def escapeTurtle(index: Int, code: Int): Boolean = {
    // TODO: use a lookup table for c <= 0xA0? c <= 0xFF?
         if (code == '\n') append(index, "\\n")
    else if (code == '\r') append(index, "\\r")
    else if (code == '\t') append(index, "\\t")
    else if (code >= 0x0020 && code < 0x007F) false
    else if (code <= 0xFFFF) appendHex(index, 'u', code, 4)
    else appendHex(index, 'U', code, 8)
  }

  private def appendHex(index: Int, esc: Char, code: Int, digits: Int): Boolean = {
    append(index)
    builder append '\\' append esc
    NumberUtils.intToHex(builder, code, digits)
    true
  }
  
  private def append(index: Int, str: String): Boolean = {
    append(index)
    builder.append(str)
    true
  }
  
  private def append(index: Int): Unit = {
    if (builder == null) builder = new StringBuilder
    builder.append(input, last, index)
  }
  
}

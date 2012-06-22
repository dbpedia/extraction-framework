package org.dbpedia.extraction.scripts

import org.apache.commons.compress.compressors.bzip2.{BZip2CompressorInputStream,BZip2CompressorOutputStream}
import java.util.zip.{GZIPInputStream,GZIPOutputStream}
import scala.io.{Source,Codec}
import java.io.{OutputStream,InputStream,FileOutputStream,OutputStreamWriter,File}
import java.net.URL
import org.dbpedia.extraction.util.{Language,WikiUtil}
import org.dbpedia.extraction.util.RichString.toRichString
import scala.collection.mutable.{Set,HashSet}
import java.io.FileInputStream
import org.dbpedia.extraction.util.{StringUtils,TurtleUtils}

/**
 * Create a dataset file with owl:sameAs links to Freebase.
 */
object CreateFreebaseLinks
{
  /**
   * We look for lines that contain wikipedia key entries.
   * Freebase calls these four columns source, property, destination and value. 
   */
  val WikipediaKey = """^([^\s]+)\t/type/object/key\t/wikipedia/en\t([^\s]+)$""".r
  
  val DBpediaNamespace = "http://dbpedia.org/resource/"
    
  val SameAsUri = "http://www.w3.org/2002/07/owl#sameAs"
    
  /**
   * The page http://rdf.freebase.com says that Freebase RDF IDs look like
   * http://rdf.freebase.com/ns/en.steve_martin 
   * On the other hand, the explanations on http://wiki.freebase.com/wiki/Mid and 
   * http://wiki.freebase.com/wiki/Id seem to state that mids like '/m/0p_47' are 
   * more stable than ids like 'steve_martin', and while all topics have a mid,
   * some don't have an id. So it seems best to use URIs like 
   * http://rdf.freebase.com/ns/m.0p_47
   * Freebase RDF also uses this URI syntax for resources that do not have an id.
   */
  val FreebaseNamespace = "http://rdf.freebase.com/ns/"
    
  private val zippers = Map[String, OutputStream => OutputStream] (
    "gz" -> { new GZIPOutputStream(_) }, 
    "bz2" -> { new BZip2CompressorOutputStream(_) } 
  )
  
  private val unzippers = Map[String, InputStream => InputStream] (
    "gz" -> { new GZIPInputStream(_) }, 
    "bz2" -> { new BZip2CompressorInputStream(_) } 
  )
  
  private def wrap[T](stream: T, file: File, wrappers: Map[String, T => T]): T = {
    val name = file.getName
    val suffix = name.substring(name.lastIndexOf('.') + 1)
    wrappers.get(suffix) match {
      case Some(wrapper) => wrapper(stream)
      case None => stream
    }
  }
  
  def main(args : Array[String]) {
    
    // Freebase input file, may be .gz or .bz2 zipped 
    // must have the format described on http://wiki.freebase.com/wiki/Data_dumps#Quad_dump
    // Latest: http://download.freebase.com/datadumps/latest/freebase-datadump-quadruples.tsv.bz2      
    val inFile = new File(args(0))
    
    // output file, may be .gz or .bz2 zipped 
    val outFile = new File(args(1))
    
    // do the DBpedia files use Turtle or N-Triples escaping?
    val turtle = args(2).toBoolean
    
    val start = System.currentTimeMillis
    println("Searching for Freebase links in "+inFile+"...")
    var count = 0
    val out = new FileOutputStream(outFile)
    try {
      val writer = new OutputStreamWriter(wrap(out, outFile, zippers), "UTF-8")
      
      val in = new FileInputStream(inFile)
      try {
        for (line <- Source.fromInputStream(wrap(in, inFile, unzippers), "UTF-8").getLines) {
          line match {
            case WikipediaKey(mid, title) => {
              writer.write("<"+DBpediaNamespace+recode(title, turtle)+"> <"+SameAsUri+"> <"+FreebaseNamespace+mid+"> .\n")
              count += 1
              if (count % 10000 == 0) log(count, start)
            }
            case _ => // ignore all other lines
          }
        }
      }
      finally in.close()
      writer.close()
    }
    finally out.close()
    log(count, start)
  }
  
  private def log(count: Int, start: Long): Unit = {
    val millis = System.currentTimeMillis - start
    println("found "+count+" links to Freebase in "+StringUtils.prettyMillis(millis))
  }

  /**
   * Freebase escapes most non-alphanumeric characters in keys by a dollar sign followed
   * by the UTF-16 code of the character. See http://wiki.freebase.com/wiki/MQL_key_escaping .
   */
  private val KeyEscape = """\$(\p{XDigit}{4})""".r

  /**
   * Undo Freebase key encoding, do URI escaping and Turtle / N-Triple escaping. 
   */
  private def recode(freebase: String, turtle: Boolean): String = {
    val plain = freebase.replaceLiteral(KeyEscape.pattern, m => Integer.parseInt(m.group(1), 16).toChar.toString)
    val wikiEncoded = WikiUtil.wikiEncode(plain)
    TurtleUtils.escapeTurtle(wikiEncoded, turtle)
  }

}
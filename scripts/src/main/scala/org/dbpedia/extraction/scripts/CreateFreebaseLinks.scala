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
import org.dbpedia.extraction.util.{Finder,Language,StringUtils,TurtleUtils}
import org.dbpedia.extraction.util.RichFile.toRichFile
import org.dbpedia.extraction.destinations.{Dataset,DBpediaDatasets}
import CreateFreebaseLinks._
import java.io.IOException
import java.io.FileNotFoundException

/**
 * Create a dataset file with owl:sameAs links to Freebase.
 */
object CreateFreebaseLinks
{
  /**
   * We look for lines that contain wikipedia key entries.
   * Freebase calls these four columns source, property, destination and value. 
   */
  private val WikipediaKey = """^([^\s]+)\t/type/object/key\t/wikipedia/en\t([^\s]+)$""".r
  
  private val DBpediaNamespace = "http://dbpedia.org/resource/"
    
  private val SameAsUri = "http://www.w3.org/2002/07/owl#sameAs"
    
  /**
   * Freebase escapes most non-alphanumeric characters in keys by a dollar sign followed
   * by the UTF-16 code of the character. See http://wiki.freebase.com/wiki/MQL_key_escaping .
   */
  private val KeyEscape = """\$(\p{XDigit}{4})""".r

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
  private val FreebaseNamespace = "http://rdf.freebase.com/ns/"
    
  private val zippers = Map[String, OutputStream => OutputStream] (
    "gz" -> { new GZIPOutputStream(_) }, 
    "bz2" -> { new BZip2CompressorOutputStream(_) } 
  )
  
  private val unzippers = Map[String, InputStream => InputStream] (
    "gz" -> { new GZIPInputStream(_) }, 
    "bz2" -> { new BZip2CompressorInputStream(_) } 
  )
  
  private def open[T](file: File, opener: File => T, wrappers: Map[String, T => T]): T = {
    val name = file.getName
    val suffix = name.substring(name.lastIndexOf('.') + 1)
    wrappers.getOrElse(suffix, identity[T] _)(opener(file)) 
  }
  
  def main(args : Array[String]) {
    
    // do the DBpedia files use IRIs or URIs?
    val iris = args(0).toBoolean
    
    // do the DBpedia files use Turtle or N-Triples escaping?
    val turtle = args(1).toBoolean
    
    // base dir of DBpedia files
    val dir = new File(args(2))
    
    // suffix of DBpedia files 
    val suffix = args(3)
    
    // Freebase input file, may be .gz or .bz2 zipped 
    // must have the format described on http://wiki.freebase.com/wiki/Data_dumps#Quad_dump
    // Latest: http://download.freebase.com/datadumps/latest/freebase-datadump-quadruples.tsv.bz2      
    val inFile = new File(args(4))
    
    // output file, may be .gz or .bz2 zipped 
    val outFile = new File(args(5))
    
    val finder = new Finder[File](dir, Language.English)
    val date = finder.dates("download-complete").last
    
    def find(dataset: Dataset): File = {
      val file = finder.file(date, dataset.name.replace('_', '-')+'.'+suffix)
      if (! file.exists()) throw new FileNotFoundException(file.toString())
      file
    }
  
    val labels = find(DBpediaDatasets.Labels)
    val redirects = find(DBpediaDatasets.Redirects)
    val disambig = find(DBpediaDatasets.DisambiguationLinks)
    
    new CreateFreebaseLinks(iris, turtle).findLinks(inFile, outFile)
  }
  
  
}

class CreateFreebaseLinks(iris: Boolean, turtle: Boolean) {
    
  def findLinks(inFile: File, outFile: File): Unit = {
    val start = System.nanoTime
    println("Searching for Freebase links in "+inFile+"...")
    var lines = 0
    var links = 0
    val out = open(outFile, new FileOutputStream(_), zippers)
    try {
      val writer = new OutputStreamWriter(out, "UTF-8")
      
      val in = open(inFile, new FileInputStream(_), unzippers)
      try {
        for (line <- Source.fromInputStream(in, "UTF-8").getLines) {
          lines += 1
          line match {
            case WikipediaKey(mid, title) => {
              links += 1
              writer.write("<"+DBpediaNamespace+recode(title)+"> <"+SameAsUri+"> <"+FreebaseNamespace+mid+"> .\n")
            }
            case _ => // ignore all other lines
          }
          if (lines % 1000000 == 0) log(lines, links, start)
        }
      }
      finally in.close()
      writer.close()
    }
    finally out.close()
    log(lines, links, start)
  }
  
  private def log(lines: Int, links: Int, start: Long): Unit = {
    val nanos = System.nanoTime - start
    println("processed "+lines+" lines, found "+links+" links to Freebase in "+StringUtils.prettyMillis(nanos / 1000000)+" ("+(nanos.toFloat/lines)+" nanos per line)")
  }
  
  /**
   * Undo Freebase key encoding, do URI escaping and Turtle / N-Triple escaping. 
   */
  private def recode(freebase: String): String = {
    val plain = freebase.replaceLiteral(KeyEscape.pattern, m => Integer.parseInt(m.group(1), 16).toChar.toString)
    val wikiEncoded = WikiUtil.wikiEncode(plain)
    TurtleUtils.escapeTurtle(wikiEncoded, turtle)
  }

}
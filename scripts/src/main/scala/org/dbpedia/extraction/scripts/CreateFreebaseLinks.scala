package org.dbpedia.extraction.scripts

import org.apache.commons.compress.compressors.bzip2.{BZip2CompressorInputStream,BZip2CompressorOutputStream}
import java.util.zip.{GZIPInputStream,GZIPOutputStream}
import scala.io.{Source,Codec}
import scala.collection.mutable.{Set,HashSet}
import java.io.{File,InputStream,OutputStream,FileInputStream,FileOutputStream,OutputStreamWriter,FileNotFoundException}
import java.net.{URI,URISyntaxException}
import org.dbpedia.extraction.util.{Finder,Language,StringUtils,TurtleUtils}
import org.dbpedia.extraction.util.WikiUtil._
import org.dbpedia.extraction.util.RichString.toRichString
import org.dbpedia.extraction.util.RichFile.toRichFile
import org.dbpedia.extraction.destinations.{Dataset,DBpediaDatasets}
import CreateFreebaseLinks._
import java.util.regex.Matcher
import java.lang.StringBuilder

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
  
  /**
   * Lines in relevant DBpedia N-Triples / Turtle files start with this prefix.
   */
  private val Prefix = "<http://dbpedia.org/resource/"
    
  /**
   * Middle part of N-Triples / Turtle lines.
   * The page http://rdf.freebase.com states that Freebase RDF IDs look like
   * http://rdf.freebase.com/ns/en.steve_martin 
   * On the other hand, the explanations on http://wiki.freebase.com/wiki/Mid and 
   * http://wiki.freebase.com/wiki/Id seem to say that mids like '/m/0p_47' are 
   * more stable than ids like 'steve_martin', and while all topics have a mid,
   * some don't have an id. So it seems best to use URIs like 
   * http://rdf.freebase.com/ns/m.0p_47
   * Freebase RDF also uses this URI syntax for resources that do not have an id.
   * Besides, finding the id for a DBpedia title would require more code, since the mid 
   * is on the same line as the Wikipedia title, but the id is on a different line.
   */
  private val Infix = "> <http://www.w3.org/2002/07/owl#sameAs> <http://rdf.freebase.com/ns/"
    
  /**
   * Closing part of N-Triples / Turtle lines. 
   */
  private val Suffix = "> .\n"
    
  /**
   * Freebase escapes most non-alphanumeric characters in keys by a dollar sign followed by the 
   * hexadecimal UTF-16 code of the character. See http://wiki.freebase.com/wiki/MQL_key_escaping .
   */
  private val KeyEscape = """\$(\p{XDigit}{4})""".r

  /**
   * Append character for hexadecimal UTF-16 code to given string builder.
   */
  private def unescapeKey(sb: StringBuilder, matcher: Matcher): Unit = {
    sb.append(Integer.parseInt(matcher.group(1), 16).toChar)
  }
  
  private def unescapeKey(key: String): String = {
    key.replaceBy(KeyEscape.pattern, unescapeKey)
  }
  
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
    
    // suffix of DBpedia files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on
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
      val file = finder.file(date, dataset.name.replace('_', '-')+suffix)
      if (! file.exists()) throw new FileNotFoundException(file.toString())
      file
    }
  
    val labels = find(DBpediaDatasets.Labels)
    val redirects = find(DBpediaDatasets.Redirects)
    val disambig = find(DBpediaDatasets.DisambiguationLinks)
    
    val dbpedia = new HashSet[String]()
    
    collectUris(dbpedia, labels, true)
    collectUris(dbpedia, redirects, false)
    collectUris(dbpedia, disambig, false)
    
    new CreateFreebaseLinks(iris, turtle).findLinks(dbpedia, inFile, outFile)
  }
  
  private def collectUris(set: Set[String], file: File, add: Boolean): Unit = {
    val start = System.nanoTime
    println((if (add) "Add" else "Subtract")+"ing DBpedia URIs in "+file+"...")
    var lines = 0
    val in = open(file, new FileInputStream(_), unzippers)
    try {
      for (line <- Source.fromInputStream(in, "UTF-8").getLines) {
        if (line.nonEmpty && line.charAt(0) != '#') {
          val close = line.indexOf('>', Prefix.length)
          if (! line.startsWith(Prefix) || close == -1) throw new IllegalArgumentException(line)
          val rdfKey = line.substring(Prefix.length, close)
          if (add) set += rdfKey else set -= rdfKey
          lines += 1
          if (lines % 1000000 == 0) log(lines, start)
        }
      }
    }
    finally in.close()
    log(lines, start)
  }
  
  private def log(lines: Int, start: Long): Unit = {
    val nanos = System.nanoTime - start
    println("processed "+lines+" lines in "+StringUtils.prettyMillis(nanos / 1000000)+" ("+(nanos.toFloat/lines)+" nanos per line)")
  }
  
}

class CreateFreebaseLinks(iris: Boolean, turtle: Boolean) {
    
  def findLinks(dbpedia: Set[String], inFile: File, outFile: File): Unit = {
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
          line match {
            case WikipediaKey(mid, key) => {
              
              val rdfKey = 
              try recode(key)
              catch {
                case ex => {
                  println("BAD LINE: ["+line+"]: "+ex)
                  ""
                }
              }
              
              if (! mid.startsWith("/m/")) throw new IllegalArgumentException(line)
              val rdfMid = "m."+mid.substring(3)
              if (dbpedia.contains(rdfKey)) {
                writer.write(Prefix+rdfKey+Infix+rdfMid+Suffix)
                links += 1
              }
            }
            case _ => // ignore all other lines
          }
          lines += 1
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
    println("processed "+lines+" lines, found "+links+" Freebase links in "+StringUtils.prettyMillis(nanos / 1000000)+" ("+(nanos.toFloat/lines)+" nanos per line)")
  }
  
  /**
   * To convert titles from IRI to URI format, we need a well-formed IRI. Because titles may 
   * contain colons and slashes, we have to append them to a dummy URI prefix, then convert them 
   * to IRI and in the end cut off the prefix again.
   */
  private val Dummy = "dummy:///"
    
  /**
   * Undo Freebase key encoding, do URI escaping and Turtle / N-Triple escaping. 
   */
  private def recode(key: String): String = {
    val plain = unescapeKey(key)
    var uri = wikiEncode(cleanSpace(plain))
    if (! iris) uri = new URI(Dummy+uri).toASCIIString.substring(Dummy.length)
    TurtleUtils.escapeTurtle(uri, turtle)
  }

}
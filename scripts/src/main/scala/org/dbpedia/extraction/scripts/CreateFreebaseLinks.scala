package org.dbpedia.extraction.scripts

import scala.collection.mutable.{Set,HashSet}
import java.io.{File,FileNotFoundException}
import java.net.URI
import org.dbpedia.extraction.util.{Finder,Language}
import org.dbpedia.extraction.util.TurtleUtils.escapeTurtle
import org.dbpedia.extraction.util.StringUtils.{prettyMillis,formatCurrentTimestamp}
import org.dbpedia.extraction.util.NumberUtils.hexToInt
import org.dbpedia.extraction.util.WikiUtil.{wikiEncode,cleanSpace}
import org.dbpedia.extraction.util.RichString.wrapString
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.destinations.{Dataset,DBpediaDatasets}
import CreateFreebaseLinks._
import scala.Console.err
import IOUtils.{readLines,write}
import java.util.regex.Matcher
import java.lang.StringBuilder

/**
 * Create a dataset file with owl:sameAs links to Freebase.
 * 
 * Example calls:
 * 
 * URIs and N-Triple escaping
 * ../run CreateFreebaseLinks false false /data/dbpedia .nt.gz freebase-links.nt.gz freebase-datadump-quadruples.tsv.bz2
 * 
 * IRIs and Turtle escaping
 * ../run CreateFreebaseLinks true true /data/dbpedia .ttl.gz freebase-links.ttl.gz freebase-datadump-quadruples.tsv.bz2
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
    sb.append(hexToInt(matcher.group(1)).toChar) // may be faster than Integer.parseInt
    // sb.append(Integer.parseInt(matcher.group(1), 16).toChar)
  }
  
  private def unescapeKey(key: String): String = {
    key.replaceBy(KeyEscape.pattern, unescapeKey)
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
    err.println((if (add) "Add" else "Subtract")+"ing DBpedia URIs in "+file+"...")
    var lines = 0
    readLines(file) { line =>
      if (line.nonEmpty && line.charAt(0) != '#') {
        val close = line.indexOf('>', Prefix.length)
        if (! line.startsWith(Prefix) || close == -1) throw new IllegalArgumentException(line)
        val rdfKey = line.substring(Prefix.length, close)
        if (add) set += rdfKey else set -= rdfKey
        lines += 1
        if (lines % 1000000 == 0) log(lines, start)
      }
    }
    log(lines, start)
  }
  
  private def log(lines: Int, start: Long): Unit = {
    val nanos = System.nanoTime - start
    err.println("processed "+lines+" lines in "+prettyMillis(nanos / 1000000)+" ("+(nanos.toFloat/lines)+" nanos per line)")
  }
  
}

class CreateFreebaseLinks(iris: Boolean, turtle: Boolean) {
    
  def findLinks(dbpedia: Set[String], inFile: File, outFile: File): Unit = {
    val start = System.nanoTime
    err.println("Searching for Freebase links in "+inFile+"...")
    var lines = 0
    var links = 0
    val writer = write(outFile)
    try {
      // copied from org.dbpedia.extraction.destinations.formatters.TerseFormatter.footer
      writer.write("# started "+formatCurrentTimestamp+"\n")
      readLines(inFile) { line =>
        line match {
          case WikipediaKey(mid, key) => {
            
            val rdfKey = 
            try recode(key)
            catch {
              case ex => {
                err.println("BAD LINE: ["+line+"]: "+ex)
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
      // copied from org.dbpedia.extraction.destinations.formatters.TerseFormatter.header
      writer.write("# completed "+formatCurrentTimestamp+"\n")
    }
    finally writer.close()
    log(lines, links, start)
  }
  
  private def log(lines: Int, links: Int, start: Long): Unit = {
    val nanos = System.nanoTime - start
    err.println("processed "+lines+" lines, found "+links+" Freebase links in "+prettyMillis(nanos / 1000000)+" ("+(nanos.toFloat/lines)+" nanos per line)")
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
    escapeTurtle(uri, turtle)
  }

}
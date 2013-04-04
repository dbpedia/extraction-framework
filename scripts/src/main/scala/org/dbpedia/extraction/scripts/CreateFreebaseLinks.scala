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
import collection.mutable

/**
 * Create a dataset file with owl:sameAs links to Freebase.
 * 
 * Example calls:
 * 
 * URIs and N-Triple escaping
 * ../run CreateFreebaseLinks /data/dbpedia .nt.gz freebase-rdf-<date>.gz freebase-links.nt.gz
 * 
 * IRIs and Turtle escaping
 * ../run CreateFreebaseLinks /data/dbpedia .ttl.gz freebase-rdf-<date>.gz freebase-links.ttl.gz
 *
 * See https://developers.google.com/freebase/data for a reference of the Freebase RDF data dumps
 */
object CreateFreebaseLinks
{
  /**
   * We look for lines that contain wikipedia key entries. Specifically we are retrieving English Wikipedia Page Ids
   */
  private val FreebaseWikipediaId = """^ns:([^\s]+)\tns:type\.object\.key\t"/wikipedia/en_id/([^\s]+)"\.$""".r
  private val WikipediaResId = """^<http://dbpedia\.org/resource/([^\s]+)> .* "(\d+)"\^\^<http://www\.w3\.org/2001/XMLSchema\#integer> \.$""".r
  private val DBpediaResId = """^<http://dbpedia\.org/resource/([^\s]+)> .*""".r
  
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
   * is on the same line as the Wikipedia page id, but the id is on a different line.
   */
  private val Infix = "> <http://www.w3.org/2002/07/owl#sameAs> <http://rdf.freebase.com/ns/"
    
  /**
   * Closing part of N-Triples / Turtle lines. 
   */
  private val Suffix = "> .\n"
  
  def main(args : Array[String]) {
    
    // base dir of DBpedia files
    val dir = new File(args(0))
    
    // suffix of DBpedia files, for example ".nt", ".ttl.gz", ".nt.bz2" and so on
    val suffix = args(1)
    
    // Freebase RDF input file, may be .gz or .bz2 zipped
    // must have the format described on https://developers.google.com/freebase/data
    // Latest: http://download.freebaseapps.com
    val inFile = new File(args(2))
    
    // output file, may be .gz or .bz2 zipped 
    val outFile = new File(args(3))
    
    val finder = new Finder[File](dir, Language.English, "wiki")
    val date = finder.dates("download-complete").last
    
    def find(dataset: Dataset): File = {
      val file = finder.file(date, dataset.name.replace('_', '-')+suffix)
      if (! file.exists()) throw new FileNotFoundException(file.toString())
      file
    }
  
    val pageIds = find(DBpediaDatasets.PageIds)
    val redirects = find(DBpediaDatasets.Redirects)
    val disambig = find(DBpediaDatasets.DisambiguationLinks)

    // wiki_title -> wiki_page_id
    val dbpediaMap = mutable.Map[String, String]()

    collectUris(dbpediaMap, pageIds, true)
    collectUris(dbpediaMap, redirects, false)
    collectUris(dbpediaMap, disambig, false)

    // Reverse the map now
    // wiki_page_id -> wiki_title
    var finalDbpediaMap : Map[String, String] = dbpediaMap.map(_.swap).toMap[String, String]

    findLinks(finalDbpediaMap, inFile, outFile)
  }

  private def collectUris(map: mutable.Map[String, String], file: File, add: Boolean): Unit = {
    val start = System.nanoTime
    err.println((if (add) "Add" else "Subtract")+"ing DBpedia URIs in "+file+"...")
    var lines = 0
    readLines(file) { line =>
      if (line.nonEmpty && line.charAt(0) != '#') {
        val (rdfKey, wikiPageId) = line match {
          // This will match lines in the page_ids file
          case WikipediaResId(rdfKey, wikiPageId) =>  (rdfKey, wikiPageId)
          // This will match other files, e.g. redirects and disambiguations
          case DBpediaResId(rdfKey) => (rdfKey, null)
          case _ => throw new IllegalArgumentException("Invalid format for line: " + line)
        }

        if (add) {
          if (rdfKey == null || wikiPageId == null) throw new IllegalArgumentException("There was a problem with line: " + line)
          map += (rdfKey -> wikiPageId)
        } else {
          if (rdfKey == null) throw new IllegalArgumentException("There was a problem with line: " + line)
          map -= rdfKey
        }

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
  
  private def findLinks(dbpedia: Map[String, String], inFile: File, outFile: File): Unit = {
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
          case FreebaseWikipediaId(mid, wikiId) => {
            if (! mid.startsWith("m.")) throw new IllegalArgumentException(line)
            if (dbpedia.contains(wikiId)) {
              writer.write(Prefix+dbpedia(wikiId)+Infix+mid+Suffix)
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
}

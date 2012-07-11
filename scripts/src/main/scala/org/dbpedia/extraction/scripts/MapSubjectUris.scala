package org.dbpedia.extraction.scripts

import org.dbpedia.extraction.util.{Finder,Language}
import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import org.dbpedia.extraction.util.StringUtils.{prettyMillis,formatCurrentTimestamp}
import org.dbpedia.extraction.util.RichFile.toRichFile
import org.dbpedia.extraction.util.RichReader.toRichReader
import org.dbpedia.extraction.scripts.IOUtils._
import scala.collection.mutable.{Map,Set,HashMap,MultiMap}
import java.io.{File,InputStream,OutputStream,Writer,FileInputStream,FileOutputStream,OutputStreamWriter,InputStreamReader,BufferedReader,FileNotFoundException}
import MapUris._

/**
 * Maps old URIs in triple files to new URIs:
 * - read one or more triple files that contain the URI mapping:
 *   - the predicate is ignored
 *   - only triples whose object URI has a certain domain are used
 * - read one or more files that need their subject URI changed
 *   - the predicate is ignored
 */
object MapSubjectUris {
  
  private def split(arg: String): Array[String] = { 
    arg.split(",").map(_.trim).filter(_.nonEmpty)
  }
  
  def main(args: Array[String]): Unit = {
    
    require(args != null && args.length == 7, 
      "need at least seven args: " +
      "base dir, " +
      "comma-separated names of datasets mapping old URIs to new URIs (e.g. 'interlanguage-links-same-as,interlanguage-links-see-also'), "+
      "comma-separated names of input datasets (e.g. 'labels,short-abstracts,long-abstracts'), "+
      "result dataset name extension (e.g. '-en-uris'), "+
      "triples file suffix (e.g. '.nt.gz', '.ttl', '.ttl.bz2'), " +
      "new URI domain (e.g. 'en.dbpedia.org', 'dbpedia.org'), " +
      "languages or article count ranges (e.g. 'en,fr' or '10000-')")
    
    val baseDir = new File(args(0))
    
    val maps = split(args(1))
    require(maps.nonEmpty, "no mapping datasets")
    
    val inputs = split(args(2))
    require(maps.nonEmpty, "no input datasets")
    
    val extension = args(3)
    require(extension.nonEmpty, "no result name extension")
    
    val suffix = args(4)
    require(suffix.nonEmpty, "no file suffix")
    
    val domain = "http://"+args(5)+"/"
    require(domain != "http:///", "no new domain")
    
    val languages = parseLanguages(baseDir, args.drop(6))
    require(languages.nonEmpty, "no languages")
    
    for (language <- languages) {
      val mapper = new MapUris(baseDir, language, suffix)
      for (map <- maps) mapper.readMap(map, (subj, pred, obj) => obj.startsWith(domain))
      for (input <- inputs) mapper.mapInput(input, input+extension, DISCARD_UNKNOWN, DONT_MAP)
    }
    
  }
  
}

package org.dbpedia.extraction.scripts

import java.io.File
import org.dbpedia.extraction.util.{Finder,Language,ConfigUtils,WikiInfo}
import org.dbpedia.extraction.util.ConfigUtils.latestDate
import org.dbpedia.extraction.util.RichFile.toRichFile
import scala.collection.immutable.SortedSet
import scala.collection.mutable.HashSet
import scala.io.Codec

object InterLanguageLinks {
  
  def main(args: Array[String]) {
    
    val baseDir = new File(args(0))
    
    // TODO: next 30 lines copy & paste in org.dbpedia.extraction.dump.sql.Import, org.dbpedia.extraction.dump.download.Download, org.dbpedia.extraction.dump.extract.Config
    
    // Use all remaining args as keys or comma or whitespace separated lists of keys
    var keys = for(arg <- args.drop(1); key <- arg.split("[,\\s]"); if (key.nonEmpty)) yield key
        
    var languages = SortedSet[Language]()(Language.wikiCodeOrdering)
    
    val ranges = new HashSet[(Int,Int)]
  
    for (key <- keys) key match {
      case ConfigUtils.Range(from, to) => ranges += ConfigUtils.toRange(from, to)
      case ConfigUtils.Language(language) => languages += Language(language)
      case other => throw new Exception("Invalid language / range '"+other+"'")
    }
    
    // resolve page count ranges to languages
    if (ranges.nonEmpty)
    {
      val listFile = new File(baseDir, WikiInfo.FileName)
      
      // Note: the file is in ASCII, any non-ASCII chars are XML-encoded like '&#231;'. 
      // There is no Codec.ASCII, but UTF-8 also works for ASCII. Luckily we don't use 
      // these non-ASCII chars anyway, so we don't have to unescape them.
      println("parsing "+listFile)
      val wikis = WikiInfo.fromFile(listFile, Codec.UTF8)
      
      // for all wikis in one of the desired ranges...
      for ((from, to) <- ranges; wiki <- wikis; if (from <= wiki.pages && wiki.pages <= to))
      {
        // ...add its language
        languages += Language(wiki.language)
      }
    }
    
    for (language <- languages) {
      val finder = new Finder[File](baseDir, language)
      val date = latestDate(finder, "interlanguage-links.ttl.gz")
      println(date)
    }
  }
}
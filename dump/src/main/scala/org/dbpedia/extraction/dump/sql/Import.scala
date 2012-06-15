package org.dbpedia.extraction.dump.sql

import java.io.File
import org.dbpedia.extraction.sources.XMLSource
import org.dbpedia.extraction.util.{Finder,Language}
import org.dbpedia.extraction.util.RichFile.toRichFile
import org.dbpedia.extraction.wikiparser.Namespace
import org.dbpedia.extraction.dump.util.WikiInfo
import scala.io.Codec
import scala.collection.mutable.{Set,HashSet,Map,HashMap}
import scala.collection.immutable.SortedSet
import org.dbpedia.extraction.dump.util.ConfigUtils
import org.dbpedia.extraction.dump.download.Download
import java.util.Properties
import scala.io.Source

object Import {
  
  def main(args: Array[String]) : Unit = {
    
    val baseDir = new File(args(0))
    val tablesFile = new File(args(1))
    val server = args(2)
    val requireComplete = args(3).toBoolean
    
    // Use all remaining args as keys or comma or whitespace separated lists of keys
    var keys: Seq[String] = for(arg <- args.drop(4); lang <- arg.split("[,\\s]"); if (lang.nonEmpty)) yield lang
        
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
    
    val source = Source.fromFile(tablesFile)(Codec.UTF8)
    val tables =
    try source.getLines.mkString("\n")
    finally source.close()
    
    val namespaces = Set(Namespace.Template)
    val namespaceList = namespaces.map(_.name).mkString("(",",",")")
    
    val info = new Properties()
    info.setProperty("allowMultiQueries", "true")
    info.setProperty("characterEncoding", "UTF-8")
    val conn = new com.mysql.jdbc.Driver().connect("jdbc:mysql://"+server+"/", info)
    try {
      for (language <- languages) {
        
        val finder = new Finder[File](baseDir, language)
        val tagFile = if (requireComplete) Download.Complete else "pages-articles.xml"
        val date = ConfigUtils.latestDate(finder, tagFile)
        val file = finder.file(date, "pages-articles.xml")
        
        val database = finder.wikiName
        
        println("importing pages in namespaces "+namespaceList+" from "+file+" to database "+database+" on server "+server)
        
        val source = XMLSource.fromFile(file, language, title => namespaces.contains(title.namespace))
        
        val stmt = conn.createStatement()
        try {
          stmt.execute("DROP DATABASE IF EXISTS "+database+"; CREATE DATABASE "+database+" CHARACTER SET binary; USE "+database+";")
          stmt.execute(tables)
        }
        finally stmt.close()
        
        new Importer(conn).process(source)
        
        println("imported  pages in namespaces "+namespaceList+" from "+file+" to database "+database+" on server "+server)
      }
    }
    finally conn.close()
    
  }
  
  private def add[K](map: Map[K,Set[String]], key: K, values: Array[String]) = {
    map.getOrElseUpdate(key, new HashSet[String]) ++= values
  }
  
}


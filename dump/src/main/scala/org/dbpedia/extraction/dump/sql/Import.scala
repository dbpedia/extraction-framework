package org.dbpedia.extraction.dump.sql

import java.io.File
import org.dbpedia.extraction.sources.XMLSource
import org.dbpedia.extraction.util.{Finder,Language,WikiInfo}
import org.dbpedia.extraction.util.ConfigUtils.parseLanguages
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.wikiparser.Namespace
import scala.io.Codec
import scala.collection.mutable.{Set,HashSet,Map,HashMap}
import scala.collection.immutable.SortedSet
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
    var languages = parseLanguages(baseDir, args.drop(4))
    
    val source = Source.fromFile(tablesFile)(Codec.UTF8)
    val tables =
    try source.getLines.mkString("\n")
    finally source.close()
    
    val namespaces = Set(Namespace.Template)
    val namespaceList = namespaces.map(_.name).mkString("[",",","]")
    
    val info = new Properties()
    info.setProperty("allowMultiQueries", "true")
    info.setProperty("characterEncoding", "UTF-8")
    val conn = new com.mysql.jdbc.Driver().connect("jdbc:mysql://"+server+"/", info)
    try {
      for (language <- languages) {
        
        val finder = new Finder[File](baseDir, language)
        val tagFile = if (requireComplete) Download.Complete else "pages-articles.xml"
        val date = finder.dates(tagFile).last
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
  
}


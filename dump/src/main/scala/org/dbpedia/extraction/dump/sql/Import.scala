package org.dbpedia.extraction.dump.sql

import java.io._
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
import scala.io.Codec.UTF8
import org.dbpedia.extraction.util.IOUtils

object Import {
  
  def main(args: Array[String]) : Unit = {
    
    val baseDir = new File(args(0))
    val tablesFile = new File(args(1))
    val url = args(2)
    val requireComplete = args(3).toBoolean
    val fileName = args(4)
    
    // Use all remaining args as keys or comma or whitespace separated lists of keys
    var languages = parseLanguages(baseDir, args.drop(5))
    
    val source = Source.fromFile(tablesFile)(Codec.UTF8)
    val tables =
    try source.getLines.mkString("\n")
    finally source.close()
    
    val namespaces = Set(Namespace.Template)
    val namespaceList = namespaces.map(_.name).mkString("[",",","]")
    
    val info = new Properties()
    info.setProperty("allowMultiQueries", "true")
    val conn = new com.mysql.jdbc.Driver().connect(url, info)
    try {
      for (language <- languages) {
        
        val finder = new Finder[File](baseDir, language, "wiki")
        val tagFile = if (requireComplete) Download.Complete else fileName
        val date = finder.dates(tagFile).last
        val file = finder.file(date, fileName)
        
        val database = finder.wikiName
        
        println("importing pages in namespaces "+namespaceList+" from "+file+" to database "+database+" on server URL "+url)
        
        /*
        Ignore the page if a different namespace is also given for the title.
        http://gd.wikipedia.org/?curid=4184 and http://gd.wikipedia.org/?curid=4185&redirect=no
        have the same title "Teamplaid:Gàidhlig", but they are in different namespaces: 4184 is
        in the template namespace (good), 4185 is in the main namespace (bad). It looks like
        MediaWiki can handle this somehow, but when we try to import both pages from the XML dump
        into the database, MySQL rightly complains about the duplicate title. As a workaround,
        we simply reject pages for which the <ns> namespace doesn't fit the <title> namespace. 
        */
        val source = XMLSource.fromReader(() => IOUtils.reader(file), language, title => title.otherNamespace == null && namespaces.contains(title.namespace))
        
        val stmt = conn.createStatement()
        try {
          stmt.execute("DROP DATABASE IF EXISTS "+database+"; CREATE DATABASE "+database+" CHARACTER SET binary; USE "+database+";")
          stmt.execute(tables)
        }
        finally stmt.close()
        
        new Importer(conn).process(source)
        
        println("imported  pages in namespaces "+namespaceList+" from "+file+" to database "+database+" on server URL "+url)
      }
    }
    finally conn.close()
    
  }
  
}


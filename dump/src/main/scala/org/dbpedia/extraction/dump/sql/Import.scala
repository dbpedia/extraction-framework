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
import org.apache.commons.compress.compressors.bzip2._
import java.util.zip._
import scala.io.Codec.UTF8

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
        
        val source = XMLSource.fromReader(reader(file), language, title => namespaces.contains(title.namespace))
        
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
  
  // TODO: The rest is copied from ConfigLoader.scala. We should find a central place for the 
  // auto-unzip stuff. Maybe in core, but I don't like the added dependency on commons-compress.
  
  private def reader(file: File): () => Reader = {
    val unzip = unzipper(file.getName)
    () => new InputStreamReader(unzip(new FileInputStream(file)), UTF8)
  }

  /**
   * @return stream zipper function
   */
  private def unzipper(name: String): InputStream => InputStream = {
    unzippers.getOrElse(suffix(name), identity)
  }
  
  /**
   * @return file suffix
   */
  private def suffix(name: String): String = {
    name.substring(name.lastIndexOf('.') + 1)
  }
  
  private val unzippers = Map[String, InputStream => InputStream] (
    "gz" -> { new GZIPInputStream(_) }, 
    "bz2" -> { new BZip2CompressorInputStream(_, true) } 
  )
    
}


package org.dbpedia.extraction.wikiparser.impl.wikipedia

import scala.io.{Source, Codec}
import javax.xml.stream.XMLInputFactory
import scala.collection.mutable
import org.dbpedia.extraction.util.WikiConfigDownloader
import java.io.{File, IOException, OutputStreamWriter, FileOutputStream, Writer}
import java.net.HttpRetryException

/**
 * Generates Namespaces.scala and Redirect.scala. Must be run with core/ as the current directory.
 */
object GenerateWikiConfig {
  
  private val inputDir = new File("src/test/resources/org/dbpedia/extraction/wikiparser/impl/wikipedia")
  private val outputDir = new File("src/main/scala/org/dbpedia/extraction/wikiparser/impl/wikipedia")
  // pattern for insertion point lines
  val Insert = """// @ insert (\w+) here @ //""".r
  
  def main(args: Array[String]) : Unit = {
    
    val errors = mutable.LinkedHashMap[String, String]()
    
    // (language -> (namespace name or alias -> code))
    val namespaceMap = mutable.LinkedHashMap[String, mutable.Map[String, Int]]()
    
    // (language -> redirect magic word aliases)
    val redirectMap = mutable.LinkedHashMap[String, mutable.Set[String]]()
    
    // (old language code -> new language code)
    val languageMap = mutable.LinkedHashMap[String, String]()
    
    val source = Source.fromURL("http://noc.wikimedia.org/conf/langlist")(Codec.UTF8)
    val languages = try source.getLines.toList finally source.close
    
    for (language <- "commons" :: languages)
    {
      print(language)
      try
      {
        val downloader = new WikiConfigDownloader(language, followRedirects = false)
        val (namespaces, aliases, magicwords) = downloader.download()
        namespaceMap(language) = aliases ++ namespaces // order is important - aliases first
        redirectMap(language) = magicwords("redirect")
        println(" - OK")
      } catch {
        case hrex : HttpRetryException => {
          languageMap(language) = hrex.getMessage 
          println(" - redirected to "+hrex.getMessage)
        }
        case ioex : IOException => {
          errors(language) = ioex.getMessage
          println(" - "+ioex.getMessage)
        }
      }
    }
    
    val namespaceStr =
    build("namespaces", languageMap, namespaceMap) { (language, namespaces, s) =>
      // LinkedHashMap to preserve order, which is important because in the reverse map 
      // in Namespaces.scala the canonical name must be overwritten by the localized value.
      s +"    private def "+language.replace('-','_')+"_namespaces = LinkedHashMap("
      var firstNS = true
      for ((name, code) <- namespaces) {
        if (firstNS) firstNS = false else s +","
        s +"\""+name+"\" -> "+code
      }
      s +")\n"
    }
    
    val redirectStr =
    build("redirects", languageMap, redirectMap) { (language, redirects, s) =>
      s +"    private def "+language.replace('-','_')+"_redirects = Set("
      var firstRe = true
      for (name : String <- redirects) {
        if (firstRe) firstRe = false else s +","
        s +"\""+name+"\""
      }
      s +")\n"
    }

    var s = new StringPlusser
    for ((language, message) <- errors) s +"// "+language+" - "+message+"\n"
    val errorStr = s toString
    
    generate("Namespaces.scala", Map("namespaces" -> namespaceStr, "errors" -> errorStr))
    generate("Redirect.scala", Map("redirects" -> redirectStr, "errors" -> errorStr))
  }
  
  /**
   * Generate file by replacing insertion point lines by strings and copying all other lines.
   * @param map from insertion point name to replacement string
   */
  private def generate(fileName : String, strings : Map[String, String]) : Unit =
  {
    val source = Source.fromFile(new File(inputDir, fileName+".txt"))(Codec.UTF8)
    try  {
      val writer = new OutputStreamWriter(new FileOutputStream(new File(outputDir, fileName)), "UTF-8")
      try {
        for (line <- source.getLines) line match {
          case Insert(name) => writer write strings.getOrElse(name, throw new Exception("unknown insertion point "+line))
          case _ => writer.write(line); writer.write('\n')
        }
      } finally writer.close
    } finally source.close
  }
  
  private def build[V](tag : String, languages : mutable.Map[String, String], values : mutable.Map[String, V])
    (append : (String, V, StringPlusser) => Unit) : String =
  {
    var s = new StringPlusser
    
    // We used to generate the map as one huge value, but then constructor code is generated
    // that is so long that the JVM  doesn't load it. So we have to use separate functions.
    var firstLang = true
    s +"    Map("
    
    for (language <- values.keys) {
      if (firstLang) firstLang = false else s + "," 
      s +"\""+language+"\"->"+language.replace('-', '_')+"_"+tag
    }
    
    for ((from, to) <- languages) {
      if (firstLang) firstLang = false else s +"," 
      s +"\""+from+"\"->"+to.replace('-', '_')+"_"+tag
    }
    
    s +")\n"
    
    for ((language, value) <- values) append(language, value, s)
    
    s +"\n"
    
    s toString
  }
  
}

private class StringPlusser {
  private val sb = new java.lang.StringBuilder // use Java StringBuilder directly, not through Scala wrapper 
  def + (value : AnyRef): this.type = { sb append value; this }
  def + (value : Int): this.type = { sb append value; this }
  // TODO: add specialized + methods for other value types
  override def toString = sb toString
}


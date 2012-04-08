package org.dbpedia.extraction.wikiparser.impl.wikipedia

import scala.io.{Source, Codec}
import javax.xml.stream.XMLInputFactory
import scala.collection.mutable
import org.dbpedia.extraction.util.WikiConfigDownloader
import java.io.{File, IOException, OutputStreamWriter, FileOutputStream, Writer}

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
    
    val namespaceMap = mutable.LinkedHashMap[String, mutable.Map[String, Int]]()
    
    val redirectMap = mutable.LinkedHashMap[String, mutable.Set[String]]()
    
    val source = Source.fromURL("http://noc.wikimedia.org/conf/langlist")(Codec.UTF8)
    val languages = try source.getLines.toList finally source.close
    
    for (language <- "commons" :: languages) {
      print(language)
      try
      {
        val (namespaces, aliases, redirects) = new WikiConfigDownloader(language).download()
        namespaceMap.put(language, aliases ++ namespaces) // order is important - aliases first
        redirectMap.put(language, redirects)
        println(" - OK")
      } catch {
        case ioex : IOException => {
          errors.put(language, ioex.getMessage)
          println(" - "+ioex.getMessage)
        }
      }
    }
    
    generate("Namespaces.scala", Map("namespaces" -> buildNamespaces(namespaceMap), "errors" -> buildErrors(errors)))
    
    generate("Redirect.scala", Map("redirects" -> buildRedirects(redirectMap)))
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
  
  private def buildNamespaces(map : mutable.Map[String, mutable.Map[String, Int]]) : String = 
  {
    var sb = new StringPlusser
    
    // We used to generate the map as one huge value, but then constructor code is generated
    // that is so long that the JVM  doesn't load it. So we have to use separate functions.
    var firstLang = true
    sb + "    Map("
    for ((language, namespaces) <- map) {
      if (firstLang) firstLang = false else sb + "," 
      sb + "\"" + language + "\" -> " + language.replace('-', '_') + "_namespaces"
    }
    sb + ")\n"
    
    for ((language, namespaces) <- map) {
      // LinkedHashMap to preserve order, which is important because in the reverse map 
      // in Namespaces.scala the canonical name must be overwritten by the localized value.
      sb + "    private def " + language.replace('-','_') + "_namespaces = LinkedHashMap("
      var firstNS = true
      for ((name, code) <- namespaces) {
        if (firstNS) firstNS = false else sb + ","
        sb + "\"" + name + "\" -> " + code
      }
      sb + ")\n"
    }
    sb + "\n"
    
    sb toString
  }
  
  private def buildErrors(map : mutable.Map[String, String]) : String = 
  {
    var sb = new StringPlusser
    for ((language, message) <- map) sb + "// " + language + " - " + message + "\n"
    sb toString
  }
  
  private def buildRedirects(map : mutable.Map[String, mutable.Set[String]]) : String = 
  {
    var sb = new StringPlusser
    
    var firstLang = true
    for ((language, redirects) <- map) {
      if (firstLang) firstLang = false else sb + ",\n"
      sb + "        \"" + language + "\" -> Set("
      var firstRe = true
      for (name : String <- redirects) {
        if (firstRe) firstRe = false else sb + ","
        sb + "\"" + name + "\""
      }
      sb + ")"
    }
    sb + "\n"
    
    sb toString
  }
  
}

private class StringPlusser {
  private val sb = new java.lang.StringBuilder // use Java StringBuilder directly, not through Scala wrapper 
  def + (value : AnyRef): this.type = { sb append value; this }
  def + (value : Int): this.type = { sb append value; this }
  // TODO: add specialized + methods for other value types
  override def toString = sb toString
}


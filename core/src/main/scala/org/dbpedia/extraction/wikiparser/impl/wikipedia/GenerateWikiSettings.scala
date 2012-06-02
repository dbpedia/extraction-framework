package org.dbpedia.extraction.wikiparser.impl.wikipedia

import scala.io.{Source, Codec}
import javax.xml.stream.XMLInputFactory
import scala.collection.{Map,Set}
import scala.collection.mutable.{LinkedHashMap,LinkedHashSet}
import org.dbpedia.extraction.util.{Language,LazyWikiCaller,WikiSettingsReader,StringUtils,StringPlusser}
import java.io.{File,IOException,OutputStreamWriter,FileOutputStream,Writer}
import java.net.{URL,HttpRetryException}

/**
 * Generates Namespaces.scala and Redirect.scala. Must be run with core/ as the current directory.
 * 
 * FIXME: This should be used to download the data for just one language and maybe store it in a 
 * text file or simply in memory. (Loading the stuff only takes between .2 and 2 seconds per language.) 
 * Currently this class is used to generate two huge configuration classes for all languages. 
 * That's not good.
 * 
 * TODO: error handling. So far, it didn't seem necessary. api.php seems to work, and this
 * class is so far used with direct human supervision.
 * 
 */
object GenerateWikiSettings {
  
  private val inputDir = new File("src/test/resources/org/dbpedia/extraction/wikiparser/impl/wikipedia")
  private val outputDir = new File("src/main/scala/org/dbpedia/extraction/wikiparser/impl/wikipedia")
  // pattern for insertion point lines
  val Insert = """// @ insert (\w+) here @ //""".r
  
  def main(args: Array[String]) : Unit = {
    
    val millis = System.currentTimeMillis
    
    require(args != null && args.length == 2 && args(0) != null && args(1) != null, "need two arguments: base dir, overwrite flag (true/false)")
    
    val baseDir = new File(args(0))
    if (! baseDir.isDirectory) throw new IOException("["+baseDir+"] is not an existing directory")
    
    val overwrite = args(1).toBoolean
    
    val followRedirects = false
    
    // language -> error message
    val errors = LinkedHashMap[String, String]()
    
    // language -> (namespace name or alias -> code)
    val namespaceMap = new LinkedHashMap[String, Map[String, Int]]()
    
    // language -> redirect aliases
    val redirectMap = new LinkedHashMap[String, Set[String]]()
    
    // old language code -> new language code
    val languageMap = new LinkedHashMap[String, String]()
    
    // Note: langlist is sometimes not correctly sorted (done by hand), but no problem for us.
    val source = Source.fromURL("http://noc.wikimedia.org/conf/langlist")(Codec.UTF8)
    var languages = try source.getLines.toList finally source.close
    languages = "mappings" :: "commons" :: languages
    
    // newInstance is expensive, call it only once
    val factory = XMLInputFactory.newInstance
    
    println("generating wiki config for "+languages.length+" languages")
    
    for (code <- languages)
    {
      print(code)
      val language = Language(code)
      val file = new File(baseDir, language.wikiCode+"wiki-configuration.xml")
      try
      {
        val url = new URL(language.apiUri+"?"+WikiSettingsReader.query)
        val caller = new LazyWikiCaller(url, followRedirects, file, overwrite)
        val settings = caller.execute { stream =>
          val xml = factory.createXMLEventReader(stream)
          WikiSettingsReader.read(xml)
        }
        namespaceMap(code) = settings.aliases ++ settings.namespaces // order is important - aliases first
        redirectMap(code) = settings.magicwords("redirect")
        // TODO: also use interwikis
        println(" - OK")
      } catch {
        case hrex: HttpRetryException => {
          val target = hrex.getMessage
          languageMap(code) = target
          println(" - redirected to "+target)
        }
        case ioex: IOException => {
          val error = ioex.getMessage
          errors(code) = error
          println(" - "+error)
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
        s +"\""+name+"\"->"+(if (code < 0) " " else "")+code
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
    
    println("generated wiki config for "+languages.length+" languages in "+StringUtils.prettyMillis(System.currentTimeMillis - millis))
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
  
  private def build[V](tag : String, languages : Map[String, String], values : Map[String, V])
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

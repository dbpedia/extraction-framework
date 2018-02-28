package org.dbpedia.extraction.wikiparser.impl.wikipedia

import scala.io.{Codec, Source}
import scala.collection.{Map, Set, mutable}
import org.dbpedia.extraction.util._
import java.io.{File, FileOutputStream, IOException, OutputStreamWriter}
import java.net.HttpRetryException

import org.dbpedia.extraction.config.ExtractionLogger

import scala.util.matching.Regex
import scala.util.{Failure, Success}

/**
 * Generates Namespaces.scala, Redirect.scala, CategoryRedirect.scala and Disambiguation.scala. Must be run with core/ as the current directory.
 */
object GenerateWikiSettings {

  private val logger = ExtractionLogger.getLogger(getClass, Language.English)

  private val inputDir = new File("src/test/resources/org/dbpedia/extraction/wikiparser/impl/wikipedia")
  private val outputDir = new File("src/main/scala/org/dbpedia/extraction/wikiparser/impl/wikipedia")

  private val englishCategoryRedirectTemplate = "Template:Category redirect"
  val englishCategoryInfoboxTemplates = "Category:Infobox templates"
  val dontQueryThoseLanguagesForInfoboxtemplates = List("ur")           //these are necessary since their category tree is HUGE (probably auto generated)

  // pattern for insertion point lines
  val Insert: Regex = """// @ insert (\w+) here @ //""".r
  
  def main(args: Array[String]) : Unit = {
    
    val millis = System.currentTimeMillis
    
    require(args != null && args.length == 2 && args(0) != null && args(1) != null, "need two arguments: base dir, overwrite flag (true/false)")
    
    val baseDir = new File(args(0))
    if (! baseDir.isDirectory) throw new IOException("["+baseDir+"] is not an existing directory")
    
    val overwrite = args(1).toBoolean

    // language -> error message
    val errors = mutable.LinkedHashMap[String, String]()
    
    // language -> (namespace name or alias -> code)
    val namespaceMap = new mutable.LinkedHashMap[String, Map[String, Int]]()
    
    // language -> redirect aliases
    val redirectMap = new mutable.LinkedHashMap[String, Set[String]]()

    // language -> category redirect template name of that language
    val categoryRedirects = new mutable.LinkedHashMap[String, Set[String]]()

    // old language code -> new language code
    val languageMap = new mutable.LinkedHashMap[String, String]()

    // language -> disambiguations
    val disambiguationsMap = new mutable.LinkedHashMap[String, Set[String]]()
    
    // Note: langlist is sometimes not correctly sorted (done by hand), but no problem for us.
    //
    // langlist was unavailable for several days in April 2013. Reported by Omri Oren:
    // https://github.com/dbpedia/extraction-framework/issues/37
    // It's back, thanks to Omri and Krinkle (Wikimedia). If it goes away again, we may use the copy in git:
    // https://gerrit.wikimedia.org/r/gitweb?p=operations/mediawiki-config.git;a=blob_plain;f=langlist
    // I don't really trust that long and ugly URL though, so I will leave the old URL for now. JC 2013-04-21

    val source = Source.fromURL(Language.wikipediaLanguageUrl)(Codec.UTF8)
    val wikiLanguages = try source.getLines.toList finally source.close
    val languages = "mappings" :: "commons" :: "wikidata" :: wikiLanguages

    logger.info("Collecting Category:Redirect templates.")
    //collect category redirects (uses langlinks from a known category redirect page, to find the same in other languages)
    new WikiLangLinkReader().execute(Language.English, englishCategoryRedirectTemplate)
      .foreach(ent => categoryRedirects.put(ent._1.wikiCode, ent._2.map {
        case WikiDisambigReader.TemplateNameRegex(templateName) => templateName
        case d => d
      }))

    logger.info("generating wiki config for "+languages.length+" languages")
    languages.foreach { code =>
      try {
        val language = Language(code)
        val file = new File(baseDir, language.wikiCode + "wiki-configuration.xml")
        val disambigFile = new File(baseDir, language.wikiCode + "wiki-disambiguation-templates.xml")

        try {
          val settings: WikiSettings = new WikiSettingsReader(language).execute(file, overwrite) match{
            case Success(s) => s
            case Failure(f) => throw f
          }

          namespaceMap(code) = settings.aliases ++ settings.namespaces // order is important - aliases first

          //get redirects
          redirectMap(code) = settings.magicwords("redirect")

          if (wikiLanguages contains code) {

            new WikiDisambigReader(language).execute(disambigFile, overwrite) match{
              case Success(s) => disambiguationsMap(code) = s
              case Failure(f) => throw f
            }
          }
          // TODO: also use interwikis
          logger.info(s"$code - OK")
        } catch {
          case hrex: HttpRetryException => {
            val target = hrex.getMessage
            languageMap(code) = target
            logger.info(s"$code - redirected to $target")
          }
          case ioex: IOException => {
            val error = ioex.getMessage
            errors(code) = error
            logger.warn(s"$code - Error: $error")
          }
          case i: Throwable =>  logger.error("Error while generating settings: ", i)
        }
      }
      catch {
        case uae: IllegalArgumentException => logger.error("Error while generating settings: ", uae)
      }
    }

    // LinkedHashMap to preserve order, which is important because in the reverse map 
    // in Namespaces.scala the canonical name must be overwritten by the localized value.
    val namespaceStr =
    build("namespaces", "LinkedHashMap", languageMap, namespaceMap) { (s, entry) =>
      val (name, code) = entry
      s +"\""+name+"\"->"+(if (code < 0) " " else "")+code
    }
    
    val redirectStr =
    build("redirects", "Set", languageMap, redirectMap) { (s, entry) =>
      val name = entry
      s +"\""+name+"\""
    }

    val categoryRedirectStr =
      build("categoryRedirects", "Set", languageMap, categoryRedirects) { (s, entry) =>
        val name = entry
        s +"\""+name+"\""
      }

    val disambiguationsStr =
      build("disambiguations", "Set", languageMap, disambiguationsMap) { (s, entry) =>
        val name = entry
        s +"\""+name+"\""
      }

    val s = new StringPlusser
    for ((language, message) <- errors) s +"// "+language+" - "+message+"\n"
    val errorStr = s.toString
    
    generate("Namespaces.scala", Map("namespaces" -> namespaceStr, "errors" -> errorStr))
    generate("Redirect.scala", Map("redirects" -> redirectStr, "errors" -> errorStr))
    generate("CategoryRedirect.scala", Map("categoryRedirects" -> categoryRedirectStr, "errors" -> errorStr))
    generate("Disambiguation.scala", Map("disambiguations" -> disambiguationsStr, "errors" -> errorStr))

    logger.info("generated wiki config for "+languages.length+" languages in "+StringUtils.prettyMillis(System.currentTimeMillis - millis))
  }
  
  /**
   * Generate file by replacing insertion point lines by strings and copying all other lines.
    *
    * @param fileName from insertion point name to replacement string
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
      } finally writer.close()
    } finally source.close
  }
  
  private def build[V](tag : String, coll: String, languages : Map[String, String], values : Map[String, Iterable[V]])
    (append : (StringPlusser, V) => Unit) : String =
  {
    var s = new StringPlusser
    
    // We used to generate the map as one huge value, but then constructor code is generated
    // that is so long that the JVM  doesn't load it. So we have to use separate functions.
    var first = true
    s +"    Map("
    
    for (language <- values.keys) {
      if (first) first = false else s + "," 
      s +"\""+language+"\"->"+language.replace('-', '_')+"_"+tag
    }
    
    for ((from, to) <- languages) {
      if (first) first = false else s +"," 
      s +"\""+from+"\"->"+to.replace('-', '_')+"_"+tag
    }
    
    s +")\n"
    
    for ((language, iterable) <- values) {
      s +"    private def "+language.replace('-','_')+"_"+tag+" = "+coll+"("
      first = true
      for (item <- iterable) {
        if (first) first = false else s +","
        append(s, item)
      }
      s +")\n"
    }
    
    s +"\n"
    
    s.toString
  }
  
}

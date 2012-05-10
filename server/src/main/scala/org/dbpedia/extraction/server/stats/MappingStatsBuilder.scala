package org.dbpedia.extraction.server.stats

import java.util.logging.Logger
import io.Source
import java.lang.IllegalArgumentException
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.util.{WikiUtil, Language}
import scala.Serializable
import scala.collection
import scala.collection.mutable
import java.io._
import org.dbpedia.extraction.destinations.{DBpediaDatasets,Dataset}
import org.dbpedia.extraction.server.stats.CreateMappingStats._
import java.net.{URLDecoder, URLEncoder}
import org.dbpedia.extraction.util.StringUtils.prettyMillis

class UriTriple(uris: Int) {
  /** @param line must not start or end with whitespace - use line.trim. */
  def unapplySeq(line: String) : Option[Seq[String]] =  {
    var count = 0
    var index = 0
    val triple = new Array[String](3)
    while (count < uris) {
      if (index == line.length || line.charAt(index) != '<') return None
      var end = index
      do {
        end += 1
        if (end == line.length) return None
      } while (line.charAt(end) != '>')
      triple(count) = line.substring(index + 1, end)
      count += 1
      index = end + 1
      while (index < line.length && line.charAt(index) == ' ') {
        index += 1 // skip space
      } 
    }
    
    if (uris == 2) { // literal
      if (index == line.length || line.charAt(index) != '"') return None
      var end = index + 1
      while (line.charAt(end) != '"') {
        if (line.charAt(end) == '\\') end += 1
        end += 1
        if (end >= line.length) return None
      } 
      triple(2) = line.substring(index + 1, end)
      index = end + 1
      if (index == line.length) return None
      val ch = line.charAt(index)
      if (ch == '@') { // lang: @[a-zA-Z][a-zA-Z0-9-]*
        index += 1 // skip '@'
        if (index == line.length) return None
        var c = line.charAt(index)
        if ((c < 'A' || c > 'Z') && (c < 'a' || c > 'z')) return None
        do {
          index += 1 // skip last lang char
          if (index == line.length) return None
          c = line.charAt(index)
        } while (c == '-' || (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'))
      }
      else if (ch == '^') { // type uri: ^^<...>
        index += 1 // skip '^'
        if (index == line.length || line.charAt(index) != '^') return None
        index += 1 // skip '^'
        if (index == line.length || line.charAt(index) != '<') return None
        do {
          index += 1
          if (index == line.length) return None
        } while (line.charAt(index) != '>')
        index += 1 // skip '>'
      } else {
        return None
      }
      while (index < line.length && line.charAt(index) == ' ') {
        index += 1 // skip space
      } 
    }
    if (index + 1 != line.length || line.charAt(index) != '.') return None
    Some(triple)
  }
  
}
    
object ObjectTriple extends UriTriple(3)

object DatatypeTriple extends UriTriple(2)

class MappingStatsBuilder(statsDir : File, language: Language)
extends MappingStatsConfig(statsDir, language)
{
    private val logger = Logger.getLogger(getClass.getName)

    private val resourceUriPrefix = language.resourceUri.append("")

    def buildStats(redirectsFile: File, infoboxPropsFile: File, templParamsFile: File, paramsUsageFile: File): Unit =
    {
        var templatesMap = new mutable.HashMap[String, TemplateStatsBuilder]()
        
        println("Reading redirects from " + redirectsFile)
        val redirects = loadTemplateRedirects(redirectsFile)
        println("Found " + redirects.size + " redirects")
        
        println("Using Template namespace " + templateNamespace + " for language " + language.wikiCode)
        
        println("Counting templates in " + infoboxPropsFile)
        countTemplates(infoboxPropsFile, templatesMap, redirects)
        println("Found " + templatesMap.size + " different templates")


        println("Loading property definitions from " + templParamsFile)
        propertyDefinitions(templParamsFile, templatesMap, redirects)

        println("Counting properties in " + paramsUsageFile)
        countProperties(paramsUsageFile, templatesMap, redirects)
        
        val wikiStats = new WikipediaStats(language, redirects.toMap, templatesMap.map(e => (e._1, e._2.build)).toMap)
        
        logger.info("Serializing "+language.wikiCode+" wiki statistics to " + mappingStatsFile)
        val output = new OutputStreamWriter(new FileOutputStream(mappingStatsFile), "UTF-8")
        try wikiStats.write(output) finally output.close()
    }

    private def eachLine(file: File)(process: String => Unit) : Unit = {
        val millis = System.currentTimeMillis
        var count = 0
        val source = Source.fromFile(file, "UTF-8")
        try {
            for (line <- source.getLines()) {
                process(line)
                count += 1
                if (count % 10000 == 0) print(count+" lines\r")
            }
        } finally source.close
        println(count+" lines - "+prettyMillis(System.currentTimeMillis - millis))
    }

    private def loadTemplateRedirects(file: File): mutable.Map[String, String] =
    {
      val redirects = new mutable.HashMap[String, String]()
      eachLine(file) {
        line => line.trim match {
          case ObjectTriple(subj, pred, obj) => {
            val templateName = cleanUri(subj)
            if (templateName.startsWith(templateNamespace)) {
              redirects(templateName) = cleanUri(obj)
            }
          }
          case str => if (str.nonEmpty && ! str.startsWith("#")) throw new IllegalArgumentException("line did not match object triple syntax: " + line)
        }
      }
      
      println("Resolving "+redirects.size+" redirects")
      // resolve transitive closure
      for ((source, target) <- redirects)
      {
          var cyclePrevention: Set[String] = Set()
          var closure = target
          while (redirects.contains(closure) && !cyclePrevention.contains(closure))
          {
              closure = redirects.get(closure).get
              cyclePrevention += closure
          }
          redirects(source) = closure
      }

      redirects
    }
    
    /**
     * @param fileName name of file generated by InfoboxExtractor, e.g. infobox_properties_en.nt
     */
    private def countTemplates(file: File, resultMap: mutable.Map[String, TemplateStatsBuilder], redirects: mutable.Map[String, String]): Unit =
    {
        // iterate through infobox properties
        eachLine(file) {
            line => line.trim match {
                // if there is a wikiPageUsesTemplate relation
                case ObjectTriple(subj, pred, obj) => if (unescape(pred) contains "wikiPageUsesTemplate")
                {
                    var templateName = cleanUri(obj)
                    
                    // resolve redirect for *object*
                    templateName = redirects.getOrElse(templateName, templateName)

                    // lookup the *object* in the resultMap, create a new TemplateStats object if not found,
                    // and increment templateCount
                    resultMap.getOrElseUpdate(templateName, new TemplateStatsBuilder).templateCount += 1
                }
                case DatatypeTriple(_,_,_) => // ignore
                case str => if (str.nonEmpty && ! str.startsWith("#")) throw new IllegalArgumentException("line did not match object or datatype triple syntax: " + line)
            }
        }
    }

    private def propertyDefinitions(file: File, resultMap: mutable.Map[String, TemplateStatsBuilder], redirects: mutable.Map[String, String]): Unit =
    {
        // iterate through template parameters
        eachLine(file) {
            line => line.trim match {
                case DatatypeTriple(subj, pred, obj) =>
                {
                    var templateName = cleanUri(subj)
                    val propertyName = cleanValue(obj)
                    
                    // resolve redirect for *subject*
                    templateName = redirects.getOrElse(templateName, templateName)

                    // lookup the *subject* in the resultMap
                    // skip the templates that are not used in any page
                    for (stats <- resultMap.get(templateName))
                    {
                        // add object to properties map with count 0
                        stats.properties.put(propertyName, 0)
                    }
                }
                case str => if (str.nonEmpty && ! str.startsWith("#")) throw new IllegalArgumentException("line did not match datatype triple syntax: " + line)
            }
        }
    }

    private def countProperties(file: File, resultMap: mutable.Map[String, TemplateStatsBuilder], redirects: mutable.Map[String, String]) : Unit =
    {
        // iterate through infobox test
        eachLine(file) {
            line => line.trim match {
                case DatatypeTriple(subj, pred, obj) => {
                    var templateName = cleanUri(pred)
                    val propertyName = cleanValue(obj)
                    
                    // resolve redirect for *predicate*
                    templateName = redirects.getOrElse(templateName, templateName)

                    // lookup the *predicate* in the resultMap
                    // skip the templates that are not found (they don't occur in Wikipedia)
                    for(stats <- resultMap.get(templateName)) {
                        // lookup *object* in the properties map
                        //skip the properties that are not found with any count (they don't occurr in the template definition)
                        if (stats.properties.contains(propertyName)) {
                            // increment count in properties map
                            stats.properties.put(propertyName, stats.properties(propertyName) + 1)
                        }
                    }
                }
                case str => if (str.nonEmpty && ! str.startsWith("#")) throw new IllegalArgumentException("line did not match datatype triple syntax: " + line)
            }
        }
    }
    
    private def stripUri(uri: String): String = {
        if (! uri.startsWith(resourceUriPrefix)) throw new Exception(uri)
        WikiUtil.wikiDecode(uri.substring(resourceUriPrefix.length))
    }
    
    private def cleanUri(uri: String) : String = cleanName(stripUri(unescape(uri)))
    
    private def cleanValue(value: String) : String = cleanName(unescape(value))
    
    // values may contain line breaks, which mess up our file format, so let's remove them.
    private def cleanName(name: String) : String = name.replaceAll("\r|\n", "")

    private def unescape(value: String): String = {
        val sb = new java.lang.StringBuilder

        val inputLength = value.length
        var offset = 0

        while (offset < inputLength)
        {
            val c = value.charAt(offset)
            if (c != '\\') sb append c
            else
            {
                offset += 1
                // FIXME: check string length 
                val specialChar = value.charAt(offset)
                specialChar match
                {
                    case '"' => sb append '"'
                    case 't' => sb append '\t'
                    case 'r' => sb append '\r'
                    case '\\' => sb append '\\'
                    case 'n' => sb append '\n'
                    case 'u' =>
                    {
                        offset += 1
                        // FIXME: check string length 
                        val codepoint = value.substring(offset, offset + 4)
                        val character = Integer.parseInt(codepoint, 16).asInstanceOf[Char]
                        sb append character
                        offset += 3
                    }
                    case 'U' =>
                    {
                        offset += 1
                        // FIXME: check string length 
                        val codepoint = value.substring(offset, offset + 8)
                        val character = Integer.parseInt(codepoint, 16)
                        sb appendCodePoint character
                        offset += 7
                    }
                }
            }
            offset += 1
        }
        sb.toString
    }
}

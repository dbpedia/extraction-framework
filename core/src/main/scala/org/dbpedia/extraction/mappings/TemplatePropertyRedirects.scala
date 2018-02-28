package org.dbpedia.extraction.mappings

import java.io.File

import org.dbpedia.extraction.config.ExtractionLogger
import org.dbpedia.extraction.util.{IOUtils, Language}
import org.dbpedia.extraction.wikiparser.impl.simple.SimpleWikiParser
import org.dbpedia.extraction.wikiparser._

import scala.collection.mutable

class TemplatePropertyRedirects(val language: Language, private val validTemplateNames: Set[String] = Set()) {
  private val map = new mutable.HashMap[String, String]()
  private val expanderMap = new mutable.HashMap[String, TemplatePropertyExpander]()

  /**
    * enter a raw meta template node
    * @param templateName - the template name of the template page! (not the name/title of the template)
    * @param template - the meta template
    */
  private def enterTemplateString(templateName: String, template: String): Unit ={
    map.put(templateName, template)
  }

  /**
    * enter a raw meta template node
    * @param templateName - the template name of the template page! (not the name/title of the template)
    * @param template - the meta template
    */
  def enterTemplate(templateName: String, template: TemplateNode): Unit ={
    map.put(templateName, template.toWikiText)
  }

  private def getExpander(templateName: String): Option[TemplatePropertyExpander] ={
    expanderMap.get(templateName) match{
      case Some(ex) => Some(ex)
      case None => map.get(templateName) match{
        case Some(t) =>
          val title = WikiTitle.parse(templateName, language)
          SimpleWikiParser.apply(new WikiPage(title, t)) match{
            case Some(p) =>
              val template = p.containedTemplateNodes(validTemplateNames).headOption.getOrElse(return None)
              Option(new TemplatePropertyExpander(template))
            case None => None
          }
        case None => None
      }
    }
  }

  /**
    * If meta template is known, expand template
    * @param template - the template to expand
    * @return
    */
  def resolveTemplate(template: TemplateNode): Option[TemplateNode] ={
    val title = template.title.decoded
    getExpander(title).map(ex => ex.resolveTemplate(template))
  }
}

object TemplatePropertyRedirects{

  private val logger = ExtractionLogger.getLogger(getClass, Language.None)

  /**
    * load from an existing map
    * @param map
    * @return
    */
  def fromMap(map : Map[String, String], lang: Language, validTemplateNames: Set[String] = Set()): TemplatePropertyRedirects ={
    val reds = new TemplatePropertyRedirects(lang, validTemplateNames)
    map.foreach(e => reds.enterTemplateString(e._1, e._2))
    reds
  }

  /**
    * Loads the redirects from a cache file.
    */
  def loadFromCache(cache : File, lang: Language, validTemplateNames: Set[String] = Set()) : TemplatePropertyRedirects =
  {
    logger.info("Loading template property redirects from cache file "+cache)
    TemplatePropertyRedirects.fromMap(IOUtils.loadSerializedObject[mutable.HashMap[String, String]](cache).toMap, lang, validTemplateNames)
  }


  /**
    * Loads the redirects from a cache file.
    */
  def saveToCache(cache : File, red: TemplatePropertyRedirects) : Unit =
  {
    logger.info("Saving template property redirects to cache file "+cache)
    IOUtils.serializeToObjectFile(cache, red.map)
  }
}

package org.dbpedia.extraction.util

import javax.xml.stream.XMLInputFactory
import scala.collection.{Map,Set}
import scala.collection.mutable.{LinkedHashMap,LinkedHashSet}
import org.dbpedia.extraction.util.XMLEventAnalyzer.richStartElement
import javax.xml.stream.XMLEventReader

class WikiSettings (
  /** name -> code */
  val namespaces: Map[String, Int], 
  /** alias -> code */
  val aliases: Map[String, Int], 
  /** name -> aliases */
  val magicwords: Map[String, Set[String]], 
  /** prefix -> url pattern using "$1" as place holder */
  val interwikis: Map[String, String]
)

object WikiSettingsReader {
  
  /**
   * The query for api.php, without the leading '?'. 
   * Order of namespaces|namespacealiases|magicwords|interwikimap is important.
   */
  val query = "action=query&format=xml&meta=siteinfo&siprop=namespaces|namespacealiases|magicwords|interwikimap"
}

/**
 * Reads result of the api.php query above.
 * 
 * Note: we use linked sets and maps to preserve order. Scala currently has no immutable linked 
 * collections, so we use mutable ones (which should also improve performance). Calling .toMap
 * to make them immutable would destroy the order, so we simply return them, but as an immutable
 * interface. Malicious users could still downcast and mutate. Meh.
 */
class WikiSettingsReader(in : XMLEventAnalyzer) {
  
  def this(reader: XMLEventReader) = this(new XMLEventAnalyzer(reader))
  
  /**
   * @return settings
   */
  def read(): WikiSettings = {
    in.document { _ =>
      in.element("api") { _ =>
        in.element("query") { _ =>
          val namespaces = readNamespaces("namespaces", true)
          val aliases = readNamespaces("namespacealiases", false)
          val magicwords = readMagicWords()
          val interwikis = readInterwikis()
          new WikiSettings(namespaces, aliases, magicwords, interwikis)
        }
      }
    }
  }
  
  /**
   * @return namespaces or aliases (name -> code)
   */
  private def readNamespaces(tag : String, canonical : Boolean) : Map[String, Int] = {
    in.element(tag) { _ =>
      // LinkedHashMap to preserve order
      val namespaces = new LinkedHashMap[String, Int]
      in.elements("ns") { ns =>
        val id = (ns getAttr "id").toInt
        in.text { text => 
          // order is important here - canonical first, because in the reverse map 
          // in Namespaces.scala it must be overwritten by the localized value.
          if (canonical && id != 0) namespaces(ns getAttr "canonical") = id
          namespaces(text) = id
        }
      }
      namespaces
    }
  }
    
  /**
   * @return magic words (name -> aliases)
   */
  private def readMagicWords() : Map[String, Set[String]] = {
    in.element("magicwords") { _ =>
      // LinkedHashMap to preserve order (although it's probably not important)
      val magicwords = new LinkedHashMap[String, Set[String]]
      in.elements("magicword") { mw =>
        // LinkedHashSet to preserve order (although it's probably not important)
        val aliases = new LinkedHashSet[String]
        in.elements("aliases") { _ =>
          in.elements("alias") { _ =>
            in.text { text => 
              aliases += text
            }
          }
        }
        magicwords.put(mw getAttr "name", aliases)
      }
      magicwords
    }
  }
  
  private def readInterwikis(): Map[String, String] = {
    in.element("interwikimap") { _ =>
      // LinkedHashMap to preserve order (although it's probably not important)
      val interwikis = new LinkedHashMap[String, String]
      in.elements("iw") { iw =>
        interwikis(iw getAttr "prefix") = iw getAttr "url"
      }
      interwikis
    }
  }

}
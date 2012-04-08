package org.dbpedia.extraction.util

import java.io.IOException
import java.net.{URL, HttpURLConnection}
import javax.xml.stream.XMLInputFactory
import scala.collection.mutable
import org.dbpedia.extraction.util.XMLEventAnalyzer.richStartElement

/**
 * Downloads namespace names, namespace alias names and redirect magic words for a wikipedia 
 * language edition via api.php.
 * 
 * FIXME: This should be used to download the data for just one language and maybe store it in a 
 * text file or simply in memory. (Loading the stuff only takes between .2 and 2 seconds per language.) 
 * Currently this class is used to generate one huge configuration class for all languages. 
 * That's not good.
 * 
 * FIXME: also use
 * http://en.wikipedia.org/w/api.php?action=query&meta=siteinfo&siprop=interwikimap
 * http://de.wikipedia.org/w/api.php?action=query&meta=siteinfo&siprop=interwikimap
 * which are identical except for stuff like q => en.wikiquote.org / de.wikiquote.org etc.
 * 
 * TODO: error handling. So far, it didn't seem necessary. api.php seems to work, and this
 * class is used with direct human supervision.
 */
class WikiConfigDownloader(language : String) {
  
  val url = new URL(api(language)+"?action=query&format=xml&meta=siteinfo&siprop=namespaces|namespacealiases|magicwords")
  
  private def api(language : String) : String =
    "http://"+language+"."+(if (language == "commons") "wikimedia" else "wikipedia")+".org/w/api.php"
    
  /**
   * @return namespaces (name -> code), namespace aliases (name -> code), redirect tags
   */
  def download() : (mutable.Map[String, Int], mutable.Map[String, Int], mutable.Set[String]) = 
  {
    val conn = url.openConnection.asInstanceOf[HttpURLConnection]
    try {
      conn.setInstanceFollowRedirects(false)
      val response = conn.getResponseCode
      if (response != HttpURLConnection.HTTP_OK) throw new IOException("URL '"+url+"' replied '"+conn.getResponseMessage+"' - Location: '"+conn.getHeaderField("Location"))
        
      val factory = XMLInputFactory.newInstance
      val stream = conn.getInputStream
      try new WikiConfigReader(new XMLEventAnalyzer(factory.createXMLEventReader(stream))).read()
      finally stream.close
    }
    finally conn.disconnect
  }
}

private class WikiConfigReader(in : XMLEventAnalyzer) {
  
  def read() : (mutable.Map[String, Int], mutable.Map[String, Int], mutable.Set[String]) = 
  {
    var namespaces : mutable.Map[String, Int] = null
    var aliases : mutable.Map[String, Int] = null
    var redirects : mutable.Set[String] = null
    
    in.document { _ =>
      in.element("api") { _ =>
        in.element("query") { _ =>
          namespaces = readNamespaces("namespaces", true)
          aliases = readNamespaces("namespacealiases", false)
          redirects = readMagicWords()("redirect")
        }
      }
    }
    
    (namespaces, aliases, redirects)
  }
  
  private def readNamespaces(tag : String, canonical : Boolean) : mutable.Map[String, Int] = 
  {
    // LinkedHashMap to preserve order
    val namespaces = mutable.LinkedHashMap[String, Int]()
    in.element(tag) { _ =>
      in.elements("ns") { ns =>
        val id = (ns getAttr "id").toInt
        in.text { text => 
          // order is important here - canonical first, because in the reverse map 
          // in Namespaces.scala it must be overwritten by the localized value.
          if (canonical && id != 0) namespaces.put(ns getAttr "canonical", id)
          namespaces.put(text, id)
        }
      }
    }
    namespaces 
  }
    
  /**
   * @return map from magic word name to aliases
   */
  private def readMagicWords() : mutable.Map[String, mutable.Set[String]] =
  {
    // LinkedHashMap to preserve order (although it's probably not important)
    val magic = mutable.LinkedHashMap[String, mutable.Set[String]]()
    in.element("magicwords") { _ =>
      in.elements("magicword") { mw =>
        // LinkedHashSet to preserve order (although it's probably not important)
        val aliases = magic.getOrElseUpdate(mw getAttr "name", mutable.LinkedHashSet[String]())
        in.elements("aliases") { _ =>
          in.elements("alias") { _ =>
            in.text { text => 
              aliases += text
            }
          }
        }
      }
    }
    magic
  }
    
}


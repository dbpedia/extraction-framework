package org.dbpedia.extraction.util

import java.io.IOException
import java.net.{URL, HttpURLConnection}
import javax.xml.stream.XMLInputFactory
import scala.collection.mutable
import org.dbpedia.extraction.util.XMLEventAnalyzer.richStartElement

/**
 * Downloads namespace names, namespace alias names and redirect magic words for a wikipedia 
 * language edition via api.php.
 * FIXME: This should be used to download the data for just one language and maybe store it in a file 
 * or simply in memory. (Loading the stuff only takes between .2 and 2 seconds per language.) 
 * Currently this class is used to generate code for all languages. That's not good.
 */
class WikiConfigDownloader(language : String) {
  
  val url = new URL("http://"+host(language)+"/w/api.php?action=query&format=xml&meta=siteinfo&siprop=namespaces|namespacealiases|magicwords")
  
  private def host(language : String) : String =
    language+"."+(if (language == "commons") "wikimedia" else "wikipedia")+".org"
    
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
          redirects = readRedirects()
        }
      }
    }
    
    (namespaces, aliases, redirects)
  }
  
  private def readNamespaces(tag : String, canonical : Boolean) : mutable.Map[String, Int] = 
  {
    val namespaces = mutable.LinkedHashMap[String, Int]()
    in.element(tag) { _ =>
      in.elements("ns") { ns =>
        val id = (ns getAttr "id").toInt
        in.text { text => 
          // order is important here - canonical first, because in the reverse map 
          // it must be overwritten by the localized value.
          if (canonical && id != 0) namespaces.put(ns getAttr "canonical", id)
          namespaces.put(text, id)
        }
      }
    }
    namespaces 
  }
    
  private def readRedirects() : mutable.Set[String] = 
  {
    val redirects = mutable.LinkedHashSet[String]()
    in.element("magicwords") { _ =>
      in.elements("magicword") { mw =>
        val name = mw getAttr "name"
        in.elements("aliases") { _ =>
          in.elements("alias") { _ =>
            in.text { text => 
              if (name == "redirect") redirects.add(text)
            }
          }
        }
      }
    }
    redirects 
  }
    
}


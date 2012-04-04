package org.dbpedia.extraction.util

import java.io.IOException
import java.net.{URL, HttpURLConnection}
import javax.xml.stream.XMLInputFactory
import scala.collection.mutable
import org.dbpedia.extraction.util.XMLEventAnalyzer.richStartElement

/**
 * Downloads namespace names, namespace alias names and redirect magic words for a wikipedia 
 * language edition via api.php.
 */
class WikiConfigDownloader(language : String) {
  
  val url = new URL("http://"+language+".wikipedia.org/w/api.php?action=query&format=xml&meta=siteinfo&siprop=namespaces|namespacealiases|magicwords")
    
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
          namespaces = readNS("namespaces")
          aliases = readNS("namespacealiases")
          redirects = readRedirects()
        }
      }
    }
    
    (namespaces, aliases, redirects)
  }
  
  private def readNS(tag : String ) : mutable.Map[String, Int] = 
  {
    val namespaces = mutable.LinkedHashMap[String, Int]()
    in.element(tag) { _ =>
      in.elements("ns") { ns =>
        val id = ns getAttr "id"
        in.text { text => 
          namespaces.put(text, id.toInt)
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


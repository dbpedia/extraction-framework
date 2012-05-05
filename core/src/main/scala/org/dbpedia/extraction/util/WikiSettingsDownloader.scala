package org.dbpedia.extraction.util

import java.io.{File,InputStream,FileInputStream,FileOutputStream,IOException}
import java.net.{URL,HttpRetryException,HttpURLConnection}
import java.net.URLDecoder.decode
import java.net.HttpURLConnection.{HTTP_OK,HTTP_MOVED_PERM,HTTP_MOVED_TEMP}
import javax.xml.stream.XMLInputFactory
import scala.collection.{Map,Set}
import scala.collection.mutable.{LinkedHashMap,LinkedHashSet}
import org.dbpedia.extraction.util.XMLEventAnalyzer.richStartElement

class WikiSettings (
  val namespaces: Map[String, Int], 
  val aliases: Map[String, Int], 
  val magicwords: Map[String, Set[String]], 
  val interwikis: Map[String, String]
)

  /**
 * Downloads namespace names, namespace alias names and redirect magic words for a wikipedia 
 * language edition via api.php.
 * 
 * FIXME: This should be used to download the data for just one language and maybe store it in a 
 * text file or simply in memory. (Loading the stuff only takes between .2 and 2 seconds per language.) 
 * Currently this class is used to generate two huge configuration classes for all languages. 
 * That's not good.
 * 
 * TODO: error handling. So far, it didn't seem necessary. api.php seems to work, and this
 * class is so far used with direct human supervision.
 * 
 * @param followRedirects if true, follow HTTP redirects for languages that have been renamed,
 * e.g. dk.wikipedia.org -> da.wikipedia.org. If false, throw a useful exception on redirects. 
 */
class WikiSettingsDownloader(language: Language, followRedirects: Boolean, overwrite: Boolean) {
  
  val url = new URL(api(language)+"?action=query&format=xml&meta=siteinfo&siprop=namespaces|namespacealiases|magicwords|interwikimap")
  
  private def api(language : Language) : String =
    "http://"+language.wikiCode+"."+(if (language.wikiCode == "commons") "wikimedia" else "wikipedia")+".org/w/api.php"
    
  /**
   * @return namespaces (name -> code), namespace aliases (name -> code), magic words (name -> aliases)
   * @throws HttpRetryException if followRedirects is false and this language is redirected to another 
   * language. The message of the HttpRetryException is the target language.
   * @throws IOException if another error occurs
   */
  def download(factory: XMLInputFactory, file: File = null): WikiSettings = 
  {
    if (file == null) {
      withConnection { in => read(in, factory) }
    } else {
      if (overwrite || ! file.exists) {
        withConnection { in => 
          val out = new FileOutputStream(file)
          try IOUtils.copy(in, out) finally out.close 
        } 
      }
      val in = new FileInputStream(file)
      try read(in, factory) finally in.close
    }
  }
  
  private def withConnection[R](process: InputStream => R) : R = {
    val conn = url.openConnection.asInstanceOf[HttpURLConnection]
    try {
      checkResponse(conn)
      val in = conn.getInputStream
      try process(in) finally in.close
    }
    finally conn.disconnect
  }
  
  private def read(stream: InputStream, factory: XMLInputFactory) = {
    new WikiSettingsReader(new XMLEventAnalyzer(factory.createXMLEventReader(stream))).read()
  }
  
  /**
   * @throws HttpRetryException if this language is redirected to another language. 
   * The message of the HttpRetryException is the target language, e.g. "da".
   * @throws IOException if another error occurred
   */
  private def checkResponse(conn : HttpURLConnection) : Unit = {
    conn.setInstanceFollowRedirects(followRedirects)
    val code = conn.getResponseCode
    if (code != HTTP_OK) {
      
      var location = conn.getHeaderField("Location")
      if (location != null) {
        
        // Decoding the whole URL is ok here. The strict equality test below will detect any mixup 
        // caused by decoding. Note: I tried encoding "|" as "%7C" in the original URL above. 
        // api.php works, but in location response headers Wikipedia uses "%257C". That's a bug.
        location = decode(location, "UTF-8")
        val url2 = new URL(location)
        
        if (! followRedirects && (code == HTTP_MOVED_PERM || code == HTTP_MOVED_TEMP) && (url2.getPath == url.getPath && url2.getQuery == url.getQuery)) {
          // Note: we use toSeq because array equality doesn't work
          val parts = url.getHost.split("\\.", -1).toSeq
          val parts2 = url2.getHost.split("\\.", -1).toSeq
          // if everything else matches, the first part of the host name is the target language
          if (parts.tail == parts2.tail) throw new HttpRetryException(parts2.head, code, location)
        }
      }
      
      throw new IOException("URL '"+url+"' replied "+code+" "+conn.getResponseMessage+" - Location: '"+location+"'")
    }
  }
  
}

/**
 * Note: we use linked sets and maps to preserve order. Scala currently has no immutable linked 
 * collections, so we use mutable ones (which should also improve performance). Calling .toMap
 * to make them immutable would destroy the order, so we simply return them, but as an immutable
 * interface. Malicious users could still downcast and mutate.
 */
class WikiSettingsReader(in : XMLEventAnalyzer) {
  
  /**
   * @return namespaces (name -> code), namespace aliases (name -> code), magic words (name -> aliases)
   */
  def read(): WikiSettings = {
    in.document { _ =>
      in.element("api") { _ =>
        in.element("query") { _ =>
          val namespaces = readNamespaces("namespaces", true)
          val aliases = readNamespaces("namespacealiases", false)
          val magicwords = readMagicWords()
          val interwikis = readInterwikiMap()
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
  
  private def readInterwikiMap(): Map[String, String] = {
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
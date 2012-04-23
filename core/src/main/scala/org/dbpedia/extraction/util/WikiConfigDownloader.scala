package org.dbpedia.extraction.util

import java.io.{File,InputStream,FileInputStream,FileOutputStream,IOException}
import java.net.{URL,URLDecoder,HttpRetryException,HttpURLConnection}
import java.net.URLDecoder.decode
import java.net.HttpURLConnection.{HTTP_OK,HTTP_MOVED_PERM,HTTP_MOVED_TEMP}
import javax.xml.stream.XMLInputFactory
import scala.collection.mutable
import org.dbpedia.extraction.util.XMLEventAnalyzer.richStartElement

/**
 * Downloads namespace names, namespace alias names and redirect magic words for a wikipedia 
 * language edition via api.php.
 * 
 * FIXME: This should be used to download the data for just one language and maybe store it in a 
 * text file or simply in memory. (Loading the stuff only takes between .2 and 2 seconds per language.) 
 * Currently this class is used to generate two huge configuration classes for all languages. 
 * That's not good.
 * 
 * FIXME: also use
 * http://en.wikipedia.org/w/api.php?action=query&meta=siteinfo&siprop=interwikimap
 * http://de.wikipedia.org/w/api.php?action=query&meta=siteinfo&siprop=interwikimap
 * which are identical for all languages except for stuff like q => en.wikiquote.org / de.wikiquote.org etc.
 * 
 * TODO: error handling. So far, it didn't seem necessary. api.php seems to work, and this
 * class is so far used with direct human supervision.
 * 
 * @param followRedirects if true, follow HTTP redirects for languages that have been renamed,
 * e.g. dk.wikipedia.org -> da.wikipedia.org. If false, throw a useful exception on redirects. 
 */
class WikiConfigDownloader(language: Language, followRedirects: Boolean) {
  
  val url = new URL(api(language)+"?action=query&format=xml&meta=siteinfo&siprop=namespaces|namespacealiases|magicwords")
  
  private def api(language : Language) : String =
    "http://"+language.wikiCode+"."+(if (language.wikiCode == "commons") "wikimedia" else "wikipedia")+".org/w/api.php"
    
  /**
   * @return namespaces (name -> code), namespace aliases (name -> code), magic words (name -> aliases)
   * @throws HttpRetryException if followRedirects is false and this language is redirected to another 
   * language. The message of the HttpRetryException is the target language.
   * @throws IOException if another error occurs
   */
  def download(factory: XMLInputFactory, file: File = null) : 
  (mutable.Map[String, Int], mutable.Map[String, Int], mutable.Map[String, mutable.Set[String]]) = 
  {
    if (file == null) {
      withConnection { in => read(in, factory) }
    } else {
      if (! file.exists) {
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
    new WikiConfigReader(new XMLEventAnalyzer(factory.createXMLEventReader(stream))).read()
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
        // Of course we should not decode the whole URL, but in this case, it's ok. We're doing
        // a very strict equality test below, so any mixup caused by decoding will be detected.
        val url2 = new URL(decode(location, "UTF-8"))
        location = url2.toString // use decoded form in IOException below
        
        if (! followRedirects && (code == HTTP_MOVED_PERM || code == HTTP_MOVED_TEMP) && (url2.getPath == url.getPath && url2.getQuery == url.getQuery)) {
          // Note: we use toSeq because array equality doesn't work
          val parts = url.getHost.split("\\.", -1).toSeq
          val parts2 = url2.getHost.split("\\.", -1).toSeq
          // if everything else matches, the first part of the host name is the target language
          if (parts.tail == parts2.tail) throw new HttpRetryException(parts2.head, code, location)
        }
      }
      
      throw new IOException("URL '"+url+"' replied "+code+" ("+conn.getResponseMessage+") - Location: '"+location+"'")
    }
  }
  
}

private class WikiConfigReader(in : XMLEventAnalyzer) {
  
  /**
   * @return namespaces (name -> code), namespace aliases (name -> code), magic words (name -> aliases)
   */
  def read() : (mutable.Map[String, Int], mutable.Map[String, Int], mutable.Map[String, mutable.Set[String]]) = 
  {
    in.document { _ =>
      in.element("api") { _ =>
        in.element("query") { _ =>
          val namespaces = readNamespaces("namespaces", true)
          val aliases = readNamespaces("namespacealiases", false)
          val magicwords = readMagicWords()
          (namespaces, aliases, magicwords)
        }
      }
    }
  }
  
  /**
   * @return namespaces or aliases (name -> code)
   */
  private def readNamespaces(tag : String, canonical : Boolean) : mutable.Map[String, Int] = 
  {
    in.element(tag) { _ =>
      // LinkedHashMap to preserve order
      val namespaces = mutable.LinkedHashMap[String, Int]()
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
  private def readMagicWords() : mutable.Map[String, mutable.Set[String]] =
  {
    in.element("magicwords") { _ =>
      // LinkedHashMap to preserve order (although it's probably not important)
      val magicwords = mutable.LinkedHashMap[String, mutable.Set[String]]()
      in.elements("magicword") { mw =>
        // LinkedHashSet to preserve order (although it's probably not important)
        val aliases = magicwords.getOrElseUpdate(mw getAttr "name", mutable.LinkedHashSet[String]())
        in.elements("aliases") { _ =>
          in.elements("alias") { _ =>
            in.text { text => 
              aliases += text
            }
          }
        }
      }
      magicwords
    }
  }
    
}


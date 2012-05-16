package org.dbpedia.extraction.util

import java.io.{File,InputStream,FileInputStream,FileOutputStream,IOException}
import java.net.{URL,HttpRetryException,HttpURLConnection}
import java.net.URLDecoder.decode
import java.net.HttpURLConnection.{HTTP_OK,HTTP_MOVED_PERM,HTTP_MOVED_TEMP}
import javax.xml.stream.XMLInputFactory
import scala.collection.{Map,Set}

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
  
  val url = new URL(language.apiUri+"?"+WikiSettingsReader.query)
  
  /**
   * @return 
   * @throws HttpRetryException if followRedirects is false and this language is redirected 
   * to another language. The message of the HttpRetryException is the target language.
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
    new WikiSettingsReader(factory.createXMLEventReader(stream)).read()
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

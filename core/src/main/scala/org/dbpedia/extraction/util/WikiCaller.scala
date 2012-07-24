package org.dbpedia.extraction.util

import java.io.{File,InputStream,FileInputStream,FileOutputStream,IOException}
import java.net.{URL,HttpRetryException,HttpURLConnection}
import java.net.HttpURLConnection.{HTTP_OK,HTTP_MOVED_PERM,HTTP_MOVED_TEMP}
import scala.collection.{Map,Set}
import org.dbpedia.util.text.uri.UriDecoder.decode

/**
 * @param file Target file. If file exists and overwrite is false, do not call url, 
 * just use current file. If file does not exist or overwrite is true, call url, save
 * result in file, and process file. Must not be null.
 */
class LazyWikiCaller(url: URL, followRedirects: Boolean, file: File, overwrite: Boolean) 
extends WikiCaller(url, followRedirects)
{
  /**
   * @param process Processor for result of call to api.php.
   * @return result of processing
   * @throws HttpRetryException if followRedirects is false and this language is redirected 
   * to another language. The message of the HttpRetryException is the target language.
   * @throws IOException if another error occurs
   */
  override def execute[R](process: InputStream => R): R = {
    if (overwrite || ! file.exists) {
      super.execute { in => 
        val out = new FileOutputStream(file)
        try IOUtils.copy(in, out) finally out.close 
      } 
    }
    val in = new FileInputStream(file)
    try process(in) finally in.close
  }
}
  
/**
 * Calls a Wikipedia URL, handles redirects to a different language version, processes the response.
 * 
 * @param followRedirects if true, follow HTTP redirects for languages that have been renamed
 * and call the new URL, e.g. dk.wikipedia.org -> da.wikipedia.org. If false, throw a useful 
 * exception on redirects.
 */
class WikiCaller(url: URL, followRedirects: Boolean) {
  
  /**
   * @param process Processor for result of call.
   * @return result of processing
   * @throws HttpRetryException if followRedirects is false and this language is redirected 
   * to another language. The message of the HttpRetryException is the target language.
   * @throws IOException if another error occurs
   */
  def execute[R](process: InputStream => R): R = { 
    val conn = url.openConnection.asInstanceOf[HttpURLConnection]
    try {
      checkResponse(conn)
      val in = conn.getInputStream
      try process(in) finally in.close
    }
    finally conn.disconnect
  }
  
  /**
   * @throws HttpRetryException if this language is redirected to another language. 
   * The message of the HttpRetryException is the target language, e.g. "da".
   * @throws IOException if another error occurred
   */
  private def checkResponse(conn: HttpURLConnection): Unit = {
    conn.setInstanceFollowRedirects(followRedirects)
    val code = conn.getResponseCode
    if (code != HTTP_OK) {
      
      var location = conn.getHeaderField("Location")
      if (location != null) {
        
        // Decoding the whole URL is ok here. The strict equality test below will detect any mixup 
        // caused by decoding. Note: I tried encoding "|" as "%7C" in the original URL above. 
        // api.php works, but in location response headers Wikipedia uses "%257C". That's a bug.
        location = decode(location)
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

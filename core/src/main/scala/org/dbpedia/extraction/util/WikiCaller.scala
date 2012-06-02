package org.dbpedia.extraction.util

import java.io.{File,InputStream,FileInputStream,FileOutputStream,IOException}
import java.net.{URL,HttpRetryException,HttpURLConnection}
import java.net.URLDecoder.decode
import java.net.HttpURLConnection.{HTTP_OK,HTTP_MOVED_PERM,HTTP_MOVED_TEMP}
import scala.collection.{Map,Set}

/**
 * Calls a Wikipedia URL, handles redirects to a different language version, processes 
 * the response and optionally saves it in a file.
 * 
 * @param followRedirects if true, follow HTTP redirects for languages that have been renamed
 * and call the new URL, e.g. dk.wikipedia.org -> da.wikipedia.org. If false, throw a useful 
 * exception on redirects.
 */
class WikiCaller(url: URL, followRedirects: Boolean) {
  
  /**
   * @param file Target file. If file exists and overwrite is false, do not call api.php, 
   * just use current file. If file does not exist or overwrite is true, call api.php, save
   * result in file, and process file. If null, just call api.php, don't store result.
   * @param process Processor for result of call to api.php.
   * @return result of processing
   * @throws HttpRetryException if followRedirects is false and this language is redirected 
   * to another language. The message of the HttpRetryException is the target language.
   * @throws IOException if another error occurs
   */
  def download[R](file: File, overwrite: Boolean)(process: InputStream => R): R = 
  {
    if (file == null) {
      withConnection(process)
    } else {
      // TODO: find a good way to move this code to its own class. Problem: it needs to call
      // a method like withConnection. If we rename withConnection to apply, then we could let
      // this class extend ((InputStream => _) => R), which is an awkward type, and it doesn't
      // work: we need a type parameter on withConnection() aka apply(), but we can't do that
      // if this class extends ((InputStream => _) => R)...
      if (overwrite || ! file.exists) {
        withConnection { in => 
          val out = new FileOutputStream(file)
          try IOUtils.copy(in, out) finally out.close 
        } 
      }
      val in = new FileInputStream(file)
      try process(in) finally in.close
    }
  }
  
  def withConnection[R](process: InputStream => R): R = {
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

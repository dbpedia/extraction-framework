package org.dbpedia.extraction.dump.download

import java.io.{File,FileOutputStream,InputStream,OutputStream}
import java.net.{URL,URLConnection,HttpURLConnection}

/**
 * Mixin that does not download a file if it has the same timestamp as the file on the server.
 */
trait LastModified extends Download {
  
  /**
   * Check server timestamp, don't do anything if not necessary.
   */
  protected abstract override def downloadFile(conn: URLConnection, file : File) : Unit = {
    val url = conn.getURL
    println("downloading '"+url+"' to '"+file+"'")
    val lastModified = conn.getLastModified
    if (lastModified != 0 && file.lastModified == lastModified) {
      println("did not download '"+url+"' to '"+file+"' - file is up to date")
    } else {
      super.downloadFile(conn, file)
      if (lastModified != 0) file.setLastModified(lastModified)
    }
  }
}

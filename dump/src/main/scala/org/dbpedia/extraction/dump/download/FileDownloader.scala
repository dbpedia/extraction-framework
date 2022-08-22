package org.dbpedia.extraction.dump.download

import java.io.{File,FileOutputStream,InputStream,OutputStream}
import java.net.{URL,URLConnection,HttpURLConnection}
import org.dbpedia.extraction.util.IOUtils.copy

/**
 * Downloads a single file.
 */
trait FileDownloader extends Downloader
{
  /**
   * Use "index.html" if URL ends with "/"
   */
  def targetName(url : URL) : String = {
    val path = url.getPath
    var part = path.substring(path.lastIndexOf('/') + 1)
    if (part.nonEmpty) part else "index.html"
  }
  
  /**
   * Download file from URL to directory.
   */
  def downloadTo(url : URL, dir : File) : File = {
    val file = new File(dir, targetName(url))
    downloadFile(url, file)
    file
  }
    
  /**
   * Download file from URL to given target file.
   */
  def downloadFile(url : URL, file : File) : Unit = {

    val conn = url.openConnection

    // handle 301 Moved Permanently
    val redirect = conn.getHeaderField("Location") match {
      case null => conn.getHeaderField("location")
      case notNull => notNull
    }
    System.out.println("# New Code")
    if (redirect != null){
      // resolve new location
      downloadFile(new URL(redirect),file)
    } else {
      // resolve current location
      try {
        downloadFile(conn, file)
      } finally conn match {
        // http://dumps.wikimedia.org/ seems to kick us out if we don't disconnect.
        case conn: HttpURLConnection => conn.disconnect
        // But only disconnect if it's a http connection. Can't do this with file:// URLs.
        case _ =>
      }
    }
  }
  
  /**
   * Download file from URL to given target file.
   */
  protected def downloadFile(conn: URLConnection, file : File): Unit = {
    val in = inputStream(conn)

    try
    {
      val out = outputStream(file)
      try
      {
        copy(in, out)
      }
      finally out.close
    }
    finally in.close
  }
  
  /**
   * Get input stream. Mixins may decorate the stream or open a different stream.
   */
  protected def inputStream(conn: URLConnection) : InputStream = {
    conn.getInputStream
  }
  
  /**
   * Get output stream. Mixins may decorate the stream or open a different stream.
   */
  protected def outputStream(file: File) : OutputStream = new FileOutputStream(file)
  
}

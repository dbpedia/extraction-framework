package org.dbpedia.extraction.dump.download

import java.io.{File,FileOutputStream,InputStream,OutputStream}
import java.net.{URL,URLConnection,HttpURLConnection}

/**
 * Downloads a single file.
 */
trait FileDownload extends Download
{
  /**
   * buffer size in bytes
   */
  val buffer : Int
  
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
    try {
      downloadFile(conn, file)
    } finally conn match { case conn : HttpURLConnection => conn.disconnect }
    // http://dumps.wikimedia.org/ seems to kick us out if we don't disconnect.
    // But only disconnect if it's a http connection. Can't do this with file:// URLs.
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
  protected def inputStream(conn: URLConnection) : InputStream = conn.getInputStream
  
  /**
   * Get output stream. Mixins may decorate the stream or open a different stream.
   */
  protected def outputStream(file: File) : OutputStream = new FileOutputStream(file)
  
  /**
   * Do the actual work.
   */
  private def copy(in : InputStream, out : OutputStream) : Unit = {
    val buf = new Array[Byte](buffer)
    while (true)
    {
      val read = in.read(buf)
      if (read == -1)
      {
        out.flush
        return
      }
      out.write(buf, 0, read)
    }
  }
  
}

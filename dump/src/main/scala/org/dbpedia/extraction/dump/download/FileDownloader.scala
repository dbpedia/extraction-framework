package org.dbpedia.extraction.dump.download

import java.io.{File,FileOutputStream,InputStream,OutputStream}
import java.net.{URL,URLConnection,HttpURLConnection}

/**
 * @param decorate optionally decorates the InputStream. Default operation is to get the stream 
 * from the connection.
 */
class FileDownloader( url : URL, file : File, getStream : URLConnection => InputStream = { conn => conn.getInputStream } ) 
{
  if (url == null) throw new NullPointerException("url")
  if (file == null) throw new NullPointerException("file")
  
  /**
   * @return true if file was downloaded, false if it was already up to date, 
   * i.e. existed and had the same timestamp as the URL resource.
   */
  def download : Boolean =
  {
    // Note: we could cast this to HttpURLConnection and call disconnect() in the end,
    // but then we can't use file: URLs, and it doesn't seem to make a difference.
    val conn = url.openConnection
    
    val lastModified = conn.getLastModified
    
    if (lastModified != 0 && file.lastModified == lastModified) return false
    
    download(conn, file)
    
    if (lastModified != 0) file.setLastModified(lastModified)
    
    return true
  }
  
  private def download(conn: URLConnection, file : File): Unit = 
  {
    val in = getStream(conn)
    try
    {
      val out = new FileOutputStream(file)
      try
      {
        copy(in, out)
      }
      finally out.close
    }
    finally in.close
  }
  
  private def copy(in : InputStream, out : OutputStream) : Unit =
  {
    val buf = new Array[Byte](2 << 20) // 1 MB
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

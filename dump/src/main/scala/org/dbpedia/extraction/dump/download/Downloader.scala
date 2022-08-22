package org.dbpedia.extraction.dump.download

import java.io.{File,InputStream,OutputStream}
import java.net.{URL,URLConnection}

/**
 * Downloads a single file.
 */
trait Downloader
{
  /**
   * @return target file name for the given URL. Usually the file name in the URL, but sometimes
   * different, e.g. may be "index.html" for URLs ending with "/", or file name with a different
   * extension if this downloader modifies the file during the download.
   */
  def targetName(url : URL) : String
  
  /**
   * Download file from given URL to given directory.
   * Extension hook, usually called by downloadTo(URL, File)
   * @return downloaded file
   */
  def downloadTo(url : URL, dir : File) : File
    
  /**
   * Extension hook, usually called by downloadTo(URL, File)
   */
  def downloadFile(url : URL, file : File) : Unit
  
  /**
   * Extension hook, usually called by downloadFile(URL, File)
   */
  protected def downloadFile(conn: URLConnection, file : File) : Unit
  
  /**
   * Extension hook, usually called by downloadFile(URLConnection, File)
   */
  protected def inputStream(conn: URLConnection) : InputStream
  
  /**
   * Extension hook, usually called by downloadFile(URLConnection, File)
   */
  protected def outputStream(file: File) : OutputStream
}

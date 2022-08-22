package org.dbpedia.extraction.dump.download

import java.io.{File,InputStream}
import java.net.{URL,URLConnection}
import org.dbpedia.extraction.util.IOUtils

/**
 * Download decorator that renames and unzips zipped files.
 */
trait Unzip extends Downloader {
  
  /**
   * Strip extension if file name of URL indicates zipped file.
   */
  abstract override def targetName(url : URL) : String = {
    unzipped(url)._1
  }
  
  /**
   * Wrap input stream in unzip stream if file name of URL indicates zipped file.
   */
  protected abstract override def inputStream(conn : URLConnection) : InputStream = {
    val stream = super.inputStream(conn)
    val unzipper = unzipped(conn.getURL)._2
    unzipper(stream)
  }
  
  /**
   * @return tuple: file name without zip extension (if it had one), stream unzipper function
   */
  private def unzipped(url : URL) : (String, InputStream => InputStream) = {
    val name = super.targetName(url)
    val dot = name.lastIndexOf('.')
    val ext = name.substring(dot + 1)
    IOUtils.unzippers.get(ext) match {
      case Some(unzipper) => (name.substring(0, dot), unzipper)
      case None => (name, identity)
    }
  }
  
}
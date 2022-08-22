package org.dbpedia.extraction.dump.download

import java.io.{File,IOException}
import java.net.URL

/**
 * Retry download if it fails.
 */
trait Retry extends Downloader {
  
  /**
   * Total number of attempts.
   */
  val retryMax : Int
  
  /**
   * Milliseconds delay between attempts.
   */
  val retryMillis : Int
  
  /**
   * Try several times to download file.
   */
  abstract override def downloadFile(url : URL, file : File) : Unit = {
    var retry = 0
    while (true) {
      try {
        super.downloadFile(url, file)
        return
      } catch {
        case ioex : IOException => {
          retry += 1
          println(retry+" of "+retryMax+" attempts to download '"+url+"' to '"+file+"' failed - "+ioex)
          if (retry >= retryMax) throw ioex
          Thread.sleep(retryMillis)
        }
      }
    }
  }
}
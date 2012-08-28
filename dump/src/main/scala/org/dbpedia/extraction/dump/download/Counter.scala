package org.dbpedia.extraction.dump.download

import java.io.{File,InputStream}
import java.net.URLConnection
import Counter.getContentLength

trait Counter extends Downloader {
  
  /**
   * After how many bytes should download progress be logged?
   */
  val progressStep : Long
  
  val progressPretty : Boolean
  
  // TODO: for more accurate timing, create the ByteLogger earlier (when we connect to the
  // server). Extend trait Downloader, add a connect() method in which we create the ByteLogger
  // (or just remember the start time). Problem: this class must be stateless, so we would
  // have to set a thread-local value in connect() and remove it in inputStream(). Ugly.
  protected abstract override def inputStream(conn: URLConnection): InputStream = {
    val logger = new ByteLogger(getContentLength(conn), progressStep, progressPretty)
    new CountingInputStream(super.inputStream(conn), logger)
  }
}

object Counter {
  
  private def getMethod(cls: Class[_], name: String, default: String) =
    try cls.getMethod(name) catch { case _: NoSuchMethodException => cls.getMethod(default) }
  
  private val contentLengthMethod = getMethod(classOf[URLConnection], "getContentLengthLong", "getContentLength")
  
  /**
   * Uses getContentLengthLong() if available (new in JDK 7). Before JDK 7, only getContentLength() 
   * is available which returns size -1 for files >= 2GB.
   */
  def getContentLength(conn : URLConnection): Long = contentLengthMethod.invoke(conn).asInstanceOf[Long]
}
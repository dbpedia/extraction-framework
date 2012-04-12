package org.dbpedia.extraction.dump.download

import java.io.{File,InputStream}
import java.net.URLConnection

trait Counter extends Download {
  
  /**
   * After how many bytes should download progress be logged?
   */
  val step : Long

  protected abstract override def inputStream(conn: URLConnection): InputStream = {
    val logger = new ByteLogger(getContentLength(conn), step)
    new CountingInputStream(super.inputStream(conn), logger)
  }
  
  private def getContentLength(conn : URLConnection) : Long = getContentLengthMethod match {
    case Some(method) => method.invoke(conn).asInstanceOf[Long]
    case None => conn.getContentLength
  }
  
  // Mew method in JDK 7. In JDK 6, files >= 2GB show size -1
  private lazy val getContentLengthMethod =
  try { Some(classOf[URLConnection].getMethod("getContentLengthLong")) }
  catch { case nmex : NoSuchMethodException => None }
}
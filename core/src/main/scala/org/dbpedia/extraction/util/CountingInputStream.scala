package org.dbpedia.extraction.util

import java.io.InputStream
import java.io.IOException

/**
 * Counts read bytes, sends number to callback function.
 * @param log callback function. first argument is number of bytes read so far,
 * second argument is true on final call (when stream is closed)
 */
class CountingInputStream(in: InputStream, log: (Long, Boolean) => Unit) extends InputStream 
{
  // TODO: also implement mark(), reset(), markSupported()?
  
  private var bytes = 0L
  
  override def read(): Int = {
    val read = in.read
    if (read != -1) count(1)
    read
  }
  
  override def read(buf: Array[Byte]): Int = {
    val read = in.read(buf)
    if (read != -1) count(read)
    read
  }
  
  override def read(buf: Array[Byte], off: Int, len: Int): Int = {
    val read = in.read(buf, off, len)
    if (read != -1) count(read)
    read
  }
  
  override def skip(skip: Long): Long = {
    val read = in.skip(skip)
    count(read)
    read
  }
  
  override def available: Int = {
    in.available
  }
  
  override def close: Unit = {
    count(0L, true)
    in.close
  }
  
  private def count(read: Long, close: Boolean = false) {
    // check for overflow (although it's unlikely with Long)
    if (bytes + read < bytes) throw new IOException("invalid byte count")
    bytes += read
    log(bytes, close)
  }
  
}
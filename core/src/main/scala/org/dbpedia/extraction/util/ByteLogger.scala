package org.dbpedia.extraction.util

/**
 * Logs read bytes. Meant to be used with CountingInputStream.
 * @param length length of input
 * @param step approximate number of bytes read between progress logs
 * @param pretty if true, re-use one line for output, otherwise use a new line for each log. 
 * Pretty printing works when logging to a console, but usuaully not in files.
 * @param nanos System.nanoTime used as start time
 */
class ByteLogger(length: Long, step: Long, pretty: Boolean, nanos: Long = System.nanoTime) extends ((Long, Boolean) => Unit)
{
  require(step > 0, "step must be > 0 but is "+step)
  
  private var next: Long = step
  
  def apply(bytes: Long, close: Boolean): Unit =
  {
    if (close || bytes >= next)
    {
      val millis = (System.nanoTime - nanos) / 1000000
      // TODO: add percentage and ETA
      print("read "+formatBytes(bytes)+" of "+formatBytes(length)+" in "+formatMillis(millis)+" ("+formatRate(bytes, millis)+")")
      if (close || ! pretty) println // new line 
      else print("                    \r") // spaces to overwrite end of previous line, back to start of line
      next = (bytes / step + 1) * step
    }
  }
  
  private def formatBytes(bytes: Long): String =
  {
    if (bytes <  0) "? B"
    else if (bytes < 1024) bytes+" B"
    else if (bytes < 1048576) (bytes / 1024F)+" KB"
    else if (bytes < 1073741824) (bytes / 1048576F)+" MB"
    else (bytes / 1073741824F)+" GB"
  }
  
  private def formatMillis(millis: Long): String =
  {
    val secs = millis / 1000
    if (secs < 60) millis / 1000F+" seconds"
    else if (secs < 3600) zeros(secs / 60)+":"+zeros(secs % 60)+" minutes"
    else zeros(secs / 3600)+":"+zeros(secs % 3600 / 60)+":"+zeros(secs % 60)+" hours"
  }
  
  private def formatRate(bytes: Long, millis: Long): String =
  {
    if (millis == 0) "? B/s"
    else if (bytes / millis < 1024) (bytes / 1.024F / millis)+" KB/s"
    else (bytes / 1048.576F / millis)+" MB/s"
  }
  
  private def zeros(num: Long): String = if (num < 10) "0"+num else num.toString
}

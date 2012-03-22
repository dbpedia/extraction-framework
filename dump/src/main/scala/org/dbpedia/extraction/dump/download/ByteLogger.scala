package org.dbpedia.extraction.dump.download

class ByteLogger( step : Long ) extends ((Long, Boolean) => Unit)
{
  private var nanos = System.nanoTime
  
  private var next : Long = step
  
  var length : Long = 0
  
  def apply( bytes : Long, close : Boolean = false ) : Unit =
  {
    if (close || bytes >= next)
    {
      val millis = (System.nanoTime - nanos) / 1000000
      print("read "+formatBytes(bytes)+" of "+formatBytes(length)+" in "+formatMillis(millis)+" ("+formatRate(bytes, millis)+")                    ") // spaces at end overwrite previous line
      if (close) println // new line 
      else print('\r') // back to start of line
      next = (bytes / step + 1) * step
    }
  }
  
  private def formatBytes( bytes : Long ) : String =
  {
    if (bytes < 1024) bytes+" B"
    else if (bytes < 1048576) (bytes / 1024F)+" KB"
    else if (bytes < 1073741824) (bytes / 1048576F)+" MB"
    else (bytes / 1073741824F)+" GB"
  }
  
  private def formatMillis( millis : Long ) : String =
  {
    if (millis < 60000) millis / 1000F+" seconds"
    else if (millis < 3600000) zeros(millis / 60000)+":"+zeros(millis % 60000 / 1000)+" minutes"
    else zeros(millis / 3600000)+":"+zeros(millis % 3600000 / 60000)+":"+zeros(millis % 60000 / 1000)+" hours"
  }
  
  private def formatRate(bytes : Long, millis : Long) : String =
  {
    if (bytes / millis > 1024) (bytes / 1048.576F / millis)+" MB/s"
    else (bytes / 1.024F / millis)+" KB/s"
  }
  
  private def zeros( num : Long ) : String = if (num < 10) "0"+num else num.toString
}

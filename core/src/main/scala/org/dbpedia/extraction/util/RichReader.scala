package org.dbpedia.extraction.util

import java.io.{Reader,BufferedReader}

object RichReader
{
  implicit def toRichReader(reader: BufferedReader) = new RichReader(reader)
  
  implicit def toRichReader(reader: Reader) = new RichReader(reader)
}


class RichReader(reader: BufferedReader) {
  
  def this(reader: Reader) = this(new BufferedReader(reader))
  
  def foreach[U](proc: String => U): Unit = {
    while (true) {
      val line = reader.readLine()
      if (line == null) return
      proc(line)
    }
  }
}
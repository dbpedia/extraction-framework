package org.dbpedia.extraction.util

import java.io.BufferedReader

object RichBufferedReader
{
  implicit def toRichBufferedReader(reader: BufferedReader) = new RichBufferedReader(reader)
}


class RichBufferedReader(reader: BufferedReader) {
  
  def foreach[U](proc: String => U): Unit = {
    while (true) {
      val line = reader.readLine()
      if (line == null) return
      proc(line)
    }
  }
}
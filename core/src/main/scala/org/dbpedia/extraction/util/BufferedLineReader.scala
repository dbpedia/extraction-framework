package org.dbpedia.extraction.util

import java.io.{BufferedReader, Reader}
import java.util.stream.Stream

/**
  * Created by chile on 07.06.17.
  */
class BufferedLineReader(reader: Reader) extends BufferedReader(reader){
  private var currentLine: String = _
  private var linecount: Int = 0
  private var noMoreLines = false

  override def readLine(): String = {
    val ret = if(currentLine == null) super.readLine() else currentLine
    currentLine = super.readLine()
    if(currentLine == null)
      noMoreLines = true
    linecount = linecount +1
    ret
  }


  override def lines(): Stream[String] = java.util.stream.Stream.concat(java.util.stream.Stream.of("", currentLine), super.lines())

  def peek: String = if(currentLine == null) readLine() else currentLine

  def gettLineCount = linecount

  def hasMoreLines = !noMoreLines
}

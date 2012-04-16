package org.dbpedia.extraction.server.util

class CollectionReader(lines : Iterator[String])
{
  def readCount(tag: String) : Int = {
    readTag(tag).toInt
  }
  
  def readTag(tag: String) : String = {
    if (! lines.hasNext) throw new Exception("missing line starting with '"+tag+"'")
    val line = lines.next
    if (! line.startsWith(tag)) throw new Exception("expected line starting with '"+tag+"', found '"+line+"'")
    line.substring(tag.length)
  }
  
  def readLine : String = {
    if (! lines.hasNext) throw new Exception("missing line")
    lines.next
  }
  
  def readEmpty : Unit = {
    if (! lines.hasNext) throw new Exception("missing empty line")
    val line = lines.next
    if (line.nonEmpty) throw new Exception("expected empty line, found '"+line+"'")
  }
  
  def readEnd : Unit = {
    if (lines.hasNext) throw new Exception("expected end of file, found "+lines.next)
  }
}

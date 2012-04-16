package org.dbpedia.extraction.server.util

import java.io._
import org.dbpedia.extraction.util.RichFile.toRichFile

/**
 * Read ignorelist files, write them back out.
 */
object IgnoreListTest {
  
  def main(args: Array[String]) : Unit = {
    val dir = new File("../extraction_framework/server/src/main/statistics/")
    val langs = List("ar","bn","ca","cs","de","el","en","es","eu","fr","ga","hi","hr","hu","it","ko","nl","pl","pt","ru","sl","tr")
    println(langs.size)
    for (lang <- langs) {
      val file = dir.resolve("ignorelist_"+lang+".txt")
      val ignoreList = new IgnoreList(file)
      ignoreList.save()
      println(file)
    }
  }
  
}

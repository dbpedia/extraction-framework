package org.dbpedia.extraction.server.stats

import java.io._
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.wikiparser.Namespace

/**
 * Read ignorelist files, write them back out.
 */
object IgnoreListTest {
  
  def main(args: Array[String]) : Unit = {
    val dir = new File("src/main/statistics/")
    val langs = Namespace.mappings.keys
    println(langs.size)
    for (lang <- langs) {
      val file = dir.resolve("ignorelist_"+lang.wikiCode+".txt")
      val ignoreList = new IgnoreList(file, () => ())
      ignoreList.save()
      println(file)
    }
  }
  
}

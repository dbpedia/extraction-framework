package org.dbpedia.extraction.server.stats

import java.io._
import org.dbpedia.extraction.util.RichFile.wrapFile
import org.dbpedia.extraction.wikiparser.Namespace

import scala.util.{Success, Failure}

/**
 * Read ignorelist files, write them back out.
 */
object IgnoreListTest {
  
  def main(args: Array[String]) : Unit = {
    val dir = new File("src/main/statistics/")
    val langs = Namespace.mappings.keys
    println(langs.size)
    for (lang <- langs) {
      dir.resolve("ignorelist_"+lang.wikiCode+".txt") match{
        case Success(file) => {
          val ignoreList = new IgnoreList(file, () => ())
          ignoreList.save()
          println(file)
        }
        case Failure(x) => println("Error: could not find file/path: " + dir.toString + "ignorelist_"+lang.wikiCode+".txt")
      }

    }
  }
  
}

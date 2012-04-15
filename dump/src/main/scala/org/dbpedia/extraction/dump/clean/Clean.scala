package org.dbpedia.extraction.dump.clean

import org.dbpedia.extraction.util.{Language,PathFinder}
import java.nio.file.Paths
import org.dbpedia.extraction.util.PathFinder
import scala.collection.JavaConversions.iterableAsScalaIterable
import org.dbpedia.extraction.util.RichPath.toRichPath

object Clean {

  def main(args: Array[String]) {
    
    require(args != null && args.length >= 3, "deletes files from old download directories. need at least two arguments: base dir, number of latest directories per language to leave untouched, following arguments are patterns of files to be deleted from older directories")
    
    val baseDir = Paths.get(args(0))
    val newDirs = args(1).toInt
    
    // Use all remaining args as comma or whitespace separated lists of glob patterns
    val filter = args.drop(2).flatMap(_.split("[,\\s]")).mkString("{",",","}")
    
    var dirs, files = 0
    
    for (language <- Language.Values.values) {
      val finder = new PathFinder(baseDir, language)
      if (finder.wikiDir.exists) {
        for (date <- finder.dates.dropRight(newDirs)) {
          
          val dir = finder.directory(date)
          
          val paths = dir.list(filter)
          try {
            for (path <- paths) {
              path.delete
              println("deleted file ["+path+"]")
              files += 1
            }
          } finally paths.close
          
          if (dir.isEmpty) {
            dir.delete
            println("deleted dir  ["+dir+"]")
            dirs += 1
          }
          
        }
      }
    }
    
    println("deleted "+files+" files and "+dirs+" dirs")
  }
}

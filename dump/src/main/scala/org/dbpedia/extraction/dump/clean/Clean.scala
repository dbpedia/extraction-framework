package org.dbpedia.extraction.dump.clean

import java.nio.file.{Path,Paths}
import scala.collection.JavaConversions.iterableAsScalaIterable
import org.dbpedia.extraction.util.{Language,Finder}
import org.dbpedia.extraction.util.RichPath.toRichPath
import org.dbpedia.extraction.dump.download.Download

object Clean {

  def main(args: Array[String]) {
    
    require(args != null && args.length >= 4, "deletes files from old download directories. need at least four arguments: base dir, marker file to look for, number of latest directories per language to leave untouched, following arguments are patterns of files to be deleted from older directories")
    
    val baseDir = Paths.get(args(0))
    val markerFile = args(1)
    val newDirs = args(2).toInt
    
    // all other args are glob patterns. create one big glob "{pat1,pat2,...}"
    val filter = args.drop(3).flatMap(_.split("[,\\s]")).mkString("{",",","}")
    
    var dirs, files = 0
    
    for (language <- Language.Values.values) {
      val finder = new Finder[Path](baseDir, language)
      if (finder.wikiDir.exists) {
        for (date <- finder.dates(markerFile).dropRight(newDirs)) {
          
          val dir = finder.directory(date)
          
          for (path <- dir.listPaths(filter)) {
            path.delete
            println("deleted file ["+path+"]")
            files += 1
          }
          
          if (dir.isEmpty) {
            dir.delete
            println("deleted dir  ["+dir+"]")
            dirs += 1
          } else {
            println("could not delete dir ["+dir+"] - not empty")
          }
          
        }
      }
    }
    
    println("deleted "+files+" files and "+dirs+" dirs")
  }
}

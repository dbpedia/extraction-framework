package org.dbpedia.extraction.dump.clean

import java.nio.file.{Path,Paths}
import scala.collection.JavaConversions.iterableAsScalaIterable
import org.dbpedia.extraction.util.{Language,Finder}
import org.dbpedia.extraction.util.RichPath.wrapPath
import org.dbpedia.extraction.dump.download.Download

/**
 * This class requires the java.nio.file package, which is available since JDK 7.
 *  
 * If you want to compile and run DBpedia with an earlier JDK version,
 * delete or blank these two files:
 * 
 * core/src/main/scala/org/dbpedia/extraction/util/RichPath.scala
 * dump/src/main/scala/org/dbpedia/extraction/dump/clean/Clean.scala
 * 
 * The launchers 'purge-download' and 'purge-extract' in the dump/ module won't work, 
 * but they are not vitally necessary.
 */
object Clean {
  
  val usage = """
Deletes files from old download directories. Need at least four arguments:
base dir, marker file to look for, number of latest directories per language 
to leave untouched. All following arguments are patterns of files to be deleted 
from older directories. If marker file name is empty or "-", check all directories."""

  def main(args: Array[String]) {
    
    require(args != null && args.length >= 4, usage)
    
    val baseDir = Paths.get(args(0).trim)
    val markerFile = if (args(1).trim.isEmpty || args(1).trim == "-") null else args(1).trim
    val newDirs = args(2).trim.toInt
    
    // all other args are glob patterns. create one big glob "{pat1,pat2,...}"
    val filter = args.drop(3).flatMap(_.split("[,\\s]")).mkString("{",",","}")
    
    var dirs, files = 0
    
    for (language <- Language.Values.values) {
      val finder = new Finder[Path](baseDir, language)
      if (finder.wikiDir.exists) {
        for (date <- finder.dates(markerFile).dropRight(newDirs)) {
          
          val dir = finder.directory(date)
          
          for (path <- dir.list(filter)) {
            path.delete()
            println("deleted file ["+path+"]")
            files += 1
          }
          
          if (! dir.hasFiles) {
            dir.delete()
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

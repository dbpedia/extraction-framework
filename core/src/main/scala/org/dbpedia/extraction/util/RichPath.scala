package org.dbpedia.extraction.util

import java.nio.file.{Path,Files}
import scala.collection.JavaConversions.iterableAsScalaIterable

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
object RichPath {
  implicit def toRichPath(path: Path) = new RichPath(path)
}

import RichPath.toRichPath

class RichPath(path: Path) extends FileLike[Path] {
  
  /**
   * @throws NotDirectoryException if the path is not a directory
   */
  def isEmpty : Boolean = {
    val stream = Files.newDirectoryStream(path)
    try ! stream.iterator.hasNext finally stream.close
  }
  
  def delete = Files.delete(path)
  
  def resolve(name: String) = path.resolve(name)
  
  def exists = Files.exists(path)
  
  def list = list("*")

  def list(glob: String) : List[String] = { 
    val stream = Files.newDirectoryStream(path, glob)
    try stream.toList.map(_.getFileName.toString) finally stream.close
  }
  
  def listPaths : List[Path] = listPaths("*")
    
  def listPaths(glob: String) : List[Path] = { 
    val stream = Files.newDirectoryStream(path, glob)
    try stream.toList finally stream.close
  }
  
}
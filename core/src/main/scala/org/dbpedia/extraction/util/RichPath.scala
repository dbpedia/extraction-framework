package org.dbpedia.extraction.util

import java.nio.file.{Path,Files}
import scala.collection.JavaConversions.iterableAsScalaIterable

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
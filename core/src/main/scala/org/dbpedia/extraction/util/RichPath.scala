package org.dbpedia.extraction.util

import java.nio.file.Path
import java.nio.file.Files

object RichPath {
  implicit def toRichPath(path: Path) = new RichPath(path)
}

class RichPath(path: Path) {
  
  /**
   * @throws NotDirectoryException if the path is not a directory
   */
  def isEmpty : Boolean = {
    val stream = Files.newDirectoryStream(path)
    try ! stream.iterator.hasNext finally stream.close
  }
  
  def delete = Files.delete(path)
  
  def exists = Files.exists(path)
  
  def list(glob: String) = Files.newDirectoryStream(path, glob)
  
  def list = Files.newDirectoryStream(path)
  
}
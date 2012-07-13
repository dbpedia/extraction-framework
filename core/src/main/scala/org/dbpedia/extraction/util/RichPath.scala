package org.dbpedia.extraction.util

import java.io.IOException
import java.nio.file.{Path,Files,SimpleFileVisitor,FileVisitResult}
import java.nio.file.attribute.BasicFileAttributes
import scala.collection.JavaConversions.iterableAsScalaIterable
import RichPath._


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
  
  val DeletionVisitor = new SimpleFileVisitor[Path] {
    
    override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
      Files.delete(file)
      FileVisitResult.CONTINUE
    }
    
    override def postVisitDirectory(dir: Path, e: IOException): FileVisitResult = {
      if (e != null) throw e
      Files.delete(dir)
      FileVisitResult.CONTINUE
    }
  }
  
}

class RichPath(path: Path) extends FileLike[Path] {
  
  /**
   * @throws NotDirectoryException if the path is not a directory
   */
  def hasFiles: Boolean = {
    val stream = Files.newDirectoryStream(path)
    try stream.iterator.hasNext finally stream.close
  }
  
  def delete(recursive: Boolean = false): Unit = {
    if (recursive && Files.isDirectory(path)) Files.walkFileTree(path, DeletionVisitor)
    else Files.delete(path)
  }
  
  def resolve(name: String): Path = path.resolve(name)
  
  def exists: Boolean = Files.exists(path)
  
  // TODO: more efficient type than List?
  def names: List[String] = names("*")

  // TODO: more efficient type than List?
  def names(glob: String): List[String] = list(glob).map(_.getFileName.toString) 
  
  // TODO: more efficient type than List?
  def list: List[Path] = list("*")
    
  // TODO: more efficient type than List?
  def list(glob: String): List[Path] = { 
    val stream = Files.newDirectoryStream(path, glob)
    try stream.toList finally stream.close
  }
  
  def isFile: Boolean = Files.isRegularFile(path)
  
  def isDirectory: Boolean = Files.isDirectory(path)
  
}
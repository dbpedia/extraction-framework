package org.dbpedia.extraction.util

import java.io.{File, IOException, InputStream, OutputStream}
import java.nio.file.{Path,Paths,Files,SimpleFileVisitor,FileVisitResult}
import java.nio.file.StandardOpenOption.{CREATE,APPEND}
import java.nio.file.attribute.BasicFileAttributes
import scala.collection.JavaConversions.iterableAsScalaIterable
import scala.language.implicitConversions
import RichPath._

import scala.util.{Failure, Success, Try}


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
  
  implicit def wrapPath(path: Path) = new RichPath(path)
  
  implicit def toPath(path: String) = Paths.get(path)
  
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
  
  override def toString: String = path.toString
  
  override def name: String = {
    val last = path.getFileName()
    if (last == null) null else last.toString
  }
  
  /**
   */
  override def hasFiles: Boolean = {
    val stream = Files.newDirectoryStream(path)
    try stream.iterator.hasNext finally stream.close
  }
  
  override def delete(recursive: Boolean = false): Unit = {
    if (recursive && Files.isDirectory(path)) Files.walkFileTree(path, DeletionVisitor)
    else Files.delete(path)
  }
  
  override def resolve(name: String): Try[Path] = Try(path.resolve(name))
  
  override def exists: Boolean = Files.exists(path)
  
  // TODO: more efficient type than List?
  override def names: List[String] = names("*")

  // TODO: more efficient type than List?
  def names(glob: String): List[String] = list(glob).map(_.getFileName.toString) 
  
  // TODO: more efficient type than List?
  override def list: List[Path] = list("*")
    
  // TODO: more efficient type than List?
  def list(glob: String): List[Path] = { 
    val stream = Files.newDirectoryStream(path, glob)
    try stream.toList finally stream.close
  }

  override def size: Long = Files.size(path)

  override def isFile: Boolean = Files.isRegularFile(path)
  
  override def isDirectory: Boolean = Files.isDirectory(path)
  
  override def inputStream(): InputStream = Files.newInputStream(path)
  
  override def outputStream(append: Boolean = false): OutputStream = {
    if (append) Files.newOutputStream(path, APPEND, CREATE) // mimic behavior of new FileOutputStream(file, true)
    else Files.newOutputStream(path)
  }

  override def getFile: File = path.toFile
}

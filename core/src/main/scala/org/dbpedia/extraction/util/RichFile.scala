package org.dbpedia.extraction.util

import java.io._
import java.util.regex.Pattern

import org.dbpedia.extraction.util.RichFile._
import org.dbpedia.iri.{URI, UriUtils}

import scala.language.implicitConversions
import scala.util.Try

object RichFile {

  implicit def wrapFile(file: File) = new RichFile(file)
  
  implicit def toFile(file: String) = new File(file)
  
  def filenameFilter(pattern: Pattern): FilenameFilter = {
    if (pattern == null) return null
    new FilenameFilter() {
      def accept(dir: File, name: String): Boolean = pattern.matcher(name).matches
    }
  }
}

/**
 * Defines additional methods on Files, which are missing in the standard library.
 */
class RichFile(file: File) extends FileLike[File] {
  
  override def toString: String = file.toString
    
  override def name: String = {
    val name = file.getName
    // java.io.File.getName returns "" for "/" and ""
    // we emulate java.nio.file.Path.getFileName, which returns null for "/" but "" for ""
    if (name.nonEmpty) name else if (file.isAbsolute) null else name
  }
  
  override def exists: Boolean = file.getAbsoluteFile.exists
  
  // TODO: more efficient type than List?
  override def names: List[String] = names(null) 
  
  // TODO: more efficient type than List?
  def names(pattern: Pattern): List[String] = {
    val list = file.list(filenameFilter(pattern))
    if (list == null) throw new IOException("failed to list files in ["+file+"]")
    list.toList
  } 

  override def size: Long = file.length()

  // TODO: more efficient type than List?
  override def list: List[File] = list(null) 
  
  // TODO: more efficient type than List?
  def list(pattern: Pattern): List[File] = {
    val list = file.listFiles(filenameFilter(pattern))
    if (list == null) throw new IOException("failed to list files in ["+file+"]")
    list.toList
  }
  
  override def resolve(name: String): Try[File] = Try(new File(file, name))
  
  /**
   * Retrieves the relative path in respect to a given base directory.
 *
   * @param child
   * @return path from parent to child. uses forward slashes as separators. may be empty.
   * does not end with a slash.
   * @throws IllegalArgumentException if parent is not a parent directory of child.
   */
  def relativize(child: File): String = {
    // Note: toURI encodes file paths, getPath decodes them
    var path = UriUtils.relativize(URI.create(file.toURI).get, URI.create(child.toURI).get).getPath
    if (path endsWith "/")
      path = path.substring(0, path.length() - 1)
    path
  }

  /**
   * Deletes this file or directory and, if this is a directory and recursive is true,
   * all contained file and sub directories.
 *
   * @throws IOException if the directory or any of its sub directories could not be deleted
   */
  override def delete(recursive: Boolean = false): Unit = {
    if (recursive && file.isDirectory) file.listFiles.foreach(_.delete(true))
    if (! file.delete()) throw new IOException("failed to delete file ["+file+"]")
  }
  
  override def isFile: Boolean = file.isFile
  
  override def isDirectory: Boolean = file.isDirectory
  
  override def hasFiles: Boolean = file.list().length > 0
  
  override def inputStream(): InputStream = new FileInputStream(file)
  
  override def outputStream(append: Boolean = false): OutputStream = new FileOutputStream(file, append)

  override def getFile: File = file
}

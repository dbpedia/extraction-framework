package org.dbpedia.extraction.util

import java.io.{IOException,File,FilenameFilter,InputStream,FileInputStream,OutputStream,FileOutputStream}
import java.util.regex.Pattern
import RichFile._

object RichFile {

  implicit def toRichFile(file: File) = new RichFile(file)
  
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
  
  def exists: Boolean = file.exists
  
  // TODO: more efficient type than List?
  def names: List[String] = names(null) 
  
  // TODO: more efficient type than List?
  def names(pattern: Pattern): List[String] = {
    val list = file.list(filenameFilter(pattern))
    if (list == null) throw new IOException("failed to list files in ["+file+"]")
    list.toList
  } 
  
  // TODO: more efficient type than List?
  def list: List[File] = list(null) 
  
  // TODO: more efficient type than List?
  def list(pattern: Pattern): List[File] = {
    val list = file.listFiles(filenameFilter(pattern))
    if (list == null) throw new IOException("failed to list files in ["+file+"]")
    list.toList
  }
  
  def resolve(name: String): File = new File(file, name)
  
  /**
   * Retrieves the relative path in respect to a given base directory.
   * @param child
   * @return path from parent to child. uses forward slashes as separators. may be empty.
   * does not end with a slash.
   * @throws IllegalArgumentException if parent is not a parent directory of child.
   */
  def relativize(child: File): String =
  {
    // Note: toURI encodes file paths, getPath decodes them
    var path = UriUtils.relativize(file.toURI, child.toURI).getPath
    if (path endsWith "/") path = path.substring(0, path.length() - 1)
    return path
  }

  /**
   * Deletes this file or directory and, if this is a directory and recursive is true,
   * all contained file and sub directories.
   * @throws IOException if the directory or any of its sub directories could not be deleted
   */
  def delete(recursive: Boolean = false): Unit = {
    if (recursive && file.isDirectory) file.listFiles.foreach(_.delete(true))
    if (! file.delete()) throw new IOException("failed to delete file ["+file+"]")
  }
  
  def isFile: Boolean = file.isFile
  
  def isDirectory: Boolean = file.isDirectory
  
  def hasFiles: Boolean = file.list().length > 0
  
  def newInputStream(): InputStream = new FileInputStream(file)
  
  def newOutputStream(append: Boolean = false): OutputStream = new FileOutputStream(file, append)
  
}
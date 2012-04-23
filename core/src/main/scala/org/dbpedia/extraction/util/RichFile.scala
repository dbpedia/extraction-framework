package org.dbpedia.extraction.util

import java.io.{IOException, File}

object RichFile {
    implicit def toRichFile(file : File) = new RichFile(file)
}

import RichFile.toRichFile

/**
 * Defines additional methods on Files, which are missing in the standard library.
 */
class RichFile(file : File) extends FileLike[File] {
  
  def exists: Boolean = file.exists
  
  def list: List[String] = {
    val list = file.list
    if (list == null) throw new IOException("failed to list files in ["+file+"]")
    list.toList
  }
  
  def resolve(name: String) = new File(file, name)
  
  /**
   * Retrieves the relative path in respect to a given base directory.
   * @param child
   * @return path from parent to child. uses forward slashes as separators. may be empty.
   * does not end with a slash.
   * @throws IllegalArgumentException if parent is not a parent directory of child.
   */
  def relativize(child : File) : String =
  {
    // Note: toURI encodes file paths, getPath decodes them
    var path = UriUtils.relativize(file.toURI, child.toURI).getPath
    if (path endsWith "/") path = path.substring(0, path.length() - 1)
    return path
  }

  /**
   * Deletes this directory and all sub directories.
   * @throws IOException if the directory or any of its sub directories could not be deleted
   */
  def deleteRecursive() : Unit =
  {
    if(file.isDirectory) file.listFiles.foreach(_.deleteRecursive)
    if(!file.delete) throw new IOException("Could not delete file "+file)
  }
}
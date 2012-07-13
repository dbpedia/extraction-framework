package org.dbpedia.extraction.util

/**
 * Allows common handling of java.io.File and java.nio.file.Path
 */
abstract class FileLike[T <% FileLike[T]] {
  
  def resolve(name: String) : T
  
  def names: List[String]
  
  def list: List[T]
  
  def exists: Boolean
  
  def delete(recursive: Boolean = false): Unit

  def isFile: Boolean

  def isDirectory: Boolean
  
  def hasFiles: Boolean
}
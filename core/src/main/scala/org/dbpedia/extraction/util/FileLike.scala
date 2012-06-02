package org.dbpedia.extraction.util

/**
 * Allows common handling of java.io.File and java.nio.file.Path
 */
abstract class FileLike[T <% FileLike[T]] {
  
  def resolve(name: String) : T
  
  def list: List[String]
  
  def exists: Boolean

}
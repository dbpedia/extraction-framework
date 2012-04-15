package org.dbpedia.extraction.util

abstract class FileLike[T <% FileLike[T]] {
  
  def resolve(name: String) : T
  
  def list: List[String]
  
  def exists: Boolean

}
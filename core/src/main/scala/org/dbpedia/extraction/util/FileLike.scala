package org.dbpedia.extraction.util

import java.io.{InputStream,OutputStream}

/**
 * Allows common handling of java.io.File and java.nio.file.Path
 */
abstract class FileLike[T <% FileLike[T]] {
  
  def resolve(name: String): T
  
  def names: List[String]
  
  def list: List[T]
  
  def exists: Boolean
  
  def delete(recursive: Boolean = false): Unit

  def isFile: Boolean

  def isDirectory: Boolean
  
  def hasFiles: Boolean
  
  def inputStream(): InputStream
  
  def outputStream(append: Boolean = false): OutputStream
  
}
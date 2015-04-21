package org.dbpedia.extraction.util

import java.io.{InputStream,OutputStream}

/**
 * Allows common handling of java.io.File and java.nio.file.Path
 */
abstract class FileLike[T] {
  
  /**
   * @return full path
   */
  def toString: String
  
  /**
   * @return file name, or null if file path has no parts
   */
  def name: String
  
  def resolve(name: String): T
  
  def names: List[String]
  
  def list: List[T]
  
  def exists: Boolean
  
  @throws[java.io.IOException]("if file does not exist or cannot be deleted")
  def delete(recursive: Boolean = false): Unit

  def size(): Long

  def isFile: Boolean

  def isDirectory: Boolean
  
  def hasFiles: Boolean
  
  def inputStream(): InputStream
  
  def outputStream(append: Boolean = false): OutputStream
  
}

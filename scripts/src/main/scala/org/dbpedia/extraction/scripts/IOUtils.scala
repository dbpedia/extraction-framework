package org.dbpedia.extraction.scripts

import org.apache.commons.compress.compressors.bzip2.{BZip2CompressorInputStream,BZip2CompressorOutputStream}
import java.util.zip.{GZIPInputStream,GZIPOutputStream}
import java.io.{File,InputStream,OutputStream,Writer,FileInputStream,FileOutputStream,OutputStreamWriter,InputStreamReader}
import scala.io.Codec
import org.dbpedia.extraction.util.RichReader.wrapReader
import org.dbpedia.extraction.util.FileLike
import java.nio.charset.Charset

/**
 * TODO: move this class to core, but modify the code such that there are no run-time dependencies
 * on commons-compress. Users should be able to use .gz files without having commons-compress on
 * the classpath. Even better, look for several different bzip2 implementations on the classpath...
 */
object IOUtils {
  
  def printerrln(x: Any) = Console.err.println(x)

  val zippers = Map[String, OutputStream => OutputStream] (
    "gz" -> { new GZIPOutputStream(_) }, 
    "bz2" -> { new BZip2CompressorOutputStream(_) } 
  )
  
  val unzippers = Map[String, InputStream => InputStream] (
    "gz" -> { new GZIPInputStream(_) }, 
    "bz2" -> { new BZip2CompressorInputStream(_, true) } 
  )
  
  def open[T](file: FileLike[_], opener: FileLike[_] => T, wrappers: Map[String, T => T]): T = {
    val name = file.name
    val suffix = name.substring(name.lastIndexOf('.') + 1)
    wrappers.getOrElse(suffix, identity[T] _)(opener(file)) 
  }
  
  def output(file: FileLike[_]) = open(file, _.outputStream(), zippers)
  
  def input(file: FileLike[_]) = open(file, _.inputStream(), unzippers)
  
  def write(file: FileLike[_], charset: Charset = Codec.UTF8) = new OutputStreamWriter(output(file), charset)
  
  def read(file: FileLike[_], charset: Charset = Codec.UTF8) = new InputStreamReader(input(file), charset)
  
  def readLines[U](file: FileLike[_])(proc: String => U): Unit = {
    val reader = read(file)
    try {
      for (line <- reader) {
        proc(line)
      }
    }
    finally reader.close()
  }
}
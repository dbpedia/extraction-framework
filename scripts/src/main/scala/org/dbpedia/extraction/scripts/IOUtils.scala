package org.dbpedia.extraction.scripts

import org.apache.commons.compress.compressors.bzip2.{BZip2CompressorInputStream,BZip2CompressorOutputStream}
import java.util.zip.{GZIPInputStream,GZIPOutputStream}
import java.io.{File,InputStream,OutputStream,Writer,FileInputStream,FileOutputStream,OutputStreamWriter,InputStreamReader}
import scala.io.Codec
import org.dbpedia.extraction.util.RichReader.toRichReader

/**
 * TODO: move this class to core, but modify the code such that there are no run-time dependencies
 * on commons-compress. Users should be able to use .gz files without having commons-compress on
 * the classpath. Even better, look for several different bzip2 implementations on the classpath...
 */
object IOUtils {

  val zippers = Map[String, OutputStream => OutputStream] (
    "gz" -> { new GZIPOutputStream(_) }, 
    "bz2" -> { new BZip2CompressorOutputStream(_) } 
  )
  
  val unzippers = Map[String, InputStream => InputStream] (
    "gz" -> { new GZIPInputStream(_) }, 
    "bz2" -> { new BZip2CompressorInputStream(_) } 
  )
  
  def open[T](file: File, opener: File => T, wrappers: Map[String, T => T]): T = {
    val name = file.getName
    val suffix = name.substring(name.lastIndexOf('.') + 1)
    wrappers.getOrElse(suffix, identity[T] _)(opener(file)) 
  }
  
  def output(file: File) = open(file, new FileOutputStream(_), zippers)
  
  def input(file: File) = open(file, new FileInputStream(_), unzippers)
  
  def write(file: File) = new OutputStreamWriter(output(file), Codec.UTF8)
  
  def read(file: File) = new InputStreamReader(input(file), Codec.UTF8)
  
  def readLines[U](file: File)(proc: String => U): Unit = {
    val reader = read(file)
    try {
      for (line <- reader) {
        proc(line)
      }
    }
    finally reader.close()
  }
}
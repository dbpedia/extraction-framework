package org.dbpedia.extraction.util

import org.apache.commons.compress.compressors.bzip2.{BZip2CompressorInputStream,BZip2CompressorOutputStream}
import java.util.zip.{GZIPInputStream,GZIPOutputStream}
import java.io._
import scala.io.Codec
import org.dbpedia.extraction.util.RichReader.wrapReader
import java.nio.charset.Charset

/**
 * TODO: modify the bzip code such that there are no run-time dependencies on commons-compress.
 * Users should be able to use .gz files without having commons-compress on the classpath.
 * Even better, look for several different bzip2 implementations on the classpath...
 */
object IOUtils {

  /**
   * Map from file suffix (without "." dot) to output stream wrapper
   */
  val zippers = Map[String, OutputStream => OutputStream] (
    "gz" -> { new GZIPOutputStream(_) }, 
    "bz2" -> { new BZip2CompressorOutputStream(_) } 
  )
  
  /**
   * Map from file suffix (without "." dot) to input stream wrapper
   */
  val unzippers = Map[String, InputStream => InputStream] (
    "gz" -> { new GZIPInputStream(_) }, 
    "bz2" -> { new BZip2CompressorInputStream(_, true) } 
  )
  
  /**
   * use opener on file, wrap in un/zipper stream if necessary
   */
  private def open[T](file: FileLike[_], opener: FileLike[_] => T, wrappers: Map[String, T => T]): T = {
    val name = file.name
    val suffix = name.substring(name.lastIndexOf('.') + 1)
    wrappers.getOrElse(suffix, identity[T] _)(opener(file)) 
  }
  
  /**
   * open output stream, wrap in zipper stream if file suffix indicates compressed file.
   */
  def outputStream(file: FileLike[_], append: Boolean = false): OutputStream =
    open(file, _.outputStream(append), zippers)
  
  /**
   * open input stream, wrap in unzipper stream if file suffix indicates compressed file.
   */
  def inputStream(file: FileLike[_]): InputStream =
    open(file, _.inputStream(), unzippers)
  
  /**
   * open output stream, wrap in zipper stream if file suffix indicates compressed file,
   * wrap in writer.
   */
  def writer(file: FileLike[_], append: Boolean = false, charset: Charset = Codec.UTF8.charSet): Writer =
    new OutputStreamWriter(outputStream(file, append), charset)
  
  /**
   * open input stream, wrap in unzipper stream if file suffix indicates compressed file,
   * wrap in reader.
   */
  def reader(file: FileLike[_], charset: Charset = Codec.UTF8.charSet): Reader =
    new InputStreamReader(inputStream(file), charset)
  
  /**
   * open input stream, wrap in unzipper stream if file suffix indicates compressed file,
   * wrap in reader, wrap in buffered reader, process all lines. The last value passed to
   * proc will be null.
   */
  def readLines(file: FileLike[_], charset: Charset = Codec.UTF8.charSet)(proc: String => Unit): Unit = {
    val reader = this.reader(file)
    try {
      for (line <- reader) {
          proc(line)
      }
    }
    finally reader.close()
  }

  /**
    * open input stream, wrap in unzipper stream if file suffix indicates compressed file,
    * wrap in reader, wrap in buffered reader, process all lines. The last value passed to
    * proc will be null.
    */
  def readLines(stream: InputStream, charset: Charset)(proc: String => Unit): Unit = {
    val reader = new InputStreamReader(stream, charset)
    try {
      for (line <- reader) {
          proc(line)
      }
    }
    finally reader.close()
  }

  /**
   * Copy all bytes from input to output. Don't close any stream.
   */
  def copy(in: InputStream, out: OutputStream) : Unit = {
    val buf = new Array[Byte](1 << 20) // 1 MB
    while (true)
    {
      val read = in.read(buf)
      if (read == -1)
      {
        out.flush
        return
      }
      out.write(buf, 0, read)
    }
  }

}

package org.dbpedia.extraction.sources

import java.io.{BufferedReader, File}
import java.util.function.Supplier

import org.dbpedia.extraction.util.IOUtils
import org.dbpedia.extraction.util.RichFile.wrapFile

class SparkSource(file: File, delimiter: String) extends Supplier[String]{
  private val br = new BufferedReader(IOUtils.reader(file))
  private val start = s"<$delimiter>"
  private val end = s"</$delimiter>"
  private val iterator : Iterator[String] = Iterator continually br.readLine takeWhile (_ != null)
  //skip lines until first page starts
  private val skipped : Int = iterator.takeWhile(_.trim != start).length

  private def getNextContent(): String = {
    iterator.takeWhile(_.trim != end) mkString
  }
  /**
    * get the next XML Page from file
    * @return
    */
  override def get(): String = {
    iterator.takeWhile(_.trim != end) mkString
  }
}

object SparkSource {
  def main(args: Array[String]): Unit = {
    val src = new SparkSource(new File("/data/2016-10/dewiki/20161020/dewiki-20161020-pages-articles.xml.bz2"),"page")
    println(src.get())
  }
}

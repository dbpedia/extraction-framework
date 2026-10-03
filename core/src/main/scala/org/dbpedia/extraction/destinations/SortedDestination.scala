package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.util.FileLike
import sys.process._
import java.util.logging.Logger
import java.io.File

/**
 * Created by nilesh on 22/4/15.
 */
class SortedDestination(destination: Destination, originalFile: FileLike[_], sortedFile: FileLike[_])
  extends WrapperDestination(destination) {
  import SortedDestination._
  private val logger = Logger.getLogger(classOf[SortedDestination].getName)

  override def close(): Unit = {
    super.close()
    logger.info(s"Sorting $originalFile into $sortedFile...")
    sort(originalFile, sortedFile)
  }
}

object SortedDestination {
  def sort(originalFile: FileLike[_], sortedFile: FileLike[_]): Unit = {
    val suffix = originalFile.name.substring(originalFile.name.lastIndexOf('.') + 1)
    suffix match {
      case "bz2" => (s"bzcat $originalFile" #| "sort" #| "bzip2" #> new File(sortedFile.toString)).!!
      case "gz" => (s"zcat $originalFile" #| "sort" #| "gzip" #> new File(sortedFile.toString)).!!
      case _ => (s"cat $originalFile" #| "sort" #> new File(sortedFile.toString)).!!
    }
  }
}
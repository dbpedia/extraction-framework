package org.dbpedia.extraction.mappings

import java.io.{File, FileWriter}
import java.util.concurrent.atomic.AtomicLong

import org.dbpedia.extraction.util.{StringUtils, Language}
import org.dbpedia.extraction.wikiparser.WikiTitle

import scala.collection.mutable

/**
  * Created by Chile on 11/3/2016.
  */
trait ExtractionRecorder {

  private var failedPages = Map[Language, scala.collection.mutable.Map[(Long, WikiTitle), Throwable]]()
  private var successfulPages = Map[Language, scala.collection.mutable.Map[Long, WikiTitle]]()

  private var logWriter: FileWriter = null
  private val startTime = new AtomicLong()

  /**
    * A map for failed pages, which could be used for a better way to record extraction fails than just a simple console output.
    *
    * @return the failed pages (id, title) for every Language
    */
  def listFailedPages: Map[Language, mutable.Map[(Long, WikiTitle), Throwable]] = failedPages

  /**
    * define the log file destination
    *
    * @param logFile the target file
    * @param preamble the optional first line of the log file
    */
  def setLogFile(logFile: File, preamble: String = null): Unit ={
    logWriter = new FileWriter(logFile)
    if(preamble != null)
      logWriter.append("# " + preamble + "\n")
  }

  def successfulPageCount: Int = successfulPages.size

  def failedPageCount: Int = failedPages.size

  /**
    * adds a new fail record for a wikipage which failed to extract; Optional: write fail to log file (if this has been set before)
    *
    * @param id - page id
    * @param title - WikiTitle of page
    * @param exception  - the Throwable responsible for the fail
    */
  def recordFailedPage(id: Long, title: WikiTitle, exception: Throwable): Unit = synchronized{
    failedPages.get(title.language) match{
      case Some(map) => map += ((id,title) -> exception)
      case None =>  failedPages += title.language -> mutable.Map[(Long, WikiTitle), Throwable]((id, title) -> exception)
    }
    if(logWriter != null) {
      logWriter.append("page " + id + ": " + title.encoded + ": " + exception.getMessage + "\n")
      for (ste <- exception.getStackTrace)
        logWriter.write(ste.toString + "\n")
    }
    System.err.println(title.language.wikiCode + ": extraction failed for page " + id + ": " + title.encoded)
  }

  /**
    * adds a record of a successfully extracted page
    * @param id - page id
    * @param title - page title
    * @param logSuccessfulPage - indicates whether the event of a successful extraction shall be included in the log file (default = false)
    */
  def recordExtractedPage(id: Long, title: WikiTitle, logSuccessfulPage:Boolean = false): Unit = synchronized {
    successfulPages.get(title.language) match{
      case Some(map) => map += (id -> title)
      case None =>  successfulPages += title.language -> mutable.Map[Long, WikiTitle](id -> title)
    }
    if(logWriter != null && logSuccessfulPage) {
      logWriter.append("page " + id + ": " + title.encoded + " extracted\n")
    }
    if(successfulPageCount % 2000 == 0){
      val time = System.currentTimeMillis - startTime.get
      System.out.println(title.language.wikiCode +": extracted "+successfulPageCount+" pages in "+StringUtils.prettyMillis(time)+" (per page: " + (time.toDouble / successfulPageCount) + " ms; failed pages: "+ failedPageCount +").")
    }
  }

  def initialzeRecorder(): Unit ={
    startTime.set(System.currentTimeMillis)
  }

  def finalizeRecorder(): Unit ={
    if(logWriter != null)
      logWriter.close()
    logWriter = null
  }
}

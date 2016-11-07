package org.dbpedia.extraction.mappings

import java.io.{File, FileWriter}
import java.util.concurrent.atomic.{AtomicInteger, AtomicLong}

import org.dbpedia.extraction.util.{Language, StringUtils}
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle}

import scala.collection.mutable

/**
  * Created by Chile on 11/3/2016.
  */
class ExtractionRecorder(logFile: FileWriter = null, preamble: String = null) {

  private var failedPageMap = Map[Language, scala.collection.mutable.Map[(Long, PageNode), Throwable]]()
  private var successfulPagesMap = Map[Language, scala.collection.mutable.Map[Long, WikiTitle]]()

  private var logWriter: FileWriter = logFile
  private val startTime = new AtomicLong()
  private var successfulPageCount = Map[Language,AtomicInteger]()

  private var label: String = ""

  /**
    * A map for failed pages, which could be used for a better way to record extraction fails than just a simple console output.
    *
    * @return the failed pages (id, title) for every Language
    */
  def listFailedPages: Map[Language, mutable.Map[(Long, PageNode), Throwable]] = failedPageMap

  /**
    * define the log file destination
    *
    * @param logFile the target file
    * @param preamble the optional first line of the log file
    */
  private def setLogFile(logFile: File, preamble: String = null): Unit ={
    logWriter = new FileWriter(logFile)
    if(preamble != null && preamble.length > 0)
      logWriter.append("# " + preamble + "\n")
  }

  def successfulPages(lang: Language): Int = successfulPageCount.get(lang) match{
    case Some(m) => m.get()
    case None => 0
  }

  def failedPages(lang: Language): Int = failedPageMap.get(lang) match{
    case Some(m) => m.size
    case None => 0
  }

  /**
    * prints a message of a RecordEntry if available and
    * assesses a RecordEntry for the existence of a Throwable and forwards
    * the record to the suitable method for a failed or successful extraction
    *
    * @param records - the RecordEntries for a WikiPage
    */
  def record(records: RecordEntry*): Unit = {
    for(record <- records) {
      if (record.errorMsg != null)
        printLabeledLine(record.errorMsg, record.page.title.language, Seq(PrinterDestination.err, PrinterDestination.file))
      Option(record.error) match {
        case Some(ex) => recordFailedPage(record.page.id, record.page, ex)
        case None => recordExtractedPage(record.page.id, record.page.title, record.logSuccessfulPage)
      }
    }
  }

  /**
    * adds a new fail record for a wikipage which failed to extract; Optional: write fail to log file (if this has been set before)
    *
    * @param id - page id
    * @param node - PageNode of page
    * @param exception  - the Throwable responsible for the fail
    */
  def recordFailedPage(id: Long, node: PageNode, exception: Throwable): Unit = synchronized{
    failedPageMap.get(node.title.language) match{
      case Some(map) => map += ((id,node) -> exception)
      case None =>  failedPageMap += node.title.language -> mutable.Map[(Long, PageNode), Throwable]((id, node) -> exception)
    }
    printLabeledLine("extraction failed for page " + id + ": " + node.title.encoded + ": " + exception.getMessage(), node.title.language, Seq(PrinterDestination.file))
    for (ste <- exception.getStackTrace)
      printLabeledLine("\t" + ste.toString + "\n", node.title.language, Seq(PrinterDestination.file), noLabel = true)
    printLabeledLine("extraction failed for page " + id + ": " + node.title.encoded, node.title.language, Seq(PrinterDestination.err, PrinterDestination.file))
  }

  /**
    * adds a record of a successfully extracted page
    *
    * @param id - page id
    * @param title - page title
    * @param logSuccessfulPage - indicates whether the event of a successful extraction shall be included in the log file (default = false)
    */
  def recordExtractedPage(id: Long, title: WikiTitle, logSuccessfulPage:Boolean = false): Unit = synchronized {
    if(logSuccessfulPage) {
      successfulPagesMap.get(title.language) match {
        case Some(map) => map += (id -> title)
        case None => successfulPagesMap += title.language -> mutable.Map[Long, WikiTitle](id -> title)
      }
      printLabeledLine("page " + id + ": " + title.encoded + " extracted", title.language, Seq(PrinterDestination.file))
    }
    val sfc = successfulPageCount.get(title.language) match {
      case Some(ai) => ai.incrementAndGet()
      case None => {
        successfulPageCount += (title.language -> new AtomicInteger(1))
        1
      }
    }
    if(sfc % 2000 == 0)
      printLabeledLine("extracted {page} pages; {mspp} per page; {fail} failed pages", title.language)
  }

    /**
    * print a line to std out, err or the log file
      *
      * @param line - the line in question
    * @param lang - langauge of current page
    * @param print - enum values for printer destinations (err, out, file - null mean all of them)
    * @param noLabel - the initial label (lang: time passed) is omitted
    */
  def printLabeledLine(line:String, lang: Language, print: Seq[PrinterDestination.Value] = null, noLabel: Boolean = false): Unit ={
    val printOptions = if(print == null) Seq(PrinterDestination.out, PrinterDestination.file) else print
    val pages = successfulPages(lang)
    val time = System.currentTimeMillis - startTime.get
    val replacedLine = ((if(noLabel) "" else label  + ": extraction at {time}; ") + line)
      .replaceAllLiterally("{time}", StringUtils.prettyMillis(time))
      .replaceAllLiterally("{mspp}", (time.toDouble / pages).toString + " ms")
      .replaceAllLiterally("{page}", pages.toString)
      .replaceAllLiterally("{fail}", failedPages(lang).toString)
    for(pr <-printOptions)
      pr match{
        case PrinterDestination.err => System.err.println(replacedLine)
        case PrinterDestination.out => System.out.println(replacedLine)
        case PrinterDestination.file if logWriter != null => logWriter.append(replacedLine + "\n")
      }
  }

  def initialzeRecorder(label: String): Unit ={
    startTime.set(System.currentTimeMillis)
    this.label = label
  }

  def finalizeRecorder(): Unit ={
    if(logWriter != null)
      logWriter.close()
    logWriter = null
  }

  def resetFailedPages(lang: Language) = failedPageMap.get(lang) match{
    case Some(m) => {
      m.clear()
      successfulPageCount.get(lang).get.set(0)
    }
    case None =>
  }

  object PrinterDestination extends Enumeration {
    val out, err, file = Value
  }
}

/**
  * This class provides the necessary attributes to record either a successful or failed extraction
  *
  * @param page
  * @param errorMsg
  * @param error
  * @param logSuccessfulPage
  */
class RecordEntry(val page: PageNode, val errorMsg: String= null, val error:Throwable = null, val logSuccessfulPage:Boolean = false)
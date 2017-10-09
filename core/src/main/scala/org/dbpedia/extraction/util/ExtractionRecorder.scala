package org.dbpedia.extraction.util

import java.io.{ByteArrayOutputStream, Writer}
import java.net.SocketTimeoutException
import java.nio.charset.Charset
import java.text.DecimalFormat
import java.util.concurrent.atomic.AtomicLong

import org.apache.jena.atlas.json.{JSON, JsonArray, JsonObject}
import org.dbpedia.extraction.config.provenance.Dataset
import org.dbpedia.extraction.mappings.ExtractionMonitor
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.config.Config.SlackCredentials
import org.dbpedia.extraction.wikiparser.{PageNode, WikiPage, WikiTitle}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scalaj.http.Http

/**
  * Created by Chile on 11/3/2016.
  */
class ExtractionRecorder[T](
                             val logWriter: Writer = null,
                             val reportInterval: Int = 100000,
                             val preamble: String = null,
                             val slackCredantials: SlackCredentials = null,
                             val datasets: ListBuffer[Dataset] = ListBuffer[Dataset](),
                             val language: Language = Language.English,
                             val monitor: ExtractionMonitor = null
   ) {

  def this(er: ExtractionRecorder[T]) = this(er.logWriter, er.reportInterval, er.preamble, er.slackCredantials)

  private var failedPageMap = Map[Language, scala.collection.mutable.Map[(String, T), Throwable]]()
  private var successfulPagesMap = Map[Language, scala.collection.mutable.Map[Long, WikiTitle]]()

  private val startTime = new AtomicLong()
  private var successfulPageCount = Map[Language,AtomicLong]()
  private var successfulTripleCount = Map[Dataset, AtomicLong]()

  private var defaultLang: Language = Language.English

  private val decForm = new DecimalFormat("#.##")

  private var slackIncreaseExceptionThreshold = 1

  private var task: String = "transformation"
  private var initialized = false

  private var writerOpen = if(logWriter == null) false else true


  /**
    * A map for failed pages, which could be used for a better way to record extraction fails than just a simple console output.
    *
    * @return the failed pages (id, title) for every Language
    */
  def listFailedPages: Map[Language, mutable.Map[(String, T), Throwable]] = failedPageMap

  /**
    * successful page count
    *
    * @param lang - for this language
    * @return
    */
  def successfulPages(lang: Language): Long = successfulPageCount.get(lang) match{
    case Some(m) => m.get()
    case None => 0
  }

  def successfulTriples(dataset : Dataset): Long = successfulTripleCount.get(dataset) match {
    case Some(m) => m.get()
    case None => 0
  }

  /**
    * get successful page count after increasing it by one
    *
    * @param lang - for this language
    * @return
    */
  def increaseAndGetSuccessfulPages(lang: Language): Long ={
    successfulPageCount.get(lang) match {
      case Some(ai) => ai.incrementAndGet()
      case None =>
        successfulPageCount += (lang -> new AtomicLong(1))
        1
    }
  }

  def increaseAndGetSuccessfulTriples(dataset: Dataset) : Long = {
    successfulTripleCount.get(dataset) match {
      case Some(ai) => ai.incrementAndGet()
      case None =>
        successfulTripleCount += (dataset -> new AtomicLong(1))
        1
    }
  }

  /**
    * number of failed pages
    *
    * @param lang - for this language
    * @return
    */
  def failedPages(lang: Language): Long = failedPageMap.get(lang) match{
    case Some(m) => m.size
    case None => 0
  }

  /**
    * the current accumulated page number
    *
    * @param lang - for this language
    * @return
    */
  def runningPageNumber(lang:Language): Long = successfulPages(lang) + failedPages(lang)

  /**
    * prints a message of a RecordEntry if available and
    * assesses a RecordEntry for the existence of a Throwable and forwards
    * the record to the suitable method for a failed or successful extraction
    *
    * @param records - the RecordEntries for a WikiPage
    */
  def record(records: RecordEntry[T]*): Unit = {
    for(record <- records) {
      //val count = increaseAndGetSuccessfulPages(record.language)
      record.page match{
        case page: WikiPage =>
          if (record.errorMsg != null)
            printLabeledLine(record.errorMsg, record.severity, page.title.language, Seq(PrinterDestination.err, PrinterDestination.file))
          Option(record.error) match {
            case Some(ex) => failedRecord(record.identifier, record.page, ex, record.language)
            case None => recordExtractedPage(page.id, page.title, record.logSuccessfulPage)
          }
        case quad: Quad =>
          Option(record.error) match {
            case Some(ex) => failedRecord(record.identifier, record.page, ex, record.language)
            case None => recordQuad(quad, record.severity, record.language)
          }
        case _  =>
          val msg = Option(record.errorMsg) match{
            case Some(m) => printLabeledLine(m, record.severity, record.language)
            case None =>
              if(record.error != null) failedRecord(null, record.page, record.error, record.language)
              else recordGenericPage(record.language, record.page.toString)
          }
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
  def failedRecord(id: String, node: T, exception: Throwable, language:Language = null): Unit = synchronized{
    val lang = if(language != null) language else defaultLang
    val tag = node match{
      case p: PageNode => "page"
      case q: Quad => "quad"
      case _ => "instance"
    }
    failedPageMap.get(lang) match{
      case Some(map) => map += ((id,node) -> exception)
      case None =>  failedPageMap += lang -> mutable.Map[(String, T), Throwable]((id, node) -> exception)
    }
    val line = "{task} failed for " + tag + " " + id + ": " + exception.getMessage
    printLabeledLine(line, RecordSeverity.Exception, lang, Seq(PrinterDestination.err, PrinterDestination.file))
    for (ste <- exception.getStackTrace)
      printLabeledLine("\t" + ste.toString, RecordSeverity.Exception, lang, Seq(PrinterDestination.file), noLabel = true)

    if(slackCredantials != null && failedPages(lang) % (slackCredantials.exceptionThreshold * slackIncreaseExceptionThreshold) == 0)
      forwardExceptionWarning(lang)

    if(monitor != null) monitor.reportError(this, exception)

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
      printLabeledLine("page " + id + ": " + title.encoded + " extracted", RecordSeverity.Info, title.language, Seq(PrinterDestination.file))
    }
    val pages = increaseAndGetSuccessfulPages(title.language)
    if(pages % reportInterval == 0)
      printLabeledLine("extracted {page} pages; {mspp} per page; {fail} failed pages", RecordSeverity.Info, title.language)
    if(slackCredantials != null && pages % slackCredantials.summaryThreshold == 0)
      forwardSummary(title.language)
  }

  def recordGenericPage(lang: Language, line: String = null): Unit ={
    val pages = increaseAndGetSuccessfulPages(lang)
    val l = if(line == null) "processed {page} instances; {mspp} per instance; {fail} failed instances" else line
    if(pages % reportInterval == 0)
      printLabeledLine(l, RecordSeverity.Info, lang)
    if(slackCredantials != null && pages % slackCredantials.summaryThreshold == 0)
      forwardSummary(lang)
  }

  /**
    * record (successful) quad
    *
    * @param quad
    * @param lang
    */
  def recordQuad(quad: Quad, severity: RecordSeverity.Value, lang:Language): Unit = synchronized {
    if(increaseAndGetSuccessfulPages(lang) % reportInterval == 0)
      printLabeledLine("processed {page} quads; {mspp} per quad; {fail} failed quads", severity, lang)
  }

    /**
    * print a line to std out, err or the log file
    *
    * @param line - the line in question
    * @param language - langauge of current page
    * @param print - enum values for printer destinations (err, out, file - null mean all of them)
    * @param noLabel - the initial label (lang: time passed) is omitted
    */
  def printLabeledLine(line:String, severity: RecordSeverity.Value, language: Language = null, print: Seq[PrinterDestination.Value] = null, noLabel: Boolean = false): Unit ={
    val lang = if(language != null) language else defaultLang
    val printOptions = if(print == null) {
      if(severity == RecordSeverity.Exception )
        Seq(PrinterDestination.err, PrinterDestination.out, PrinterDestination.file)
      else if(severity == RecordSeverity.Info)
        Seq(PrinterDestination.out, PrinterDestination.file)
      else
        Seq(PrinterDestination.file)
    } else print

    val status = getStatusValues(lang)
    val replacedLine = (if (noLabel) "" else severity.toString + "; " + lang.wikiCode + "; {task} at {time} for {data}; ") + line
    val pattern = "\\{\\s*\\w+\\s*\\}".r
    var lastend = 0
    var resultString = ""
    for(matchh <- pattern.findAllMatchIn(replacedLine)){
      resultString += replacedLine.substring(lastend, matchh.start)
      resultString += (Option(matchh.matched) match{
        case Some(m) =>
          m match{
            case i if i == "{time}" => status("time")
            case i if i == "{mspp}" => status("mspp")
            case i if i == "{page}" => status("pages")
            case i if i == "{erate}" => status("erate")
            case i if i == "{fail}" => status("failed")
            case i if i == "{data}" => status("dataset")
            case i if i == "{task}" => status("task")
            case _ => ""
          }
        case None => ""
      })
      lastend = matchh.end
    }
    resultString += replacedLine.substring(lastend)

    for(pr <-printOptions)
      pr match{
        case PrinterDestination.err => System.err.println(resultString)
        case PrinterDestination.out => System.out.println(resultString)
        case PrinterDestination.file if writerOpen => logWriter.append(resultString + "\n")
        case _ =>
      }
  }

  def getStatusValues(lang: Language): Map[String, String] = {
    val pages = successfulPages(lang)
    val time = System.currentTimeMillis - startTime.get
    val failed = failedPages(lang)
    val datasetss = if(datasets.nonEmpty && datasets.size <= 3)
      datasets.foldLeft[String]("")((x,y) => x + ", " + y.encoded).substring(2)
    else
      String.valueOf(datasets.size) + " datasets"

    Map("pages" -> pages.toString,
      "failed" -> failed.toString,
      "mspp" -> (decForm.format(time.toDouble / pages) + " ms"),
      "erate" -> (if(failed == 0) "0" else ((pages+failed) / failed).toString),
      "dataset" -> datasetss,
      "time" -> StringUtils.prettyMillis(time),
      "task" -> task
    )
  }

  def initialize(lang: Language, task: String = "transformation", datasets: Seq[Dataset] = Seq()): Boolean ={
    if(initialized)
      return false
    this.failedPageMap = Map[Language, scala.collection.mutable.Map[(String, T), Throwable]]()
    this.successfulPagesMap = Map[Language, scala.collection.mutable.Map[Long, WikiTitle]]()
    this.successfulPageCount = Map[Language,AtomicLong]()
    this.successfulTripleCount = Map[Dataset, AtomicLong]()

    this.startTime.set(System.currentTimeMillis)
    this.defaultLang = lang
    this.task = task
    this.datasets ++= datasets

    if(monitor != null)
      monitor.init(this)

    if(preamble != null)
      printLabeledLine(preamble, RecordSeverity.Info, lang)

    val line = "Extraction started for language: " + lang.name + " (" + lang.wikiCode + ")" + (if (datasets.nonEmpty) " on " + datasets.size + " datasets." else "")
    printLabeledLine(line, RecordSeverity.Info, lang)
    forwardExtractionOverview(lang, line)
    initialized = true
    true
  }

  override def finalize(): Unit ={
    if(writerOpen){
      logWriter.close()
      writerOpen = false
    }

    if(monitor != null) {
      printMonitorSummary(monitor.summarize(this, datasets))
    }

    val line = "Extraction finished for language: " + defaultLang.name + " (" + defaultLang.wikiCode + ") " +
      (if(datasets.nonEmpty) ", extracted " + successfulPages(defaultLang) + " pages for " + datasets.size + " datasets after " + StringUtils.prettyMillis(System.currentTimeMillis - startTime.get) + " minutes." else "")
    printLabeledLine(line, RecordSeverity.Info, defaultLang)
    forwardSimpleLine(line)

    super.finalize()
  }

  def resetFailedPages(lang: Language) = failedPageMap.get(lang) match{
    case Some(m) =>
      m.clear()
      successfulPageCount(lang).set(0)
    case None =>
  }

  object PrinterDestination extends Enumeration {
    val out, err, file = Value
  }

  /**
    * the following methods will post messages to a Slack webhook if the Slack-Cedentials are available in the config file
    */
  var lastExceptionMsg = new java.util.Date().getTime

  /**
    * forward an exception summary to slack
    * (will increase the slack-exception-threshold by factor 2 if two of these messages are fired within 2 minutes)
    * @param lang
    */
  def forwardExceptionWarning(lang: Language) : Unit =
  {
    if(slackCredantials == null)
      return
    //if error warnings are less than 2 min apart increase threshold
    if((new java.util.Date().getTime - lastExceptionMsg) / 1000 < 120)
      slackIncreaseExceptionThreshold = slackIncreaseExceptionThreshold*2
    lastExceptionMsg = new java.util.Date().getTime

    val attachments = new JsonArray()
    val attachment = getAttachment("Multiple pages failed to be extracted.", if(slackIncreaseExceptionThreshold < 5) "warning" else "danger")
    val fields = new JsonArray()
    attachment.put("fields", fields)
    attachments.add(attachment)
    addKeyValue(fields, "Number of exceptions", failedPages(lang).toString)

    val data = defaultMessage("Exception status report for language " + lang.name, null, attachments)

    sendCurl(slackCredantials.webhook.toString, data)
  }

  def forwardSummary(lang: Language) : Unit =
  {
    if(slackCredantials == null)
      return
    val status = getStatusValues(lang)
    val attachments = new JsonArray()
    val attachment = getAttachment("Status Report of " + status("task"), "#36a64f")
    val fields = new JsonArray()
    addKeyValue(fields, "extracted pages:", status("pages"))
    if(status("dataset").nonEmpty)
      addKeyValue(fields, "extracted datasets:", status("dataset"))
    addKeyValue(fields, "time elapsed: ", status("time"))
    addKeyValue(fields, "per page: ", status("mspp"))
    addKeyValue(fields, "failed pages: ", status("failed"))
    attachment.put("fields", fields)
    attachments.add(attachment)

    sendCurl(slackCredantials.webhook.toString, defaultMessage("Summary report for extraction of language " + lang.name + " (" + lang.wikiCode + ")", null, attachments))
  }

  def forwardExtractionOverview(lang: Language, msg: String) : Unit ={
    if(slackCredantials == null || datasets.isEmpty)
      return
    val attachments = new JsonArray()
    val attachment = getAttachment("The datasets extracted are:", "#439FE0")
    val fields = new JsonArray()

    for(dataset <- datasets.sortBy(x => x.encoded)){
      val field = new JsonObject()
      field.put("title", dataset.name)
      field.put("value", dataset.versionUri)
      field.put("short", false)
      fields.add(field)
    }

    attachment.put("fields", fields)
    attachments.add(attachment)

    sendCurl(slackCredantials.webhook.toString, defaultMessage(msg, null, attachments))
  }

  def forwardSimpleLine(line: String) : Unit =
  {
    if(slackCredantials == null)
      return

    sendCurl(slackCredantials.webhook.toString, defaultMessage(line, null))
  }

  def getAttachment(attachMsg: String, color: String): JsonObject =
  {
    val attachment = new JsonObject()
    attachment.put("title", attachMsg)
    val fields = new JsonArray()
    attachment.put("fields", fields)
    attachment.put("color", color)
    attachment
  }

  def addKeyValue(array: JsonArray, key: String, value: String): Unit =
  {
    val left = new JsonObject()
    left.put("value", key)
    left.put("short", true)
    val right = new JsonObject()
    right.put("value", value)
    right.put("short", true)
    array.add(left)
    array.add(right)
  }

  def defaultMessage(mainText: String, subText: String, attachments: JsonArray = null): JsonObject =
  {
    val data = new JsonObject()
    data.put("text", mainText)
    if(subText != null)
      data.put("pretext", subText)
    data.put("username", slackCredantials.username)
    data.put("icon_emoji", ":card_index:")

    if(attachments != null)
      data.put("attachments", attachments)
    data
  }

  def sendCurl(url: String, data: JsonObject): Boolean =
  {
    try {
      val baos = new ByteArrayOutputStream()
      JSON.write(baos, data)
      val resp = Http(url).postData(new String(baos.toByteArray, Charset.defaultCharset())).asString
      if (resp.code != 200) {
        System.err.println(resp.body)
      }
      true
    }
    catch{
      case e : SocketTimeoutException => false
    }
  }

  def printMonitorSummary(summaryMap : mutable.HashMap[String, Object]) : Unit = {
    val error = summaryMap.getOrElse("EXCEPTIONCOUNT","0")
    val success = summaryMap.getOrElse("SUCCESSFUL", new AtomicLong(0))
    val crashed = summaryMap.getOrElse("CRASHED", "no")
    val dataIDResults = summaryMap.getOrElse("DATAID", "")
    var exceptionString = ""
    val exceptionMap = summaryMap.getOrElse("EXCEPTIONS", mutable.HashMap[String, Int]())
      .asInstanceOf[mutable.HashMap[String,Int]]
    exceptionMap.keySet.foreach(key => exceptionString += key + ": " + exceptionMap(key))

    var errString = ""
    var outString = ""

    // Summary String Building:
    // Complete Statistic on Severity-Level Info
    // Critical Error Statistic on Severity-Level Exception

    outString += (String.format("%-20s", "EXCEPTIONCOUNT:")+ s"$error")+ "\n"

    if(error.toString.toInt > 0) {
      errString += (String.format("%-20s", "EXCEPTIONCOUNT:")+ s"$error")+ "\n"
    }

    outString += String.format("%-20s", "SUCCESSFUL:") + success.asInstanceOf[AtomicLong].get() + "\n"
    if(success.asInstanceOf[AtomicLong].get() == 0)
      errString += String.format("%-20s", "SUCCESSFUL:") + success.asInstanceOf[AtomicLong].get()+ "\n"

    outString += String.format("%-20s", "CRASHED:") + s"$crashed"+ "\n"
    if(crashed == "yes")
      errString += String.format("%-20s", "CRASHED:") + s"$crashed"+ "\n"

    if(exceptionString != "") {
      exceptionString = "EXCEPTIONS:\n" + exceptionString+ "\n"
      errString += exceptionString
    }

    var errDataid = ""
    dataIDResults.toString.lines.foreach(line => {
      outString += line + "\n"
      if(line.startsWith("!")) errDataid += line + "\n"
    })

    if(errDataid != "") errString += "DATAID:\n" + errDataid

    outString = "\n----- ----- ----- EXTRACTION MONITOR STATISTICS ----- ----- -----\n" + outString +
    "\n----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----"
    printLabeledLine( outString, RecordSeverity.Info, defaultLang)

    if(errString != "") {
      errString = "\n----- ----- ----- EXTRACTION MONITOR STATISTICS ----- ----- -----\n" + errString +
      "\n----- ----- ----- ----- ----- ----- ----- ----- ----- ----- -----"
      printLabeledLine( errString, RecordSeverity.Exception, defaultLang)
    }

  }

  def getSuccessfulPageCount(): Map[Language,AtomicLong] = {
    successfulPageCount
  }

}

/**
  * This class provides the necessary attributes to record either a successful or failed extraction
  *
  * @param page
  * @param language
  * @param errorMsg
  * @param error
  * @param logSuccessfulPage
  */
class RecordEntry[T](
  val page: T,
  val identifier: String,
  val severity: RecordSeverity.Value,
  val language: Language,
  val errorMsg: String= null,
  val error:Throwable = null,
  val logSuccessfulPage:Boolean = false
)

object RecordSeverity extends Enumeration {
  val Info, Warning, Exception = Value
}

class WikiPageEntry(page : WikiPage) extends RecordEntry[WikiPage](
  page = page,
  identifier = page.uri,
  severity = RecordSeverity.Info,
  language = page.title.language
)
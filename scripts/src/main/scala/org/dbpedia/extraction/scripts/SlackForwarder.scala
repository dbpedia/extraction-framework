package org.dbpedia.extraction.scripts

import java.awt.event.ActionEvent
import java.io._
import java.net.URI
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.logging.{Logger}
import javax.swing.{AbstractAction, Timer}

import org.apache.jena.atlas.json.{JsonArray, JsonObject, JSON}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.{StringBuilder, ListBuffer}
import scala.util.matching.Regex

import scalaj.http.{Http}

/**
  * Created by Chile on 2/22/2016.
  */
object SlackForwarder {

  val msgBuffer = new mutable.LinkedHashMap[Int, String]()
  var msgCount = 0
  var regexMap : Map[Regex, (Int, JsonObject)] = null
  var exceptionMap : Map[String, Int] = Map.empty
  //1: display all msg from, 2: msg to, 3: forward this to slack, 4: exit forwarder after this msg, 5: optional initial mag
  var actions = new ListBuffer[SlackTransition]()

  val exceptionKey = "^(?!(Caused by:|\\[|\\s+at)).*Exception".r
  var lastExceptionSummery = 0l
  val pagesInitKey = "^INFO:\\s*(\\w+):\\s+(\\d+)\\s+extractors\\s+\\(([a-zA-Z,; ]+)\\)".r
  val pagesKey = "^(\\w+):\\s*extracted\\s+(\\d+)\\s+pages\\s+in\\s+([0-9.:s]+)\\s+\\(per\\s+page:\\s+([0-9.]+)\\s+ms;\\s+failed\\s+pages:\\s+(\\d+)".r

  class SlackTransition
  (
      val regex: Regex,
      val startFrom: Int,
      val until: Int,
      val current: Int,
      val threshold: Int,
      val exit: Boolean,
      val sb: StringBuilder
  )

  def main(args: Array[String]): Unit =
  {
    val logger = Logger.getLogger(getClass.getName)

    val webhookurl = args(0)
    require(URI.create(webhookurl) != null, "Please provide a valid slack webhook url!")
    val regexMapFile = new File(args(1))
    require(regexMapFile.isFile() && regexMapFile.canRead(), "Please specify a valid regex map file!")

    val logDirectory : File = if(args.length>2) new File(args(2)) else null
    require(logDirectory == null || (logDirectory.isDirectory() && logDirectory.canWrite()), "Please specify a valid log directory !")

    val launcher = args(3)
    val date = new SimpleDateFormat("ddMMyyyyhhmmss").format(new Date())

    val outFile = new File(logDirectory, date + "-" + launcher + (if(args.length > 4) "-" + args(4).replace(" ","_") else ""))
    outFile.createNewFile()
    val outPrintStream = new PrintStream(new FileOutputStream(outFile))

    outPrintStream.append("Extraction started at: " + date + "\n")
    outPrintStream.append("Used launcher: " + launcher + "\n")

    val source = scala.io.Source.fromFile(regexMapFile)
    val jsonString = source.mkString.replaceAll("#.*", "")
    source.close()
    val regexObj = JSON.parse(jsonString)

    regexMap = regexObj.keys().asScala.map(x => x.r -> (0, regexObj.get(x).getAsObject)).toMap

    insertRegex(exceptionKey, 100, "Exception summary:", "warning", "Multiple exceptions occurred.\n")
    insertRegex(pagesInitKey, 1, "Extraction started for language $1", "good")
    insertRegex(pagesKey, 50, "Extraction summary:", "good")

    val t = new Timer(300000, new AbstractAction() {
      def actionPerformed(e : ActionEvent) = {
        outPrintStream.close()
        System.exit(0)
      }
    })
    t.start()

    val in = new BufferedReader(new InputStreamReader(System.in))
    Stream.continually(in.readLine())
      .takeWhile(_ => true)
      .foreach(msg => processMsg(msg))

    def processMsg(msg: String): Unit =
    {
      t.restart()
      if(msgBuffer.size > 9999)
        msgBuffer.remove(0)
      msgBuffer.put(msgCount, msg)

      outPrintStream.append(msg.replace("stdSlack:", "").replace("stdGist:", "") + "\n")

      regexMap.keys.map(x => processRegexes(x, msg))
      for(action <- actions.filter(x => x.until == msgCount)) {
        msgBuffer.filter(x => x._1 >= action.startFrom && x._1 <= action.until).map(y => action.sb.append("\n" + y._2))
        //
        if (action.current % action.threshold == 0) {
          action.regex match {
            case `exceptionKey` => sendCurl(webhookurl, exceptionSummary())
            case `pagesInitKey` => {
              sendCurl(webhookurl, pagesSummary(getLastPageMsg()))
              sendCurl(webhookurl, startLangMsg(msg))
            }
            case `pagesKey` => sendCurl(webhookurl, pagesSummary(msg))
            case _ => sendCurl(webhookurl, defaultMessage(action.regex))
          }
        }
        if (action.exit)
        {
          sendCurl(webhookurl, pagesSummary(getLastPageMsg()))
          outPrintStream.close()
          System.exit(0)
        }
      }
      msgCount = msgCount+1
    }
  }

  def getLastPageMsg() : String =
  {
    msgBuffer.toList.reverse.find(z => pagesKey.findFirstMatchIn(z._2) match{
      case Some(u) => true
      case None => false
    }) match {
      case Some(f) => f._2
      case None => ""
    }
  }

  def insertRegex(regexKey: Regex, slack: Int, attachMsg: String, color: String = "#439FE0", defaultMsg: String = ""): Unit = {
    val exceptionObj = new JsonObject()
    exceptionObj.put("msg", defaultMsg)
    exceptionObj.put("exit", false)
    exceptionObj.put("slack", slack)
    exceptionObj.put("msgStart", 0)
    exceptionObj.put("msgUntil", 0)
    val attachment = new JsonObject()
    attachment.put("text", attachMsg)
    val fields = new JsonArray()
    attachment.put("fields", fields)
    attachment.put("color", color)
    exceptionObj.put("attachment", attachment)
    regexMap += regexKey -> (0, exceptionObj)
  }

  def processRegexes(x : Regex, msg : String) : Boolean =
  {
    x.findFirstMatchIn(msg) match {
      case Some(y) =>
      {
        actions += new SlackTransition(
          x,
          msgCount + regexMap.get(x).get._2.get("msgStart").getAsNumber.value().intValue(),
          msgCount + regexMap.get(x).get._2.get("msgUntil").getAsNumber.value().intValue(),
          regexMap.get(x).get._1 +1,
          regexMap.get(x).get._2.get("slack").getAsNumber.value().intValue(),
          regexMap.get(x).get._2.get("exit").getAsBoolean.value(),
          new scala.StringBuilder(regexMap.get(x).get._2.get("msg").getAsString.value())
        )
        regexMap += (x -> (regexMap.get(x).get._1 +1, regexMap.get(x).get._2))

        if(x == exceptionKey)
          "^[^\\s]*Exception[^\\s]*".r.findFirstIn(msg) match{
            case Some(z) => exceptionMap += z -> (exceptionMap.get(z) match {
              case Some(count) => count +1
              case None => 1
            })
            case None =>
          }
        true
      }
      case None => false
    }
  }

  def exceptionSummary() : JsonObject =
  {
    val tuple = regexMap.get(exceptionKey).get._2
    val data = defaultMessage(exceptionKey)
    val attachments = new JsonArray()
    val attachment = tuple.get("attachment").getAsObject
    val fields = new JsonArray()
    attachment.put("fields", fields)
    attachments.add(attachment)
    data.put("attachments", attachments)

    exceptionMap.map(y => addKeyValue(fields, y._1, y._2.toString))

    //increase number of exception needed to trigger an exception summary 10-fold if too many exception occur
    if((new Date().getTime - lastExceptionSummery) / 1000 <= tuple.get("slack").getAsNumber.value().intValue())
    {
      regexMap.get(exceptionKey).get._2.put("slack", tuple.get("slack").getAsNumber.value().intValue()*10)
      attachment.put("pretext","Warning, more than one exception per second!")
      attachment.put("color","danger")
    }

    lastExceptionSummery = new Date().getTime
    data
  }

  def startLangMsg(msg: String) : JsonObject =
  {
    val tuple = regexMap.get(pagesInitKey).get._2
    val data = defaultMessage(pagesInitKey)
    val attachments = new JsonArray()
    val attachment = tuple.get("attachment").getAsObject
    val fields = new JsonArray()
    attachment.put("fields", fields)
    attachments.add(attachment)
    data.put("attachments", attachments)

    val matchh = pagesInitKey.findAllMatchIn(msg).next()

    addKeyValue(fields, "Extracting language:", matchh.group(1))
    addKeyValue(fields, "Using " + matchh.group(2) + " extractors", "")

    data
  }

  def pagesSummary(msg: String) : JsonObject =
  {
    val tuple = regexMap.get(pagesKey).get._2
    val data = defaultMessage(pagesKey)
    val attachments = new JsonArray()
    val attachment = tuple.get("attachment").getAsObject
    val fields = new JsonArray()
    attachment.put("fields", fields)
    attachments.add(attachment)
    data.put("attachments", attachments)

    pagesKey.findFirstMatchIn(msg) match {
      case Some(matchh) =>{
        addKeyValue(fields, matchh.group(1) + " - extracted pages:", matchh.group(2))
        addKeyValue(fields, "time elapsed: ", matchh.group(3))
        addKeyValue(fields, "per page: ", matchh.group(4) + " ms")
        addKeyValue(fields, "failed pages: ", matchh.group(5))
      }
      case None =>
    }
    data
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

  def defaultMessage(key: Regex): JsonObject =
  {
    val data = new JsonObject()
    data.put("text", regexMap.get(key).get._2.get("msg").getAsString.value())
    data.put("username", "extractor")
    data.put("icon_emoji", ":card_index:")
    data
  }

  def sendCurl(url: String, data: JsonObject): Unit =
  {
    val baos = new ByteArrayOutputStream()
    JSON.write(baos, data)
    val resp = Http(url).postData(new String(baos.toByteArray(), Charset.defaultCharset())).asString
    if(resp.code != 200)
    {
      System.err.println(resp.body)
    }
  }
}
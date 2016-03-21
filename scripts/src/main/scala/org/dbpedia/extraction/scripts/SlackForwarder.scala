package org.dbpedia.extraction.scripts

import java.io._
import java.net.URI
import java.util.logging.{Level, Logger}

import org.apache.jena.atlas.json.{JsonObject, JSON}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex

import scalaj.http.{Http}

/**
  * Created by Chile on 2/22/2016.
  */
object SlackForwarder {

  val msgBuffer = new mutable.LinkedHashMap[Int, String]()
  var msgCount = 0
  var actions = new ListBuffer[Tuple5[Int,Int,Boolean, Boolean, StringBuilder]]() //1: display all msg from, 2: msg to, 3: forward this to slack, 4: exit forwarder after this msg, 5: optional initial mag, 6: is obsolete

  def main(args: Array[String]): Unit =
  {
    val logger = Logger.getLogger(getClass.getName)

    val webhookurl = args(0)
    require(URI.create(webhookurl) != null, "Please provide a valid slack webhook url!")

    val regexMapFile = new File(args(1))
    require(regexMapFile.isFile() && regexMapFile.canRead(), "Please specify a valid regex map file!")

    val stdoutFile : File = if(args.length>2) new File(args(2)) else null
    stdoutFile.createNewFile()
    require(stdoutFile == null || (stdoutFile.isFile() && stdoutFile.canWrite()), "Please specify a valid file for writing the stdout stream!")

    val outPrintStream = if(stdoutFile != null) new PrintStream(new FileOutputStream(stdoutFile)) else null

    val source = scala.io.Source.fromFile(regexMapFile)
    val jsonString = source.mkString.replaceAll("#.*", "")
    source.close()
    val regexObj = JSON.parse(jsonString)
    val regexMap : Map[Regex, JsonObject] = regexObj.keys().asScala.map(x => x.r -> regexObj.get(x).getAsObject).toMap

    val in = new BufferedReader(new InputStreamReader(System.in))
    Stream.continually(in.readLine())
      .takeWhile(_ => true)
      .foreach(msg => processMsg(msg))

    def processMsg(msg: String): Unit =
    {
      if(msgBuffer.size > 999)
        msgBuffer.remove(0)
      msgBuffer.put(msgCount, msg)

      regexMap.keys.map(x => {
        x.findFirstMatchIn(msg) match {
          case Some(y) =>
            {
              actions += new Tuple5(
                msgCount + regexMap.get(x).get.get("msgStart").getAsNumber.value().intValue(),
                msgCount + regexMap.get(x).get.get("msgUntil").getAsNumber.value().intValue(),
                regexMap.get(x).get.get("slack").getAsBoolean.value(),
                regexMap.get(x).get.get("exit").getAsBoolean.value(),
                new StringBuilder(regexMap.get(x).get.get("msg").getAsString.value())
              )
            }
          case None =>
        }
      })

      for(action <- actions.filter(x => x._2 == msgCount)) {
        msgBuffer.filter(x => x._1 >= action._1 && x._1 <= action._2).map(y => action._5.append("\n" + y._2))
        outPrintStream.append(action._5.toString().replace("stdSlack:", "").replace("stdGist:", ""))
        if (action._3) {
          sendCurl(webhookurl, action._5.toString().replace("stdSlack:", "").replace("stdGist:", ""))
        }
        if (action._4) {
          outPrintStream.close()
          logger.log(Level.INFO, action._5.toString().replace("stdSlack:", "").replace("stdGist:", ""))
          System.exit(0)
        }
      }
      msgCount = msgCount+1
    }
  }

  def sendCurl(url: String, msg: String): Unit =
  {
    val resp = Http(url).postData("{\"text\": \"" + msg + "\" }").asString
    if(resp.code != 200)
    {
      System.err.println(resp.body)
    }
  }
}

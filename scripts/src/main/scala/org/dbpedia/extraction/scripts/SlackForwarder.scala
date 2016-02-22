package org.dbpedia.extraction.scripts

import java.io._
import java.net.URI

import org.apache.jena.atlas.json.{JsonObject, JSON}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.util.matching.Regex

import scalaj.http.{HttpOptions, Http}

/**
  * Created by Chile on 2/22/2016.
  */
object SlackForwarder {

  val msgBuffer = new mutable.LinkedHashMap[Int, String]()
  var msgCount = 0

  def main(args: Array[String]): Unit =
  {
    val webhookurl = args(0)
    require(URI.create(webhookurl) != null, "Please provide a valid slack webhook url!")

    val regexMapFile = new File(args(1))
    require(regexMapFile.isFile() && regexMapFile.canRead(), "Please specify a valid regex map file!")

    val stdoutFile : File = if(args.length>2) new File(args(2)) else null
    if(!stdoutFile.exists())
      stdoutFile.createNewFile()
    require(stdoutFile == null || (stdoutFile.isFile() && stdoutFile.canWrite()), "Please specify a valid file for writing the stdout stream!")
    val stderrFile : File = if(args.length>3) new File(args(3)) else null
    if(!stderrFile.exists())
      stderrFile.createNewFile()
    require(stderrFile == null || (stderrFile.isFile() && stderrFile.canWrite()), "Please specify a valid file for writing the stderr stream!")

    val outPrintStream = if(stdoutFile != null) new PrintStream(new FileOutputStream(stdoutFile)) else null
    val errPrintStream = if(stderrFile != null) new PrintStream(new FileOutputStream(stderrFile)) else null

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
        msgBuffer.remove(msgCount-1000)
      msgBuffer.put(msgCount, msg)

      if(outPrintStream != null && msg.startsWith("stdout:"))
        outPrintStream.print(msg.replace("stdout:",""))
      if(errPrintStream != null && msg.startsWith("stderr:"))
        errPrintStream.print(msg.replace("stderr:",""))

      regexMap.keys.map(x => {
        x.findFirstMatchIn(msg) match {
          case Some(y) =>
            {
            if(regexMap.get(x).get.get("exit").getAsBoolean.value())
            {
              outPrintStream.close()
              errPrintStream.close()
              sendCurl(webhookurl, regexMap.get(x).get.get("msg").getAsString.value())
              System.exit(0)
            }
              else
              sendCurl(webhookurl, regexMap.get(x).get.get("msg").getAsString.value())

            }
        }
      })

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

package org.dbpedia.extraction.scripts

import java.io.{File, Writer}
import java.util.logging.{Level, Logger}

import scala.util.control.Breaks._
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.util.RichFile.wrapFile

import scala.collection.mutable
import scala.language.postfixOps
import scala.sys.process._
import scala.util.matching.Regex

/**
  * Created by chile on 30.06.17.
  */
object CreateLinesBytesPacked {
  private val logger = Logger.getLogger(getClass.getName)
  private var pattern: Regex = ".*\\.bz2$".r

  def resolveSymLink(file: File): String ={
    val command = "file=" + file.getAbsolutePath + "; test=$(stat  --printf='%F' $file ); if [[ 'symbolic link' == \"$test\" ]] ; then file=$(readlink -f $file); fi; echo \"$file\";"
    Process("/bin/bash", Seq("-c", command)).!!.replaceAll("\\n", "")
  }

  def getMd5(file: File): String = Process("/bin/bash", Seq("-c", "md5deep -o fl -l <" + resolveSymLink(file) + " | tr --delete '\\n'")).!!.trim
  def getLinesCompressed(file: File): (String, String) = {
    val link = resolveSymLink(file)
    val exception = new StringBuilder()
    val zw = Process("/bin/bash", Seq("-c", "bzip2 -cd <" + link + " | wc -cl")).!!<(
      ProcessLogger.apply((o: String) => o, (e: String)=>
        exception.append(e)))

    if(exception.nonEmpty && exception.toString.contains("bzip2"))
      throw new IllegalArgumentException("An exception arose: " + exception.toString)

    val tt = zw.replaceAll("\\n", "").trim.split("\\s+")
    (tt.head, tt(1))
  }

  def getLength(file: File): String = Process("/bin/bash", Seq("-c", "stat --printf=' %s ' " + resolveSymLink(file))).!!.replaceAll("\\n", "").trim

  def readLBP(file: File): mutable.HashMap[String, mutable.HashMap[String, String]] = {
    val map: mutable.HashMap[String, mutable.HashMap[String, String]] = new mutable.HashMap[String, mutable.HashMap[String, String]]()
    IOUtils.readLines(file) { line: String =>
      breakable {
        if(line == null || line.startsWith("Exception:"))
          break

        val splits = line.trim.split(";")
        if (splits.length != 5) {
          logger.log(Level.WARNING, "The file " + file.getAbsoluteFile + " was not in the expected csv format of 5 columns.")
          break
        }
        val insert = new mutable.HashMap[String, String]()
        insert("lines") = splits(1)
        insert("bytes") = splits(2)
        insert("packed") = splits(3)
        insert("hash") = splits(4)
        map.put(splits.head, insert)
      }
    }
    map
  }



  def main(args: Array[String]): Unit = {

    require(args != null && args.length == 1, "One arguments required, extraction config file")

    val config = new Config(args(0))

    val baseDir = config.dumpDir
    require(baseDir.isDirectory && baseDir.canRead && baseDir.canWrite, "Please specify a valid local base extraction directory - invalid path: " + baseDir)

    val language = config.languages

    baseDir.listFiles().filter(x => x.isDirectory && x.getName.endsWith(config.wikiName)).foreach(x =>
      throw new IllegalArgumentException("Please make sure to run this script on the Download Server only. The following directory should therefore not end with the wiki string: " + x))

    def writeInsert(file: File, writer: Writer): Unit = {
      val lin = getLinesCompressed(file)
      writer.write(file.getAbsolutePath.substring(baseDir.getAbsolutePath.length+1))
      writer.write(";")
      writer.write(lin._1)
      writer.write(";")
      writer.write(lin._2)
      writer.write(";")
      writer.write(getLength(file))
      writer.write(";")
      writer.write(getMd5(file))
      writer.write("\n")
    }

    val dirWorkers = SimpleWorkers(config.parallelProcesses, config.parallelProcesses) { lang: Language =>
      logger.log(Level.INFO, "starting language " + lang.name)
      var map: mutable.HashMap[String, mutable.HashMap[String, String]] = null
      val path = new File(baseDir, lang.wikiCode)
      if (!path.exists || !path.isDirectory) {
        logger.log(Level.SEVERE, "No direcory for language " + lang.wikiCode + " was found: " + path.getAbsolutePath)
        return
      }

      val target = new File(path, "lines-bytes-packed.csv")
      if (target.exists())
        map = readLBP(target)
      else
        map = new mutable.HashMap[String, mutable.HashMap[String, String]]()

      target.createNewFile()
      val writer = IOUtils.writer(target)
      var lastFile: String = ""

      try {
        for (file <- path.listFiles().filter(x => x.isFile && pattern.findFirstMatchIn(x.getName).isDefined).sortBy(x => x.getName)) {
          lastFile = file.getAbsolutePath
          map.get(file.getAbsolutePath.substring(baseDir.getAbsolutePath.length+1)) match {
            case Some(insert) => if (insert("hash") != getMd5(file))
              writeInsert(file, writer)
            else {
              writer.write(file.getAbsolutePath.substring(baseDir.getAbsolutePath.length+1))
              writer.write(";")
              writer.write(insert("lines"))
              writer.write(";")
              writer.write(insert("bytes"))
              writer.write(";")
              writer.write(insert("packed"))
              writer.write(";")
              writer.write(insert("hash"))
              writer.write("\n")
            }
            case None => writeInsert(file, writer)
          }
        }
      } catch{
        case f: Throwable =>
          logger.log(Level.SEVERE, "An exception for filr " + lastFile + " arose: " + f.getMessage)
          writer.write("Exception: " + f.getMessage + "\n")
          writer.close()
      }
      finally
        writer.close()
    }

    Workers.work[Language](dirWorkers, language)
  }
}

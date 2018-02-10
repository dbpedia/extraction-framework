package org.dbpedia.extraction.scripts

import java.io.{File, Writer}

import org.apache.commons.lang3.SystemUtils
import org.apache.log4j.{Level, Logger}
import org.dbpedia.extraction.config.{Config, ExtractionLogger, ExtractionRecorder}

import scala.util.control.Breaks._
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.util.RichFile.wrapFile

import scala.collection.mutable
import scala.language.postfixOps
import scala.sys.process._
import scala.util.matching.Regex

/**
  * Created by chile on 30.06.17.
  * This script will create the lines-bytes-packed.csv files for every subdirectory (maxdepth 2),
  * as well as the _checksums.md5 file...
  * recording each bz2 file (this can be changed by adapting the 'pattern' variable.
  * Format (filepath;lines;uncompressed length;file length;md5 hash)
  * It should only be run on the Download Server. Its results are then used by the DataIdGenerator script.
  * ! only suitable for UNIX environments !
  */
object CreateLinesBytesPacked {
  private val logger = ExtractionLogger.getLogger(getClass, Language.None)
  private val pattern: Regex = ".*\\.bz2$".r

  def resolveSymLink(file: File): String ={
    val command = "file=" + file.getAbsolutePath + "; test=$(stat  --printf='%F' $file ); if [[ 'symbolic link' == \"$test\" ]] ; then file=$(readlink -f $file); fi; echo \"$file\";"
    Process("/bin/bash", Seq("-c", command)).!!.replaceAll("\\n", "").trim
  }

  def getMd5(file: File): String = Process("/bin/bash", Seq("-c", "md5deep -o fl -l <" + resolveSymLink(file) + " | tr --delete '\\n'")).!!.trim

  def getLinesCompressed(file: File): (String, String) = {
    val link = resolveSymLink(file)
    val exception = new StringBuilder()
    val zw = Process("/bin/bash", Seq("-c", "bzip2 -cd <" + link + " | wc -cl")).!!< (
      ProcessLogger.apply((o: String) => o.toString, (e: String)=> exception.append(e))
    )

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
          logger.log(Level.WARN, "The file " + file.getAbsoluteFile + " was not in the expected csv format of 5 columns.")
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

    if (!SystemUtils.IS_OS_UNIX)
      throw new RuntimeException("Make sure to run this script only on UNIX environments!")

    require(args != null && args.length == 1, "One arguments required, extraction config file")

    val config = new Config(args(0))

    val dirPattern = config.getArbitraryStringProperty("directory-pattern") match{
      case Some(p) => p.r
      case None => "\\.".r
    }

    val baseDir = config.dumpDir
    require(baseDir.isDirectory && baseDir.canRead && baseDir.canWrite, "Please specify a valid local base extraction directory - invalid path: " + baseDir)

    val directories = baseDir.listFiles.filter(_.isDirectory).flatMap(x => x.listFiles.filter(x => x.isDirectory && dirPattern.findFirstMatchIn(x.getName).isDefined)).map(x => new RichPath(x.toPath)) ++
      baseDir.listFiles.filter(x => x.isDirectory && dirPattern.findFirstMatchIn(x.getName).isDefined).map(x => new RichPath(x.toPath))

    def writeInsert(file: File, writer: Writer, md5: String): Unit = {
      val lin = getLinesCompressed(file)
      writer.write(file.getAbsolutePath.substring(baseDir.getAbsolutePath.length+1))
      writer.write(";")
      writer.write(lin._1)
      writer.write(";")
      writer.write(lin._2)
      writer.write(";")
      writer.write(getLength(file))
      writer.write(";")
      writer.write(md5)
      writer.write("\n")
    }

    val dirWorkers = SimpleWorkers(config.parallelProcesses, config.parallelProcesses) { path: FileLike[_] =>

      if (!path.exists || !path.isDirectory) {
        logger.log(Level.FATAL, "Directory does not exist : " + path.getFile.getAbsolutePath)
        return
      }
      //get all bz2 files of this directory and check if this list is empty
      val files = path.getFile.listFiles().filter(x => x.isFile && pattern.findFirstMatchIn(x.getName).isDefined).sortBy(x => x.getName)
      if(files.isEmpty)
        return

      logger.log(Level.INFO, "starting directory " + path.name)
      var map: mutable.HashMap[String, mutable.HashMap[String, String]] = null

      val target = new File(path.getFile, "lines-bytes-packed.csv")
      if (target.exists())
        map = readLBP(target)
      else
        map = new mutable.HashMap[String, mutable.HashMap[String, String]]()
      target.createNewFile()
      val writer = IOUtils.writer(target)

      val md5File = new File(path.getFile, "_checksums.md5")
      md5File.createNewFile()
      val md5Writer = IOUtils.writer(md5File)

      var lastFile: String = ""

      try {
        for (file <- files) {
          lastFile = file.getAbsolutePath
          val md5 = getMd5(file)
          md5Writer.write(md5 + "  " + file.getCanonicalPath.replace(baseDir.toString, ".") + "\n")

          map.get(file.getAbsolutePath.substring(baseDir.getAbsolutePath.length+1)) match {
            case Some(insert) => if (insert("hash").trim != md5)
              writeInsert(file, writer, md5)
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
            case None => writeInsert(file, writer, md5)
          }
        }
      } catch{
        case f: Throwable =>
          logger.log(Level.FATAL, "An exception for file " + lastFile + " arose: " + f.getMessage)
          writer.write("Exception: " + f.getMessage + "\n")
          writer.close()
      }
      finally{
        writer.close()
        md5Writer.close()
      }
    }

    Workers.work[FileLike[_]](dirWorkers, directories.sortBy(x => x.getFile.getCanonicalPath))
  }
}

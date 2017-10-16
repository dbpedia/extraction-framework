package org.dbpedia.extraction.dump.download

import java.net.URL
import java.io.{File, PrintWriter}

import org.dbpedia.extraction.config.Config

import scala.collection.immutable.SortedSet
import scala.io.{Codec, Source}
import org.dbpedia.extraction.util.{Finder, Language}
import org.dbpedia.extraction.util.RichFile.wrapFile

import scala.collection.mutable

/**
 */
class LanguageDownloader(final val config: DownloadConfig, final val downloader: Downloader, final val lang: Language)
{
  private val DateLink = """<a href="(\d{8})/">""".r

  private val finder = new Finder[File](config.dumpDir, lang, config.wikiName)
  private val wiki = finder.wikiName
  private val mainPage = new URL(config.baseUrl, wiki+"/") // here the server does NOT use index.html
  private val mainDir = new File(config.dumpDir, wiki)
  private val fileNames = config.source
  if (! mainDir.exists && ! mainDir.mkdirs) throw new Exception("Target directory ["+mainDir+"] does not exist and cannot be created")

  def downloadDates(dateRange: (String, String), dumpCount: Int): Unit = {

    val firstDate = dateRange._1
    val lastDate = dateRange._2


    config.getDownloadStarted(lang) match{
      case Some(started) =>
        if (! started.createNewFile) throw new Exception("Another process may be downloading files to ["+mainDir+"] - stop that process and remove ["+started+"]")
        try {

          // find all dates on the main page, sort them latest first
          var dates = SortedSet.empty(Ordering[String].reverse)

          downloader.downloadTo(mainPage, mainDir) // creates index.html, although it does not exist on the server
          forEachLine(new File(mainDir, "index.html")) { line =>
            DateLink.findAllIn(line).matchData.foreach(dates += _.group(1))
          }

          if (dates.isEmpty) throw new Exception("found no date - "+mainPage+" is probably broken or unreachable. check your network / proxy settings.")

          var count = 0

          // find date pages that have all files we want
          for (date <- dates) {
            if (count < dumpCount && date >= firstDate && date <= lastDate && downloadDate(date)) count += 1
          }

          if (count == 0)
            throw new Exception("found no date on "+mainPage+" in range "+firstDate+"-"+lastDate+" with files "+fileNames.mkString(","))
        }
        finally started.delete
      case None =>
    }
  }

  private def expandFilenameRegex(date: String, index: File, filenameRegexes: Seq[String]): Seq[String] = {

    // Prepare regexes
    val regexes = filenameRegexes.map { regex =>
      ("<a href=\"/" + wiki + "/" + date + "/" + wiki + "-" + date + "-(" + regex + ")\">").r
    }

    // Result
    val filenames = mutable.Set[String]()

    forEachLine(index) { line =>
      regexes.foreach(regex => regex.findAllIn(line).matchData.foreach(filenames += _.group(1)))
    }

    filenames.toSeq
  }

  def downloadMostRecent(): Boolean = {
    // find all dates on the main page, sort them latest first
    var dates = SortedSet.empty(Ordering[String].reverse)

    downloader.downloadTo(mainPage, mainDir) // creates index.html, although it does not exist on the server
    forEachLine(new File(mainDir, "index.html")) { line =>
      DateLink.findAllIn(line).matchData.foreach(dates += _.group(1))
    }

    if (dates.isEmpty)
      throw new Exception("found no date - "+mainPage+" is probably broken or unreachable. check your network / proxy settings.")

    downloadDate(dates.head)
  }

  def downloadDate(date: String): Boolean = {

    val datePage = new URL(mainPage, date+"/") // here we could use index.html
    val dateDir = new File(mainDir, date)
    if (! dateDir.exists && ! dateDir.mkdirs) throw new Exception("Target directory '"+dateDir+"' does not exist and cannot be created")

    finder.file(date, Config.DownloadComplete) match{
      case None => false
      case Some(complete) =>

        // First download the files list to expand regexes
        downloader.downloadTo(datePage, dateDir) // creates index.html

        // Collect regexes
        val fileNamesFromRegexes = expandFilenameRegex(date, new File(dateDir, "index.html"), fileNames)

        val urls = fileNamesFromRegexes.map {
          fileName => new URL(config.baseUrl, wiki+"/"+date+"/"+wiki+"-"+date+"-"+fileName)
        }

        if (complete.exists) {
          // Previous download process said that this dir is complete. Note that we MUST check the
          // 'complete' file - the previous download may have crashed before all files were fully
          // downloaded. Checking that the downloaded files exist is necessary but not sufficient.
          // Checking the timestamps is sufficient but not efficient.

          if (urls.forall(url => new File(dateDir, downloader.targetName(url)).exists)) {
            println("did not download any files to '" + dateDir + "' - all files already complete")
            return true
          }

          // Some files are missing. Maybe previous process was configured for different files.
          // Download the files that are missing or have the wrong timestamp. Delete 'complete'
          // file first in case this download crashes.
          complete.delete
        }

        // all the links we need - only for non regexes (we have already checked regex ones)
        val links = new mutable.HashMap[String, String]()
        for (fileName <- fileNamesFromRegexes) links(fileName) = "<a href=\"/"+wiki+"/"+date+"/"+wiki+"-"+date+"-"+fileName+"\">"
        // Here we should set "<a href=\"/"+wiki+"/"+date+"/"+wiki+"-"+date+"-"+fileName+"\">"
        // but "\"/"+wiki+"/"+date+"/" does not exists in incremental updates, keeping the trailing "\">" should do the trick
        // for (fileName <- fileNames) links(fileName) = wiki+"-"+date+"-"+fileName+"\">"

        // downloader.downloadTo(datePage, dateDir) // creates index.html

        forEachLine(new File(dateDir, "index.html")) { line =>
          links.foreach{ case (fileName, link) => if (line contains link) links -= fileName }
        }

        // did we find them all?
        // Fail if:
        // - the user specified static file names and not all of them have been found
        // OR
        // - the user specified regular expressions and no file has been found that satisfied them
/*        if ((staticFileNames.nonEmpty && links.nonEmpty) || (regexes.nonEmpty && fileNamesFromRegexes.isEmpty)) {
          val staticFilesMessage = if (links.nonEmpty) " has no links to ["+links.keys.mkString(",")+"]" else ""
          val dynamicFilesMessage = if (fileNamesFromRegexes.isEmpty && regexes.nonEmpty) " has no links that satisfies ["+regexes.mkString(",")+"]" else ""
          println("date page '"+datePage+ staticFilesMessage + dynamicFilesMessage)
          false
        }
        else {*/
          println("date page '"+datePage+"' has all files ["+fileNamesFromRegexes.mkString(",")+"]")

          complete.createNewFile
          val pw = new PrintWriter(complete)
          pw.println(date)
          // download all files
          for (url <- urls) {
            downloader.downloadTo(url, dateDir)
            pw.println(url)
          }
          pw.close()
          true
    }
  }

  private def forEachLine(file: File)(process: String => Unit): Unit = {
    val source = Source.fromFile(file)(Codec.UTF8)
    try for (line <- source.getLines) process(line)
    finally source.close
  }

}

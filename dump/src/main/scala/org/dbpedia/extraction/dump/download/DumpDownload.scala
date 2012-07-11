package org.dbpedia.extraction.dump.download

import scala.collection.mutable.{Set,Map,HashSet}
import scala.collection.immutable.SortedSet
import java.net.{URL,URLConnection}
import java.io.{File,InputStream,IOException}
import scala.io.{Source,Codec}
import org.dbpedia.extraction.util.Finder
import org.dbpedia.extraction.util.RichFile.toRichFile
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.util.zip.GZIPInputStream

/**
 */
class DumpDownload(baseUrl: URL, baseDir: File, dumpRange: (Int, Int), downloader: Downloader)
{
  private val firstDump = dumpRange._1
  
  private val lastDump = dumpRange._2

  def downloadFiles(languages: Map[String, Set[String]]): Unit =
  {
    // sort them to have reproducible behavior
    val keys = SortedSet.empty[String] ++ languages.keys
    keys.foreach { key => 
      val done = keys.until(key)
      val todo = keys.from(key)
      println("done: "+done.size+" - "+done.mkString(","))
      println("todo: "+todo.size+" - "+keys.from(key).mkString(","))
      downloadFiles(key,languages(key)) 
    }
  }
  
  val DateLink = """<a href="(\d{8})/">""".r
  
  private def downloadFiles(language: String, fileNames: Set[String]): Unit = {
    
    val finder = new Finder[File](baseDir, language)
    
    val wiki = finder.wikiName
    
    val mainPage = new URL(baseUrl, wiki+"/") // here the server does NOT use index.html 
    val mainDir = new File(baseDir, wiki)
    if (! mainDir.exists && ! mainDir.mkdirs) throw new Exception("Target directory ["+mainDir+"] does not exist and cannot be created")
    
    val started = finder.file(Download.Started)
    if (! started.createNewFile) throw new Exception("Another process may be downloading files to ["+mainDir+"] - stop that process and remove ["+started+"]")
    try {
      
      // 1 - find all dates on the main page, sort them latest first
      var dates = SortedSet.empty(Ordering[String].reverse)
      
      downloader.downloadTo(mainPage, mainDir) // creates index.html, although it does not exist on the server
      forEachLine(new File(mainDir, "index.html")) { line => 
        DateLink.findAllIn(line).matchData.foreach(dates += _.group(1))
      }
      
      var currentDate = 0
      
      // 2 - find date pages that have all files we want
      for (date <- dates) {
        
        currentDate += 1
        
        val datePage = new URL(mainPage, date+"/") // here we could use index.html
        val dateDir = new File(mainDir, date)
        if (! dateDir.exists && ! dateDir.mkdirs) throw new Exception("Target directory '"+dateDir+"' does not exist and cannot be created")
        
        val complete = finder.file(date, Download.Complete)
        
        val urls = fileNames.map(fileName => new URL(baseUrl, wiki+"/"+date+"/"+wiki+"-"+date+"-"+fileName))
        
        if (complete.exists) {
          // Previous download process said that this dir is complete. Note that we MUST check the
          // 'complete' file - the previous download may have crashed before all files were fully
          // downloaded. Checking that the downloaded files exist is necessary but not sufficient.
          // Checking the timestamps is sufficient but not efficient.
          
          if (urls.forall(url => new File(dateDir, downloader.targetName(url)).exists)) {
            println("did not download any files to '"+dateDir+"' - all files already complete")
            return
          } 
          
          // Some files are missing. Maybe previous process was configured for different files.
          // Download the files that are missing or have the wrong timestamp. Delete 'complete' 
          // file first in case this download crashes. 
          complete.delete
        }
        
        // all the links we need
        val links = fileNames.map("<a href=\"/"+wiki+"/"+date+"/"+wiki+"-"+date+"-"+_+"\">")
        
        downloader.downloadTo(datePage, dateDir) // creates index.html
        forEachLine(new File(dateDir, "index.html")) { line => 
          links.foreach(link => if (line contains link) links -= link)
        }
        
        // did we find them all?
        if (! links.isEmpty) {
          println("date page '"+datePage+"' has no links to ["+links.mkString(",")+"]")
        }
        else {
          if (currentDate > lastDump) return
          
          val msg = "date page #"+currentDate+" '"+datePage+"' has all files ["+fileNames.mkString(",")+"]"
          if (currentDate < firstDump) {
            println(msg+", but we download only older dumps")
          }
          else {
            println(msg)
            
            // download all files
            for (url <- urls) downloader.downloadTo(url, dateDir)
            
            complete.createNewFile
          }
        }
      }
    }
    finally started.delete
    
    throw new Exception("found no date in "+mainPage+" with files "+fileNames.mkString(","))
  }
  
  private def forEachLine(file: File)(process: String => Unit): Unit = {
    val source = Source.fromFile(file)(Codec.UTF8)
    try for (line <- source.getLines) process(line)
    finally source.close
  }
  
}

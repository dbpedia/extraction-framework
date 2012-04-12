package org.dbpedia.extraction.dump.download

import scala.collection.mutable.{Set,Map,HashSet}
import scala.collection.immutable.SortedSet
import java.net.{URL,URLConnection}
import java.io.{File,InputStream,IOException}
import scala.io.{Source,Codec}

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.util.zip.GZIPInputStream

/**
 */
class DumpDownload(baseUrl : URL, baseDir : File, download : Download)
{
  def downloadFiles(languages : Map[String, Set[String]]) : Unit =
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
  
  private def downloadFiles(language : String, fileNames : Set[String]) : Unit =
  {
    import DumpDownload._
    
    val wiki = dumpName(language)
    
    val mainPage = new URL(baseUrl, wiki+"/") // here the server does NOT use index.html 
    val mainDir = new File(baseDir, wiki)
    if (! mainDir.exists && ! mainDir.mkdirs) throw new Exception("Target directory '"+mainDir+"' does not exist and cannot be created")
    val running = runningFile(baseDir, language)
    if (! running.createNewFile) throw new Exception("Another process is downloading files to '"+mainDir+"' - stop that process and remove '"+running+"'")
    try
    {
      // 1 - find all dates on the main page, sort them latest first
      var dates = SortedSet.empty(Ordering[String].reverse)
      
      download.downloadTo(mainPage, mainDir) // creates index.html, although it does not exist on the server
      eachLine(new File(mainDir, "index.html"), line => DateLink.findAllIn(line).matchData.foreach(dates += _.group(1)))
      
      // 2 - find date page that has all files we want
      for (date <- dates) // implicit conversion
      {
        val datePage = new URL(mainPage, date+"/") // here we could use index.html
        val dateDir = new File(mainDir, date)
        if (! dateDir.exists && ! dateDir.mkdirs) throw new Exception("Target directory '"+dateDir+"' does not exist and cannot be created")
        
        val complete = completeFile(baseDir, language, date)
        
        val urls = fileNames.map(fileName => new URL(baseUrl, wiki+"/"+date+"/"+wiki+"-"+date+"-"+fileName))
        if (complete.exists) {
          // Previous download process said that this dir is complete. Note that we MUST check the
          // 'complete' file - the previous download may have crashed before all files were fully
          // downloaded. Checking that the downloaded files exist is necessary but not enough.
          
          
          if (urls.forall(url => new File(dateDir, download.targetName(url)).exists)) {
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
        
        download.downloadTo(datePage, dateDir) // creates index.html
        eachLine(new File(dateDir, "index.html"), line => links.foreach(link => if (line contains link) links -= link))
        
        // did we find them all?
        if (links.isEmpty)
        {
          // 3 - download all files
          println("date page '"+datePage+"' has all files ["+fileNames.mkString(",")+"]")
          
          for (url <- urls) download.downloadTo(url, dateDir)
          
          complete.createNewFile
          
          return
        }
        
        println("date page '"+datePage+"' has no links to ["+links.mkString(",")+"]")
      }
    }
    finally running.delete
    
    throw new Exception("found no date in "+mainPage+" with files "+fileNames.mkString(","))
  }
  
  private def eachLine(file : File, f : String => Unit) : Unit =
  {
    val source = Source.fromFile(file)(Codec.UTF8)
    try
    {
      for (line <- source.getLines) f(line)
    }
    finally source.close
  }
  
}

object DumpDownload {
  
  def dumpName(language: String) = language.replace('-', '_') + "wiki"
  
  /**
   * Suffix of the file that indicates that a download of the Wikipedia dump files in the
   * directory containing this file is running. Note that this file may also have been left
   * over by a previous crashed download process. TODO: use file locks etc.
   */
  def runningFile (baseDir : File, language : String) : File = {
    val name = dumpName(language)
    new File(baseDir, name+"/"+name+"-download-running")
  }
  
  /**
   * File that indicates that a download of the Wikipedia dump files in the directory containing 
   * this file is complete. Note that this does not mean that all possible files have been downloaded, 
   * only those that a certain download process was supposed to download.
   */
  def completeFile(baseDir : File, language : String, date : String) : File = {
    val name = dumpName(language)
    new File(baseDir, name+"/"+date+"/"+name+"-"+date+"-download-complete")
  }
        

}
package org.dbpedia.extraction.dump.download

import scala.collection.mutable.{Set,Map,HashSet}
import scala.collection.immutable.SortedSet
import java.net.{URL,URLConnection,MalformedURLException}
import java.io.{File,InputStream,IOException}
import scala.io.{Source,Codec}

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.util.zip.GZIPInputStream

/**
 * TODO: this class is too big. Move the unzip and retry concerns to separate classes.
 */
class DumpDownloader(baseUrl : URL, baseDir : File, retry : Retry, unzip : Boolean)
{
  def init : Unit =
  {
    if (! baseDir.exists && ! baseDir.mkdir) throw Usage("Target directory '"+baseDir+"' does not exist and cannot be created")
  }
  
  def download(urls : Traversable[URL]) : Traversable[File] =
  {
    urls.map(download(_, baseDir))
  }
  
  def downloadFiles(languages : Map[String, Set[String]]) : Traversable[File] =
  {
    // sort them to have reproducible behavior
    val keys = SortedSet.empty[String] ++ languages.keys
    keys.flatMap { key => 
      val done = keys.until(key)
      val todo = keys.from(key)
      println("done: "+done.size+" - "+done.mkString(","))
      println("todo: "+todo.size+" - "+keys.from(key).mkString(","))
      downloadFiles(key,languages(key)) 
    }
  }
  
  val DateLink = """<a href="(\d{8})/">""".r
  
  private def downloadFiles(language : String, fileNames : Set[String]) : Traversable[File] =
  {
    import Downloader._
    
    val dumpName = Downloader.dumpName(language)
    
    val mainPage = new URL(baseUrl, dumpName+"/") // here the server does NOT use index.html 
    val mainDir = new File(baseDir, dumpName)
    if (! mainDir.exists && ! mainDir.mkdirs) throw new Exception("Target directory '"+mainDir+"' does not exist and cannot be created")
    val running = runningFile(baseDir, language)
    if (! running.createNewFile) throw new Exception("Another process is downloading files to '"+mainDir+"' - stop that process and remove '"+running+"'")
    try
    {
      // 1 - find all dates on the main page, sort them latest first
      var dates = SortedSet.empty(Ordering[String].reverse)
      
      download(mainPage, mainDir) // creates index.html, although it does not exist on the server
      eachLine(new File(mainDir, "index.html"), line => DateLink.findAllIn(line).matchData.foreach(dates += _.group(1)))
      
      // 2 - find date page that has all files we want
      for (date <- dates) // implicit conversion
      {
        val datePage = new URL(mainPage, date+"/index.html") // here the server uses index.html
        val dateDir = new File(mainDir, date)
        if (! dateDir.exists && ! dateDir.mkdirs) throw new Exception("Target directory '"+dateDir+"' does not exist and cannot be created")
        
        val complete = completeFile(baseDir, language, date)
        
        var files = for (fileName <- fileNames) yield new File(dateDir, dumpName+"-"+date+"-"+unzipped(fileName)._1)
        if (complete.exists) {
          // Previous download process said that this dir is complete. Note that we MUST check the
          // 'complete' file - the previous download may have crashed before all files were fully
          // downloaded. Checking that the downloaded files exist is necessary but not enough.
          
          if (files.forall(_.exists)) {
            println("did not download any files to '"+dateDir+"' - all files already complete")
            return files // yes, all files are there
          } 
          
          // Some files are missing. Maybe previous process was configured for different files.
          // Download the files that are missing or have the wrong timestamp. Delete 'complete' 
          // file first in case this download crashes. 
          complete.delete 
        }
        
        // all the links we need
        val links = fileNames.map("<a href=\"/"+dumpName+"/"+date+"/"+dumpName+"-"+date+"-"+_+"\">")
        
        download(datePage, dateDir)
        // Note: removing elements while iterating is scary but seems to work...
        eachLine(new File(dateDir, "index.html"), line => links.foreach(link => if (line contains link) links -= link))
        
        // did we find them all?
        if (links.isEmpty)
        {
          // 3 - download all files
          println("date page '"+datePage+"' has all files ["+fileNames.mkString(",")+"]")
          
          files = for (fileName <- fileNames) yield download(new URL(baseUrl, dumpName+"/"+date+"/"+dumpName+"-"+date+"-"+fileName), dateDir)
          complete.createNewFile
          
          return files
        }
        
        println("date page '"+datePage+"' has no links to ["+links.mkString(",")+"]")
      }
    }
    finally running.delete
    
    throw new Exception("found no date in "+mainPage+" with files "+fileNames.mkString(","))
  }
  
  private def unzipped(name : String) : (String, InputStream => InputStream) = 
  {
    if (unzip) 
    {
      val dot = name.lastIndexOf('.')
      val ext = name.substring(dot + 1)
      if(unzippers.contains(ext)) return (name.substring(0, dot), unzippers(ext))
    }
    
    (name, identity)
  }
  
  def download(url : URL, dir : File) : File =
  {
    val path = url.getPath
    var part = path.substring(path.lastIndexOf('/') + 1)
    if (part.isEmpty) part = "index.html"
    
    var (name, unzipper) = unzipped(part)
    
    val file = new File(dir, name)
    
    retry {
      println("downloading '"+url+"' to '"+file+"'")
      val logger = new ByteLogger(1 << 20) // create it now to start the clock
      val getStream = { conn : URLConnection =>
        logger.length = getContentLength(conn)
        unzipper(new CountingInputStream(conn.getInputStream, logger))
      }
      val downloader = new FileDownloader(url, file, getStream)
      if (! downloader.download) println("did not download '"+url+"' to '"+file+"' - file is up to date")
      
      return file
    } log { (attempt, max, ioex) =>
      if (! ioex.isInstanceOf[IOException]) throw ioex
      println(attempt+" of "+max+" attempts to download '"+url+"' to '"+file+"' failed - "+ioex)
    }
  }
  
  private def getContentLength(conn : URLConnection) : Long =
  getContentLengthMethod match
  {
    case Some(method) => method.invoke(conn).asInstanceOf[Long]
    case None => conn.getContentLength
  }
  
  // Mew method in JDK 7. In JDK 6, files >= 2GB show size -1
  private lazy val getContentLengthMethod =
  try { Some(classOf[URLConnection].getMethod("getContentLengthLong")) }
  catch { case nme : NoSuchMethodException => None }
  
  private val unzippers = Map[String, InputStream => InputStream] (
      "gz" -> { new GZIPInputStream(_) }, 
      "bz2" -> { new BZip2CompressorInputStream(_) } 
  )
  
  private def eachLine( file : File, f : String => Unit ) : Unit =
  {
    val source = Source.fromFile(file)(Codec.UTF8)
    try
    {
      for (line <- source.getLines) f(line)
    }
    finally source.close
  }
  
}

object Downloader {
  
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
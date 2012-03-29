package org.dbpedia.extraction.dump.download

import scala.collection.mutable.{Set,Map,HashSet,ArrayBuffer}
import scala.collection.immutable.SortedSet
import java.net.{URL,URLConnection,MalformedURLException}
import java.io.{File,InputStream,IOException}
import scala.io.{Source,Codec}
import java.util.TreeSet
import java.util.Collections.reverseOrder
import scala.collection.JavaConversions.asScalaSet
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.util.zip.GZIPInputStream

/**
 * TODO: this class is too big. Move the unzip and retry concerns to separate classes.
 */
class Downloader(baseUrl : URL, baseDir : File, retryMax : Int, retryMillis : Int, unzip : Boolean)
{
  def init : Unit =
  {
    if (! baseDir.exists && ! baseDir.mkdir) throw Usage("Target directory '"+baseDir+"' does not exist and cannot be created")
  }
  
  def download(urls : Traversable[URL]) : Traversable[File] =
  {
    urls.map(download(_, baseDir))
  }
  
  def resolveRanges(csvUrl : URL, ranges : Map[(Int,Int), Set[String]], languages : Map[String, Set[String]]) : Unit =
  {
    val csvFile = download(csvUrl, baseDir)
    
    // Note: the file is in ASCII, any non-ASCII chars are XML-encoded like '&#231;'. 
    // There is no Codec.ASCII, but UTF-8 also works for ASCII. Luckily we don't use 
    // these non-ASCII chars anyway, so we don't have to unescape them.
    // TODO: the CSV file URL is configurable, so the encoding should be too.
    println("parsing "+csvFile)
    val wikis = WikiInfo.fromFile(csvFile, Codec.UTF8)
    
    // Note: we don't have to download the file, but it seems nicer.
    // val wikis = WikiInfo.fromURL(csvUrl, Codec.UTF8)
    
    // for all wikis in one of the desired ranges...
    for (((from, to), files) <- ranges; wiki <- wikis; if (from <= wiki.pages && wiki.pages <= to))
    {
      // ...add files for this range to files for this language
      languages.getOrElseUpdate(wiki.language, new HashSet[String]) ++= files
    }
  }
  
  def downloadFiles(languages : Map[String, Set[String]]) : Traversable[File] =
  {
    // sort them to have reproducible behavior
    val keys = SortedSet.empty[String] ++ languages.keys
    keys.flatMap { key => 
      println("done: "+keys.until(key).mkString(","))
      println("todo: "+keys.from(key).mkString(","))
      downloadFiles(key,languages(key)) 
    }
  }
  
  val DateLink = """<a href="(\d{8})/">""".r
  
  private def downloadFiles(language : String, files : Set[String]) : Traversable[File] =
  {
    val name = dumpName(language)
    
    val mainPage = new URL(baseUrl, name+"/")
    
    // 1 - find all dates on the main page, sort them latest first
    
    // there is no mutable sorted set in Scala (yet) - use Java's TreeSet. see http://www.scala-lang.org/node/6484
    val dates = new TreeSet[String](reverseOrder[String])
    
    eachLine(mainPage, line => DateLink.findAllIn(line).matchData.foreach(m => dates.add(m.group(1))))
    
    // 2 - find date page that has all files we want
    for (date <- dates) // implicit conversion
    {
      val datePage = new URL(mainPage, date+"/")
      
      // all the links we need
      val links = files.map("<a href=\"/"+name+"/"+date+"/"+name+"-"+date+"-"+_+"\">")
      
      // Note: removing elements while iterating is scary but seems to work...
      eachLine(datePage, line => links.foreach(link => if (line contains link) links -= link))
      
      // did we find them all?
      if (links.isEmpty)
      {
        // 3 - download all files
        println("date page '"+datePage+"' has all files ["+files.mkString(",")+"]")
        return downloadFiles(language, date, files)
      }
      
      println("date page '"+datePage+"' has no links to ["+links.mkString(",")+"]")
    }
    
    throw new Exception("found no date in "+mainPage+" with files "+files.mkString(","))
  }
  
  private def downloadFiles(language : String, date : String, files : Set[String]) : Traversable[File] =
  {
    val name = dumpName(language)
    val dir = new File(baseDir, "/"+name+"/"+date)
    if (! dir.exists && ! dir.mkdirs) throw new Exception("Target directory '"+dir+"' does not exist and cannot be created")
    files.map(file => download(new URL(baseUrl, name+"/"+date+"/"+name+"-"+date+"-"+file), dir))
  }
  
  private def download(url : URL, dir : File) : File =
  {
    val path = url.getPath
    var name = path.substring(path.lastIndexOf('/') + 1)
    if (name.isEmpty) throw Usage("cannot download '"+url+"' to a file")
    
    var unzipper : InputStream => InputStream = identity
    if (unzip)
    {
      val dot = name.lastIndexOf('.')
      val ext = name.substring(dot + 1)
      if(unzippers.contains(ext))
      {
        name = name.substring(0, dot)
        unzipper = unzippers(ext)
      }
    }
    
    val file = new File(dir, name)
    
    var retry = 0
    while (true)
    {
      try
      {
        println("downloading '"+url+"' to '"+file+"'")
        val logger = new ByteLogger(1 << 20) // create it now to start the clock
        val getStream = { conn : URLConnection =>
          logger.length = getContentLength(conn)
          unzipper(new CountingInputStream(conn.getInputStream, logger))
        }
        val downloader = new FileDownloader(url, file, getStream)
        if (! downloader.download) println("did not download '"+url+"' to '"+file+"' - file is up to date")
        
        return file
      }
      catch
      {
        case ioe : IOException =>
          retry += 1
          println(retry+" of "+retryMax+" attempts to download '"+url+"' to '"+file+"' failed - "+ioe)
          if (retry >= retryMax) throw ioe
          Thread.sleep(retryMillis)
      }
    }
    
    throw new Exception("can't get here, but the scala compiler doesn't know")
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
  
  private val unzippers = Map[String, InputStream => InputStream]("gz" -> { in => in }, "bz2" -> bunzipper)
  
  private def gunzipper( in : InputStream ) : InputStream =
  {
    new GZIPInputStream(in)
  }
  
  private def bunzipper( in : InputStream ) : InputStream =
  {
    new BZip2CompressorInputStream(in)
  }
  
  private def eachLine( url : URL, f : String => Unit ) : Unit =
  {
    val source = Source.fromURL(url)(Codec.UTF8)
    try
    {
      for (line <- source.getLines) f(line)
    }
    finally source.close
  }
  
  private def dumpName(language: String) = language.replace('-', '_') + "wiki"
  
}

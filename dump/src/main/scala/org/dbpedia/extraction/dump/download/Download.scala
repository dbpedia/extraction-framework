package org.dbpedia.extraction.dump.download

import java.io.File
import scala.io.Codec
import java.net.Authenticator
import scala.collection.mutable.HashSet
import org.dbpedia.extraction.util.{WikiInfo,Language,ProxyAuthenticator}
import org.dbpedia.extraction.util.Language.wikiCodeOrdering
import scala.collection.immutable.SortedSet

object Download extends DownloadConfig
{
  /** name of marker file in wiki directory */
  val Started = "download-started"
  
  /** name of marker file in wiki date directory */
  // Note: also used in CreateMappingStats.scala
  // TODO: move this constant to core, or use config value
  val Complete = "download-complete"
    
  def main(args: Array[String]) : Unit =
  {
    parse(null, args)
    
    if (baseDir == null) throw Usage("No target directory")
    if ((languages.nonEmpty || ranges.nonEmpty) && baseUrl == null) throw Usage("No base URL")
    if (languages.isEmpty && ranges.isEmpty) throw Usage("No files to download")
    if (! baseDir.exists && ! baseDir.mkdir) throw Usage("Target directory '"+baseDir+"' does not exist and cannot be created")
    Authenticator.setDefault(new ProxyAuthenticator())
    
    class Downloader extends FileDownloader with Counter with LastModified with Retry {
      val progressStep = 1L << 21 // 2M
      val progressPretty = Download.this.progressPretty
      val retryMax = Download.this.retryMax
      val retryMillis = Download.this.retryMillis
    }
    
    val downloader = 
      if (unzip) new Downloader with Unzip 
      else new Downloader 
    
    // resolve page count ranges to languages
    if (ranges.nonEmpty)
    {
      val listFile = new File(baseDir, WikiInfo.FileName)
      downloader.downloadFile(WikiInfo.URL, listFile)
      
      // Note: the file is in ASCII, any non-ASCII chars are XML-encoded like '&#231;'. 
      // There is no Codec.ASCII, but UTF-8 also works for ASCII. Luckily we don't use 
      // these non-ASCII chars anyway, so we don't have to unescape them.
      println("parsing "+listFile)
      val wikis = WikiInfo.fromFile(listFile, Codec.UTF8)
      
      // for all wikis in one of the desired ranges...
      for (((from, to), files) <- ranges; wiki <- wikis; if (from <= wiki.pages && wiki.pages <= to))
      {
        // ...add files for this range to files for this language
        languages.getOrElseUpdate(wiki.language, new HashSet[String]) ++= files
      }
    }
    
    // sort them to have reproducible behavior
    val keys = SortedSet.empty[Language] ++ languages.keys
    keys.foreach { key => 
      val done = keys.until(key)
      val todo = keys.from(key)
      println("done: "+done.size+" - "+done.mkString(","))
      println("todo: "+todo.size+" - "+keys.from(key).mkString(","))
      new LanguageDownloader(baseUrl, baseDir, wikiName, key, languages(key), downloader).downloadDates(dateRange, dumpCount)
    }
  }
  
}



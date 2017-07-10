package org.dbpedia.extraction.dump.download

import java.io.File

import scala.io.Codec
import java.net.Authenticator

import org.dbpedia.extraction.util.{ProxyAuthenticator, WikiInfo}

object Download
{
  /** name of marker file in wiki directory */
  val Started = "download-started"
  
  /** name of marker file in wiki date directory */
  // Note: also used in CreateMappingStats.scala
  // TODO: move this constant to core, or use config value
  val Complete = "download-complete"
    
  def main(args: Array[String]) : Unit =
  {
    val config = new DownloadConfig(args.head)

    if (config.languages.isEmpty) throw Usage("No files to download, because no languages were defined.")
    if (! config.dumpDir.exists && ! config.dumpDir.mkdir) throw Usage("Target directory '"+config.dumpDir+"' does not exist and cannot be created")
    Authenticator.setDefault(new ProxyAuthenticator())
    
    class Downloader extends FileDownloader with Counter with LastModified with Retry {
      val progressStep: Long = 1L << 21 // 2M
      val progressPretty: Boolean = true
      val retryMax: Int = config.retryMax
      val retryMillis: Int = config.retryMillis
    }
    
    val downloader = 
      if (config.unzip) new Downloader with Unzip
      else new Downloader


    val listFile = new File(config.dumpDir, WikiInfo.FileName)
    downloader.downloadFile(WikiInfo.URL, listFile)
    val wikis = WikiInfo.fromFile(listFile, Codec.UTF8)

    
    // sort them to have reproducible behavior
      config.languages.foreach { lang =>
        val ld = new LanguageDownloader(config.baseUrl, config.dumpDir, config.wikiName, lang, config.source.map(x => (x, true)), downloader)
        if(!ld.downloadMostRecent())
          System.err.println("An error occurred while trying to download the most recent dump file for language: " + lang.wikiCode)
      }
  }
  
}



package org.dbpedia.extraction.dump.download

import java.io.File

import scala.io.Codec
import java.net.Authenticator

import org.dbpedia.extraction.util.{ProxyAuthenticator, WikiInfo}

object Download
{

  def main(args: Array[String]) : Unit =
  {
    assert(args.length == 1,"Download needs a single parameter: the path to the pertaining properties file (see: download.10000.properties).")
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
        val ld = new LanguageDownloader(config, downloader, lang)
        config.dumpDate match{
          case Some(d) => if(!ld.downloadDate(d))
            System.err.println("An error occurred while trying to download a dump file (" + d + ")for language: " + lang.wikiCode)
          case None => if(!ld.downloadMostRecent())
            System.err.println("An error occurred while trying to download the most recent dump file for language: " + lang.wikiCode)
        }
      }
  }
  
}



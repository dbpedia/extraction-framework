package org.dbpedia.extraction.dump.download

import scala.io.Codec
import scala.collection.mutable.HashSet

object Download extends DownloadConfig
{
  /** name of marker file in wiki directory */
  val Running = "download-running"
  
  /** name of marker file in wiki date directory */
  // Note: also used in CreateMappingStats.scala
  // TODO: move this constant to core, or use config value
  val Complete = "download-complete"
    
  def main(args: Array[String]) : Unit =
  {
    parse(null, args)
    
    if (baseDir == null) throw Usage("No target directory")
    if ((languages.nonEmpty || ranges.nonEmpty) && baseUrl == null) throw Usage("No base URL")
    if (ranges.nonEmpty && csvUrl == null) throw Usage("No CSV file URL")
    if (languages.isEmpty && ranges.isEmpty && others.isEmpty) throw Usage("No files to download")
    if (! baseDir.exists && ! baseDir.mkdir) throw Usage("Target directory '"+baseDir+"' does not exist and cannot be created")
    
    class Decorator extends FileDownloader with Counter with LastModified with Retry {
      val progressStep = 1L << 20 // count each MB
      val progressPretty = Download.this.progressPretty
      val retryMax = Download.this.retryMax
      val retryMillis = Download.this.retryMillis
    }
    
    val download = if (unzip) new Decorator with Unzip else new Decorator 
    
    // download other files, may be none
    if (others.nonEmpty)
    {
      others.map(download.downloadTo(_, baseDir))
    }
    
    // resolve page count ranges to languages
    if (ranges.nonEmpty)
    {
      val csvFile = download.downloadTo(csvUrl, baseDir)
      
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
    
    // download the dump files, if any
    if (languages.nonEmpty)
    {
      new DumpDownload(baseUrl, baseDir, download).downloadFiles(languages)
    }
  }
  
}



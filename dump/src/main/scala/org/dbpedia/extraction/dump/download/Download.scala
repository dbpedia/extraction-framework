package org.dbpedia.extraction.dump.download

import scala.io.Codec
import scala.collection.mutable.HashSet

object Download
{
  def main(args: Array[String]) : Unit =
  {
    val cfg = new Config
    cfg.parse(null, args)
    cfg.validate
    
    val downloader = new DumpDownloader(cfg.baseUrl, cfg.baseDir, new Retry(cfg.retryMax, cfg.retryMillis), cfg.unzip)
    
    downloader.init
    
    // download other files, may be none
    if (cfg.others.nonEmpty)
    {
      downloader.download(cfg.others)
    }
    
    // resolve page count ranges to languages
    if (cfg.ranges.nonEmpty)
    {
      val csvFile = downloader.download(cfg.csvUrl, cfg.baseDir)
      
      // Note: the file is in ASCII, any non-ASCII chars are XML-encoded like '&#231;'. 
      // There is no Codec.ASCII, but UTF-8 also works for ASCII. Luckily we don't use 
      // these non-ASCII chars anyway, so we don't have to unescape them.
      // TODO: the CSV file URL is configurable, so the encoding should be too.
      println("parsing "+csvFile)
      val wikis = WikiInfo.fromFile(csvFile, Codec.UTF8)
      
      // Note: we don't have to download the file, but it seems nicer.
      // val wikis = WikiInfo.fromURL(csvUrl, Codec.UTF8)
      
      // for all wikis in one of the desired ranges...
      for (((from, to), files) <- cfg.ranges; wiki <- wikis; if (from <= wiki.pages && wiki.pages <= to))
      {
        // ...add files for this range to files for this language
        cfg.languages.getOrElseUpdate(wiki.language, new HashSet[String]) ++= files
      }
    }
    
    // download the dump files, if any
    if (cfg.languages.nonEmpty)
    {
      downloader.downloadFiles(cfg.languages)
    }
  }
  
}



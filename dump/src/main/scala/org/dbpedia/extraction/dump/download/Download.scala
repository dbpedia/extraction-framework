package org.dbpedia.extraction.dump.download

object Download
{
  def main(args: Array[String]) : Unit =
  {
    val cfg = new Config
    cfg.parse(null, args)
    cfg.validate
    
    val downloader = new Downloader(cfg.baseUrl, cfg.baseDir, new Retry(cfg.retryMax, cfg.retryMillis), cfg.unzip)
    
    downloader.init
    
    // download other files, may be none
    if (cfg.others.nonEmpty)
    {
      downloader.download(cfg.others)
    }
    
    // resolve page count ranges to languages
    if (cfg.ranges.nonEmpty)
    {
      downloader.resolveRanges(cfg.csvUrl, cfg.ranges, cfg.languages)
    }
    
    // download the dump files, if any
    if (cfg.languages.nonEmpty)
    {
      downloader.downloadFiles(cfg.languages)
    }
  }
}



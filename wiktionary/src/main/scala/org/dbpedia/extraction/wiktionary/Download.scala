package org.dbpedia.extraction.wiktionary

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.net.URL
import java.io._
import java.util.logging.Logger
import org.dbpedia.extraction.util.StringUtils._
import org.dbpedia.extraction.util.RichFile.wrapFile
import io.{Codec, Source}

/**
 * Downloads Wikipedia dumps.
 */
object Download
{
    /**
     * Downloads and updates a list of dumps
     */
    def download(dumpDir : File, wikiCodes : List[String])
    {
        DumpDownloader.download("commons" :: wikiCodes, dumpDir)
    }

    /**
     * Downloads and updates all dumps with a minimum number of "good" articles
     */
    def download(dumpDir : File, minGoodArticleCount : Int = 10000) : Unit =
    {
        //Retrieve list of available Wikipedias
        val wikiCodes = WikiInfo.download.filter(_.goodArticleCount >= minGoodArticleCount).map(_.prefix)

        //Download all
        DumpDownloader.download("commons" :: wikiCodes, dumpDir)
    }

/**
 * Downloads Wikipedia dumps from the server.
 */
private object DumpDownloader
{
    private val logger = Logger.getLogger(Download.getClass.getName)

    // The URI where the Wikipedia dumps can be found
    private val downloadUri = "http://download.wikimedia.org"

    // The dump files we are interested in
    private val dumpFiles = List("pages-articles.xml.bz2")

    /**
     * Downloads a list of MediaWiki dumps.
     */
    def download(wikiCodes : List[String], outputDir : File) : Unit =
    {
        for(wikiCode <- wikiCodes)
        {
            downloadWiki(wikiCode, new File(outputDir + "/" + wikiCode))
        }
    }

    /**
     * Downloads one MediaWiki dump, which consist of multiple dump files.
     */
    private def downloadWiki(wikiCode : String, dir : File) : Unit =
    {
        val name = wikiCode.replace('-', '_') + "wiki"
        val date = findMostRecentDate(wikiCode).getOrElse(throw new Exception("No complete dump of " + wikiCode + " found"))
        val url = downloadUri + "/" + name + "/" + date + "/"

        //Delete outdated dumps
        for(subDirs <- Option(dir.listFiles()); subDir <- subDirs; if subDir.getName != date.toString && subDir.getName.matches("\\d{8}") )
        {
            logger.info("Deleting outdated dump " + subDir.getName)
            subDir.delete(true)
        }

        //Generate a list of the expected links to the dump files
        val dumpLinks = dumpFiles.map(dumpFile => name + "-" + date + "-" + dumpFile)

        //Retrieve download page
        val downloadPage = Source.fromURL(new URL(url))

        //Create output directory
        val outputDir = new File(dir + "/" + date)
        outputDir.mkdirs()

        //Download all links
        for(link <- dumpLinks)
        {
            val xmlDumpFile = new File(outputDir + "/" + link.substring(0, link.size - 4))
            if(xmlDumpFile.exists)
            {
                logger.info("Found dump file " + xmlDumpFile + " on disk")
            }
            else
            {
                logger.info("Downloading " + link + " to disk")

                //Download to a temporary file which will be renamed to the destination file afterwards
                val tempFile = new File(xmlDumpFile + ".tmp")

                downloadFile(new URL(url + "/" + link), tempFile)

                if(!tempFile.renameTo(xmlDumpFile))
                {
                    logger.warning("Could not rename file " + tempFile + " to " + xmlDumpFile)
                }
            }
        }
    }

    /**
     * Finds the most recent dump, which provides links to all requested dump files.
     *
     * @param wiki The MediaWiki
     * @return The date of the found dump
     */
    private def findMostRecentDate(wikiCode : String) : Option[Int] =
    {
        val name = wikiCode.replace('-', '_') + "wiki"
        val uri = downloadUri + "/" + name

        //Retrieve download overview page
        val overviewPage = Source.fromURL(new URL(uri)).getLines().mkString

        //Get available dates
        val availableDates = "\\d{8}".r.findAllIn(overviewPage).toList.collect{case IntLiteral(i) => i}

        //Find the first date for which the download page contains all requested dump files
        availableDates.sortWith(_ > _).find(date =>
        {
            //Retrieve the download page for this date
            val downloadPage = Source.fromURL(new URL(uri + "/" + date)).getLines().mkString

            //Generate a list of the expected links to the dump files
            val dumpLinks = dumpFiles.map(dumpFile => name + "-" + date + "-" + dumpFile)

            //Check if this page contains all links
            dumpLinks.forall(link => downloadPage.contains("<a href=\"" + uri + "/" + date + "/" + link))
        })
    }

    /**
     * Downloads a MediaWiki dump file.
     * The file is uncompressed on the fly.
     */
    private def downloadFile(url : URL, file : File) : Unit =
    {
        require(url.toString.endsWith(".bz2"), "Unsupported extension")

        val inputStream = new BZip2CompressorInputStream(url.openStream(), true)
        val outputStream = new FileOutputStream(file)
        val buffer = new Array[Byte](65536)

        var totalBytes = 0L
        val startTime = System.nanoTime
        var lastLogTime = 0L

        try
        {
            while(true)
            {
                val bytesRead = inputStream.read(buffer)
                if(bytesRead == -1) return;
                outputStream.write(buffer, 0, bytesRead)

                //TODO BZip2CompressorInputStream.count

                totalBytes += bytesRead
                val kb = totalBytes / 1024L
                val time = System.nanoTime
                if(time - lastLogTime > 1000000000L)
                {
                    lastLogTime = time
                    println("Uncompressed: " + kb + " KB (" + (kb.toDouble / (time - startTime) * 1000000000.0).toLong + "kb/s)")
                }
            }
        }
        finally
        {
            inputStream.close()
            outputStream.close()
        }
    }
}

/**
* Informations about a MediaWiki.
*/
private case class WikiInfo(prefix : String, language : String, goodArticleCount : Int, totalArticleCount : Int)
{
    override def toString = prefix.replace('-', '_') + ".wikipedia.org"
}

/**
* Retrieves a list of all available Wikipedias.
*/
private object WikiInfo
{
   private val wikiInfoFile = new URL("http://s23.org/wikistats/wikipedias_csv.php")

   /**
    * Retrieves a list of all available Wikipedias.
    */
   def download : List[WikiInfo] =
   {
       val source = Source.fromURL(wikiInfoFile)(Codec.UTF8)
       try
       {
           //Each line (except the first) contains information about a Wikipedia instance
           return source.getLines.toList.tail.filter(!_.isEmpty).map(loadWikiInfo)
       }
       finally
       {
           source.close()
       }
   }

   /**
    * Loads a WikiInfo from a line.
    */
   private def loadWikiInfo(line : String) : WikiInfo = line.split(',').map(_.trim).toList match
   {
       case rank :: id :: prefix :: language :: loclang :: good :: total :: edits :: views :: admins :: users ::
               activeusers :: images :: stubratio :: timestamp :: Nil => new WikiInfo(prefix, language, good.toInt, total.toInt)
       case _ => throw new IllegalArgumentException("Unexpected format in line '" + line + "'")
   }
}

}

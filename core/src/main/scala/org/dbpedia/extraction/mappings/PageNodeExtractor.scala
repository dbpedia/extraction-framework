package org.dbpedia.extraction.mappings

import java.io.{FileWriter, File}

import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._

import scala.collection.mutable

/**
 * Extractors are mappings that extract data from a PageNode.
 * Necessary to get some type safety in CompositeExtractor: 
 * Class[_ <: Extractor] can be checked at runtime, but Class[_ <: Mapping[PageNode]] can not.
 */
trait PageNodeExtractor extends Extractor[PageNode] {


  def extract(page:PageNode):Seq[Quad] = {
    this.extract(page, page.uri, new PageContext())
  }

  private var failedPages = Map[Language, scala.collection.mutable.Map[(Long, WikiTitle), Throwable]]()
  private var writer: FileWriter = null

  /**
    * A map for failed pages, which could be used for a better way to record extraction fails than just a simple console output.
    *
    * @return the failed pages (id, title) for every Language
    */
  def listFailedPages: Map[Language, mutable.Map[(Long, WikiTitle), Throwable]] = failedPages

  /**
    * define the log file destination
    *
    * @param logFile the target file
    * @param preamble the optional first line of the log file
    */
  protected def setLogFile(logFile: File, preamble: String = null): Unit ={
    writer = new FileWriter(logFile)
    if(preamble != null)
      writer.append("# " + preamble + "\n")
  }

  /**
    * adds a new fail record for a wikipage which failed to extract; Optional: write fail to log file (if this has been set before)
    *
    * @param id - page id
    * @param title - WikiTitle of page
    * @param exception  - the Throwable responsible for the fail
    */
  protected def recordFailedPage(id: Long, title: WikiTitle, exception: Throwable): Unit={
    failedPages.get(title.language) match{
      case Some(map) => map += ((id,title) -> exception)
      case None =>  failedPages += title.language -> mutable.Map[(Long, WikiTitle), Throwable]((id, title) -> exception)
    }
    if(writer != null) {
      writer.append("page " + id + ": " + title.encoded + ": " + exception.getMessage + "\n")
      for (ste <- exception.getStackTrace)
        writer.write(ste.toString + "\n")
    }
    System.err.println(title.language.wikiCode + ": Extraction failed for page " + id + ": " + title.encoded)
  }

  override def finalizeExtractor(): Unit ={
    writer.close()
  }
}
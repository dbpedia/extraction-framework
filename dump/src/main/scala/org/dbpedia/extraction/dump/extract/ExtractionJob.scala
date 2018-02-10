package org.dbpedia.extraction.dump.extract

import org.apache.log4j.Level
import org.dbpedia.extraction.config.{DefaultEntry, ExtractionLogger, ExtractionRecorder, RecordEntry}
import org.dbpedia.extraction.config.provenance.Dataset
import org.dbpedia.extraction.destinations.Destination
import org.dbpedia.extraction.mappings.WikiPageExtractor
import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.wikiparser.{Namespace, PageNode, WikiPage}

/**
 * Executes a extraction.
 *
 * @param extractor The Extractor
 * @param source The extraction source
 * @param namespaces Only extract pages in these namespaces
 * @param destination The extraction destination. Will be closed after the extraction has been finished.
 * @param language the language of this extraction.
 */
class ExtractionJob(
   extractor: WikiPageExtractor,
   source: Source,
   val namespaces: Set[Namespace],
   val destination: Destination,
   val language: Language)
{
  private val logger = ExtractionLogger.getLogger(getClass, language)
  def datasets: Set[Dataset] = extractor.datasets

  private val workers = SimpleWorkers { page: WikiPage =>
    try {
      if (namespaces.contains(page.title.namespace)) {
        val graph = extractor.extract(page, page.uri)
        graph.foreach(q => logger.record(q))
        destination.write(graph)
      }
      //if the internal extraction process of this extractor yielded extraction records (e.g. non critical errors etc.), those will be forwarded to the ExtractionRecorder, else a new record is produced
      val records = page.recordEntries match{
        case seq :Seq[RecordEntry[PageNode]] if seq.nonEmpty => seq
        case _ =>  Seq[RecordEntry[PageNode]]()
      }
      //forward all records to the recorder
      logger.record(records:_*)
    } catch {
      case ex: Exception =>
        page.recordException(ex)
        logger.record(page)
    }
  }
  
  def run(): Unit =
  {
    extractor.initializeExtractor()
    destination.open()
    workers.start()

    try {
      for (page <- source)
        workers.process(page)

      logger.record(new DefaultEntry("finished extraction after {page} pages with {mspp} per page", null, language, Level.INFO))
    }
    catch {
      case ex : Throwable =>
        logger.error(ex)
    } finally {
      workers.stop()
      destination.close()
      extractor.finalizeExtractor()
    }
  }
}

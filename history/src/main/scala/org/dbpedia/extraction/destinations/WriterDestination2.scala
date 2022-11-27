package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.config.provenance.Dataset
import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.mappings.BadQuadException
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{ExtractionRecorder, ExtractionRecorder2}
import org.dbpedia.extraction.wikiparser.{WikiPage, WikiPageWithRevisions}

import java.io.Writer


/**
 * Writes quads to a writer.
 */
class WriterDestination2(factory: () => Writer, formatter : Formatter, extractionRecorder: ExtractionRecorder2[WikiPageWithRevisions] = null, dataset : Dataset = null)
extends Destination
{
  private var writer: Writer = null
  
  override def open() = {
    if(writer == null) //to prevent errors when called twice
    {
      writer = factory()
      writer.write(formatter.header)
    }
  }
  
  /**
   * Note: using synchronization here is not strictly necessary (writers should be thread-safe),
   * but without it, different sequences of quads will be interleaved, which is harder to read
   * and makes certain parsing optimizations impossible.
   */
  override def write(graph : Traversable[Quad]) = synchronized {
    for(quad <- graph) {
      val formatted = formatter.render(quad)
      if(extractionRecorder != null) {
        if(formatted.trim.startsWith("#")){
          if(formatted.contains("BAD URI:"))
            //TODO create trait 'Recordable'
            extractionRecorder.failedRecord(quad.toString(), null, new BadQuadException(formatted))
        }
        else if(dataset != null)
          extractionRecorder.increaseAndGetSuccessfulTriples(dataset)
      }
      writer.write(formatted)
    }
  }

  override def close() = {
    if(writer != null) {
      writer.write(formatter.footer)
      writer.close()
    }
  }
}

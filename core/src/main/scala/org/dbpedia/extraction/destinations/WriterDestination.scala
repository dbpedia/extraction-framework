package org.dbpedia.extraction.destinations

import java.io.Writer

import org.dbpedia.extraction.config.ExtractionRecorder
import org.dbpedia.extraction.config.provenance.Dataset
import org.dbpedia.extraction.destinations.formatters.Formatter
import org.dbpedia.extraction.mappings.BadQuadException
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language


/**
 * Writes quads to a writer.
 */
class WriterDestination(factory: () => Writer, formatter : Formatter, extractionRecorder: ExtractionRecorder[Quad] = null, dataset : Dataset = null)
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
            extractionRecorder.failedRecord(quad, new BadQuadException(formatted), Language.getOrElse(quad.language, Language.None))
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

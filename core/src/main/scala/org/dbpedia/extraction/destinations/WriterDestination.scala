package org.dbpedia.extraction.destinations

import java.io.Writer
import org.dbpedia.extraction.destinations.formatters.Formatter

/**
 * Writes quads to a writer.
 * 
 * @param called in open() to obtain the writer.
 */
class WriterDestination(factory: () => Writer, formatter : Formatter)
extends Destination
{
  private var writer: Writer = null
  
  override def open() = {
    writer = factory()
    writer.write(formatter.header)
  }
  
  /**
   * Note: using synchronization here is not strictly necessary (writers should be thread-safe),
   * but without it, different sequences of quads will be interleaved, which is harder to read
   * and makes certain parsing optimizations impossible.
   */
  override def write(graph : Seq[Quad]) = synchronized {
    for(quad <- graph) {
      writer.write(formatter.render(quad))
    }
  }

  override def close() = {
    writer.write(formatter.footer)
    writer.close()
  }
}

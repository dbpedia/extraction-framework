package org.dbpedia.extraction.destinations

import java.io.Writer
import org.dbpedia.extraction.destinations.formatters.Formatter

/**
 * Writes all statements to a writer.
 *
 * This class is thread-safe.
 *
 * @param formatter The formatter used to serialize the statements.
 */
class WriterDestination(writer: Writer, formatter : Formatter) extends Destination
{
    private var header = false

    private var closed = false

    override def write(graph : Seq[Quad]) = synchronized {
      
      if(closed) throw new IllegalStateException("Trying to write to a closed destination")

      if(! header) {
        writer.write(formatter.header)
        header = true
      }

      for(quad <- graph) {
        writer.write(formatter.render(quad))
      }
    }

    override def close() = synchronized {
      if (! header) writer.write(formatter.header)
      writer.write(formatter.footer)
      closed = true
    }
}

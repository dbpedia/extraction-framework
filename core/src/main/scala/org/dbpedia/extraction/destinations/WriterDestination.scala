package org.dbpedia.extraction.destinations

import java.io.Writer

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
        formatter.writeHeader(writer)
        header = true
      }

      for(quad <- graph) {
        formatter.write(writer, quad)
      }
    }

    override def close() = synchronized {
      if (! header) formatter.writeHeader(writer)
      formatter.writeFooter(writer)
      closed = true
    }
}

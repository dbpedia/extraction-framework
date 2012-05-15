package org.dbpedia.extraction.destinations

import java.io.StringWriter

/**
 * Writes all statements to a string.
 * This class is thread-safe.
 *
 * @param formatter The formatter used to serialize the statements. If no formatter is provided, the statements are written using the N-Triples format.
 */
class StringDestination(formatter : Formatter) extends Destination
{
    private val writer = new StringWriter()

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

    /**
     * Retrieves the formatted data as a string.
     * The returned data is only partial until the destination has been closed.
     */
    override def toString = synchronized { writer.toString }
}

package org.dbpedia.extraction.destinations

import formatters.NTriplesFormatter
import java.io.StringWriter

/**
 * Writes all statements to a string.
 * This class is thread-safe.
 *
 * @param formatter The formatter used to serialize the statements. If no formatter is provided, the statements are written using the N-Triples format.
 */
class StringDestination(formatter : Formatter) extends Destination
{
  def this() = this( new NTriplesFormatter )

	private val stringWriter = new StringWriter()

    private var headerWritten = false

    private var closed = false

	override def write(graph : Graph) = synchronized
    {
        if(closed) throw new IllegalStateException("Trying to write to a closed destination")

        if(!headerWritten)
        {
            formatter.writeHeader(stringWriter)
            headerWritten = true
        }

        for(quad <- graph.quads)
        {
            formatter.write(quad, stringWriter)
        }
    }

    override def close() = synchronized
    {
        if(!headerWritten) formatter.writeHeader(stringWriter)
        formatter.writeFooter(stringWriter)
        closed = true
    }

    /**
     * Retrieves the formatted data as a string.
     * The returned data is only partial until the destination has been closed.
     */
	override def toString = synchronized { stringWriter.toString }
}

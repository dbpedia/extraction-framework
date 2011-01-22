package org.dbpedia.extraction.destinations.formatters

import java.io.Writer
import org.dbpedia.extraction.destinations.{Quad, Formatter}

/**
 * Formats statements according to the N-Quads format.
 * See: http://sw.deri.org/2008/07/n-quads/
 */
class NQuadsFormatter extends Formatter
{
    override def write(quad : Quad, writer : Writer) : Unit =
    {
        writer.write(quad.renderNQuad + "\n")
    }
}

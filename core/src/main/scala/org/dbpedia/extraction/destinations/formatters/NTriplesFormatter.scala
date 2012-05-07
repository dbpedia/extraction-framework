package org.dbpedia.extraction.destinations.formatters

import java.io.Writer
import org.dbpedia.extraction.destinations.{Formatter, Quad}

/**
 * Formats statements according to the N-Triples format.
 * See: http://www.w3.org/TR/rdf-testcases/#ntriples
 */
class NTriplesFormatter extends TextFormatter
{
    override def write(quad : Quad, writer : Writer) : Unit =
    {
        writer.write(quad.render(turtle=false, quad=false))
    }
}

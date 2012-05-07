package org.dbpedia.extraction.destinations.formatters

import java.io.Writer
import org.dbpedia.extraction.destinations.{Quad, Formatter}

/**
 * Formats statements according to the N-Triples format, but with Turtle escaping rules.
 * See http://www.w3.org/TR/rdf-testcases/#ntriples and http://www.w3.org/TR/turtle/ .
 */
class TurtleTriplesFormatter extends TextFormatter
{
    override def write(quad : Quad, writer : Writer) : Unit =
    {
        writer.write(quad.render(turtle=true, quad=false))
    }
}

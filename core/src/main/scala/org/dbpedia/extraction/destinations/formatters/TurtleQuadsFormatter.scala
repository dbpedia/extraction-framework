package org.dbpedia.extraction.destinations.formatters

import java.io.Writer
import org.dbpedia.extraction.destinations.{Quad, Formatter}

/**
 * Formats statements according to the N-Quads format, but with Turtle escaping rules.
 * See http://sw.deri.org/2008/07/n-quads/ and http://www.w3.org/TR/turtle/ .
 */
class TurtleQuadsFormatter extends TextFormatter
{
    override def write(quad : Quad, writer : Writer) : Unit =
    {
        writer.write(quad.render(turtle=true, quad=true))
    }
}

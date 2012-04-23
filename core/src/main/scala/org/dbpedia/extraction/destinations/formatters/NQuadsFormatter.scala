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
        // use UNIX EOL. N-Triples doesn't care: http://www.w3.org/TR/rdf-testcases/#eoln
        // and it's probably better to be consistent instead of using the EOL of the platform
        // where the file was generated. These files are moved around a lot anyway.
        writer.write(quad.renderNQuad + "\n")
    }
}

package org.dbpedia.extraction.destinations.formatters

import java.io.Writer
import org.dbpedia.extraction.destinations.{Formatter, Quad}

/**
 * Formats statements according to the N-Triples format.
 * See: http://www.w3.org/TR/rdf-testcases/#ntriples
 */
class NTriplesFormatter extends Formatter
{
    override def write(quad : Quad, writer : Writer) : Unit =
    {
        // use UNIX EOL. N-Triples doesn't care: http://www.w3.org/TR/rdf-testcases/#eoln
        // and it's probably better to be consistent instead of using the EOL of the platform
        // where the file was generated. These files are moved around a lot anyway.
        writer.write(quad.renderNTriple + "\n")
    }
}

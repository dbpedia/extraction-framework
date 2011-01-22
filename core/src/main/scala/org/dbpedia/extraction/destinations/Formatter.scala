package org.dbpedia.extraction.destinations

import java.io.Writer

/**
 * Serializes statements.
 */
trait Formatter
{
    def writeHeader(writer : Writer) : Unit = {}
    def writeFooter(writer : Writer) : Unit = {}

    def write(quad : Quad, writer : Writer) : Unit
}

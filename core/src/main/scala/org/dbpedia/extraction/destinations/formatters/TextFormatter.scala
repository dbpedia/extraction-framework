package org.dbpedia.extraction.destinations.formatters

import org.dbpedia.extraction.destinations.Formatter
import java.io.Writer

/**
 * TODO: rename this class.
 * TODO: add functionality - the comments may contain more useful info
 */
trait TextFormatter extends Formatter {
    override def writeHeader(writer : Writer) : Unit = { writer.write("# started\n") }
    override def writeFooter(writer : Writer) : Unit = { writer.write("# complete\n") }
}
package org.dbpedia.extraction.destinations.formatters

import java.io.Writer
import org.dbpedia.extraction.destinations.{Quad, Formatter}
import scala.xml.Utility.escape
import java.net.URI

/**
 * Formats statements according to the TriX format.
 * See: http://www.hpl.hp.com/techreports/2004/HPL-2004-56.html
 */
class TriXFormatter(header : String = null) extends Formatter
{
    override def writeHeader(writer : Writer) : Unit =
    {
        if (header != null) writer.write(header)
        writer.write("<TriX xmlns=\"http://www.w3.org/2004/03/trix/trix-1/\">\n")
    }

    override def writeFooter(writer : Writer) : Unit =
    {
        writer.write("</TriX>\n")
    }

    override def write(quad : Quad, writer : Writer) : Unit =
    {
        // TODO: string + is relatively inefficient
        writer.write("  <graph>\n")
        writer.write("    <uri>" + escape(quad.context) + "</uri>\n")
        writer.write("    <triple>\n")

        writer.write("      <uri>" + escape(quad.subject) + "</uri>\n")
        writer.write("      <uri>" + escape(quad.predicate) + "</uri>\n")

        if(quad.datatype != null)
        {
            writer.write("      <typedLiteral datatype=\"" + escape(quad.datatype.uri) + "\">" + escape(quad.value) + "</typedLiteral>\n")
        }
        else
        {
            writer.write("      <uri>" + escape(quad.value) + "</uri>\n")
        }

        writer.write("    </triple>\n")
        writer.write("  </graph>\n")
    }
}

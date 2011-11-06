package org.dbpedia.extraction.destinations.formatters

import java.io.Writer
import org.dbpedia.extraction.destinations.{Quad, Formatter}
import scala.xml.Utility
import java.net.URI

/**
 * Formats statements according to the TriX format.
 * See: http://www.hpl.hp.com/techreports/2004/HPL-2004-56.html
 */
class TriXFormatter(stylesheetURI : URI = null) extends Formatter
{
    override def writeHeader(writer : Writer) : Unit =
    {
        writer.write("<?xml-stylesheet type=\"text/xsl\" href=\"" + stylesheetURI + "\"?>\n")
        writer.write("<TriX xmlns=\"http://www.w3.org/2004/03/trix/trix-1/\">\n")
    }

    override def writeFooter(writer : Writer) : Unit =
    {
        writer.write("</TriX>\n")
    }

    override def write(quad : Quad, writer : Writer) : Unit =
    {
        writer.write("  <graph>\n")
        writer.write("    <uri>" + encode(quad.context) + "</uri>\n")
        writer.write("    <triple>\n")

        writer.write("      <uri>" + encode(quad.subject) + "</uri>\n")
        writer.write("      <uri>" + encode(quad.predicate) + "</uri>\n")

        if(quad.datatype != null)
        {
            writer.write("      <typedLiteral datatype=\"" + encode(quad.datatype.uri) + "\">" + encode(quad.value) + "</typedLiteral>\n")
        }
        else
        {
            writer.write("      <uri>" + encode(quad.value) + "</uri>\n")
        }

        writer.write("    </triple>\n")
        writer.write("  </graph>\n")
    }

    private def encode(str : String) = str.map(c => Utility.Escapes.escMap.get(c).getOrElse(c)).mkString
}

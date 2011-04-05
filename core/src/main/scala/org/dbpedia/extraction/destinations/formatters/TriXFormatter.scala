package org.dbpedia.extraction.destinations.formatters

import java.io.Writer
import org.dbpedia.extraction.destinations.{Quad, Formatter, IriRef, TypedLiteral, PlainLiteral, LanguageLiteral}
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
        writer.write("    <uri>" + encode(quad.context.uri) + "</uri>\n")
        writer.write("    <triple>\n")

        writer.write("      <uri>" + encode(quad.subject.uri) + "</uri>\n")
        writer.write("      <uri>" + encode(quad.predicate.uri) + "</uri>\n")

        quad.value match {
          case tl : TypedLiteral =>     writer.write("      <typedLiteral datatype=\"" + encode(tl.dataType.uri) + "\">" + encode(tl.value) + "</typedLiteral>\n")
          case ll : LanguageLiteral =>  writer.write("      <plainLiteral xml:lang=\"" + encode(ll.language) + "\">" + encode(ll.value) + "</typedLiteral>\n")
          case pl : PlainLiteral =>     writer.write("      <plainLiteral>" + encode(pl.value) + "</plainLiteral>\n")
          case iri : IriRef =>          writer.write("      <uri>" + encode(iri.uri) + "</uri>\n")
        }

        writer.write("    </triple>\n")
        writer.write("  </graph>\n")
    }

    private def encode(str : String) = str.map(c => Utility.Escapes.escMap.get(c).getOrElse(c)).mkString
}

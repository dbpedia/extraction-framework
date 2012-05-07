package org.dbpedia.extraction.destinations.formatters

import java.io.Writer
import org.dbpedia.extraction.destinations.{Quad, Formatter}
import scala.xml.Utility.escape
import java.net.URI

/**
 * Formats statements according to the TriX format.
 * See: http://www.hpl.hp.com/techreports/2004/HPL-2004-56.html
 */
class TriXFormatter(iri: Boolean, header : String = null) extends Formatter
{
    override def writeHeader(writer : Writer): Unit = {
      
      if (header != null) writer.write(header)
      writer.write("<TriX xmlns=\"http://www.w3.org/2004/03/trix/trix-1/\">\n")
    }

    override def writeFooter(writer : Writer): Unit = {
      
      writer.write("</TriX>\n")
    }

    override def write(quad : Quad, writer : Writer) : Unit = {
      
      val trix = new TriXBuilder(iri: Boolean)
      
      trix.startTag("graph")
      
      trix.uri(quad.context)
      
      trix.startTag("triple")
      
      trix.uri(quad.subject)
      
      trix.uri(quad.predicate)
      
      if (quad.datatype == null) trix.uri(quad.value)
      else trix.literal(quad.value, quad.datatype, quad.language)
      
      trix.endTag("triple")
      
      trix.endTag("graph")
      
      writer.write(trix.toString)
    }
}

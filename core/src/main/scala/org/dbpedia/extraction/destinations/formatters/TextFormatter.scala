package org.dbpedia.extraction.destinations.formatters

import org.dbpedia.extraction.destinations.Formatter
import java.io.Writer
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.destinations.TripleBuilder

/**
 * TODO: rename this class.
 * TODO: add functionality - the comments may contain more useful info
 */
final class TextFormatter(iri: Boolean, turtle: Boolean, quads: Boolean) extends Formatter {
  
    override def writeHeader(writer : Writer) = {
      
      writer.write("# started\n")
    }
    
    override def writeFooter(writer : Writer) = {
      
      writer.write("# complete\n")
    }
    
    override def write(quad : Quad, writer : Writer) = {
      
      val triple = new TripleBuilder(iri, turtle)
      
      triple.uri(quad.subject)
      
      triple.uri(quad.predicate)
      
      if (quad.datatype == null) triple.uri(quad.value) 
      else triple.value(quad.value, quad.datatype, quad.language)
      
      if (quads) triple.uri(quad.context)
      
      triple.close
      
      writer.write(triple.toString)
    }
}

object TextFormatter {
}
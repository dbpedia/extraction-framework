package org.dbpedia.extraction.destinations.formatters

import org.dbpedia.extraction.destinations.Formatter
import java.io.Writer
import org.dbpedia.extraction.destinations.Quad
import org.dbpedia.extraction.destinations.TripleBuilder

/**
 * TODO: add functionality - the comments may contain more useful info
 */
final class TerseFormatter(iri: Boolean, quads: Boolean, turtle: Boolean) extends Formatter {

  override val fileSuffix = {
    val qt = if (quads) 'q' else 't'
    (if (iri) "iri" else "uri") + '.' + (if (turtle) "t"+qt+"l" else "n"+qt) // use strings here, don't add chars... :-)
  }

  override def writeHeader(writer : Writer) = {
    writer.write("# started\n")
  }
  
  override def writeFooter(writer : Writer) = {
    writer.write("# complete\n")
  }
  
  override def write(writer : Writer, quad : Quad) = {
    writer.write(new TerseBuilder(iri, turtle, quads).render(quad))
  }
}

object TerseFormatter {
  
  def all = for (iri <- 0 to 1; quads <- 0 to 1; turtle <- 0 to 1) yield new TerseFormatter(iri == 0, quads == 0, turtle == 0)
  
  def TQuadsIris = new TerseFormatter(true, true, true)
  def TQuadsUris = new TerseFormatter(false, true, true)
  def TTriplesIris = new TerseFormatter(true, false, true)
  def TTriplesUris = new TerseFormatter(false, false, true)
  def NQuadsIris = new TerseFormatter(true, true, false)
  def NQuadsUris = new TerseFormatter(false, true, false)
  def NTriplesIris = new TerseFormatter(true, false, false)
  def NTriplesUris = new TerseFormatter(false, false, false)
}
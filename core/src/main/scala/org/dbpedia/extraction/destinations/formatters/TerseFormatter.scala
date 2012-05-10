package org.dbpedia.extraction.destinations.formatters

import java.io.Writer
import org.dbpedia.extraction.destinations.{Quad, Formatter}

/**
 * TODO: add functionality - the comments may contain more useful info
 */
final class TerseFormatter(iri: Boolean, quads: Boolean, turtle: Boolean) extends Formatter {

  override val fileSuffix = TerseFormatter.fileSuffix(iri, quads, turtle)

  override def writeHeader(writer : Writer) = {
    writer.write("# started\n")
  }
  
  override def writeFooter(writer : Writer) = {
    writer.write("# complete\n")
  }
  
  override def write(writer : Writer, quad : Quad) = {
    writer.write(new TerseBuilder(iri, quads, turtle).render(quad))
  }
}

object TerseFormatter {
  
  def TQuadsIris = formatter(true, true, true)
  def TQuadsUris = formatter(false, true, true)
  def TTriplesIris = formatter(true, false, true)
  def TTriplesUris = formatter(false, false, true)
  def NQuadsIris = formatter(true, true, false)
  def NQuadsUris = formatter(false, true, false)
  def NTriplesIris = formatter(true, false, false)
  def NTriplesUris = formatter(false, false, false)
  
  def formatter(iri: Boolean, quads: Boolean, turtle: Boolean): TerseFormatter =
    new TerseFormatter(iri, quads, turtle)
  
  def all = for (iri <- 0 to 1; quads <- 0 to 1; turtle <- 0 to 1) yield formatter(iri == 0, quads == 0, turtle == 0)
  
  def fileSuffix(iri: Boolean, quads: Boolean, turtle: Boolean): String = {
    val qt = if (quads) 'q' else 't'
    (if (iri) "iri" else "uri") + '.' + (if (turtle) "t"+qt+"l" else "n"+qt) // use strings here, don't add chars... :-)
  }

  def suffixes: Seq[String] =
    for (iri <- 0 to 1; quads <- 0 to 1; turtle <- 0 to 1) yield 
      fileSuffix(iri == 0, quads == 0, turtle == 0)
  
  /** suffix must start with "iri" or "uri" and end with "tql", "nq", "ttl" or "nt". */
  val Suffix = """(iri|uri)\.(tql|nq|ttl|nt)""".r
  
  /**
   * @return formatter for the given file suffix, or None if there is no formatter for the suffix.
   */
  def forSuffix(suffix: String): Option[TerseFormatter] = suffix match {
    case Suffix(i,qt) => Some(formatter(i == "iri", qt == "tql" || qt == "nq", qt == "tql" || qt == "ttl"))
    case _ => None
  }
}
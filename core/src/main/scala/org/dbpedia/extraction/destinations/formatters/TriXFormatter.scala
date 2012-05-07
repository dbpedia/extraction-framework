package org.dbpedia.extraction.destinations.formatters

import java.io.Writer
import org.dbpedia.extraction.destinations.{Quad, Formatter}

/**
 * Formats statements according to the TriX format.
 * See: http://www.hpl.hp.com/techreports/2004/HPL-2004-56.html
 */
class TriXFormatter(iri: Boolean, quads: Boolean, header: String = null) extends Formatter
{
  override val fileSuffix = TriXFormatter.fileSuffix(iri, quads)

  override def writeHeader(writer : Writer): Unit = {
    if (header != null) writer.write(header)
    writer.write("<TriX xmlns=\"http://www.w3.org/2004/03/trix/trix-1/\">\n")
  }

  override def writeFooter(writer : Writer): Unit = {
    writer.write("</TriX>\n")
  }

  override def write(writer : Writer, quad : Quad) : Unit = {
    writer.write(new TriXBuilder(iri, quads).render(quad))
  }
}

object TriXFormatter {
  
  def QuadsIris(header: String = null) = formatter(true, true, header)
  def QuadsUris(header: String = null) = formatter(false, true, header)
  def TriplesIris(header: String = null) = formatter(true, false, header)
  def TriplesUris(header: String = null) = formatter(false, false, header)

  def formatter(iri: Boolean, quads: Boolean, header: String = null): TriXFormatter =
    new TriXFormatter(iri, quads, header)
  
  def all = for (iri <- 0 to 1; quads <- 0 to 1) yield formatter(iri == 0, quads == 0)
  
  def fileSuffix(iri: Boolean, quads: Boolean) = {
    (if (iri) "iri" else "uri") + '.' + (if (quads) "quads" else "triples") + ".trix"
  }

  def suffixes: Seq[String] = 
    for (iri <- 0 to 1; quads <- 0 to 1) yield
      fileSuffix(iri == 0, quads == 0)
  
  /** suffix must start with "iri" or "uri", have "quads" or "triples" in the middle, and end with "trix" */
  val Suffix = """(iri|uri)\.(quads|triples)\.trix""".r
  
  /**
   * @return formatter for the given file suffix, or None if there is no formatter for the suffix.
   * TODO: add header parameter? Probably not needed.
   */
  def forSuffix(suffix: String): Option[TriXFormatter] = suffix match {
    case Suffix(i, q) => Some(formatter(i == "iri", q == "quads"))
    case _ => None
  }
  
}
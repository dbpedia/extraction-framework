package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.util.Language
import java.net.URI

/**
 * Represents a statement.
 */
class Quad( val language : Language,
            val dataset : Dataset,
            val subject : String,
            val predicate : String,
            val value : String,
            val context : String,
            var datatype : Datatype )
{
    //Validate input
    if (subject == null) throw new NullPointerException("subject")
    if (predicate == null) throw new NullPointerException("predicate")
    if (value == null) throw new NullPointerException("value")
    if (context == null) throw new NullPointerException("context")

    if (value.isEmpty) throw new IllegalArgumentException("Value is empty")

    // Validate URIs
    // new URI(subject)
    // new URI(predicate)
    // new URI(context)
    // if (datatype == null) new URI(value)
    
    def this(language : Language,
             dataset : Dataset,
             subject : String,
             predicate : OntologyProperty,
             value : String,
             context : String,
             datatype : Datatype = null) = this(language, dataset, subject, predicate.uri, value, context, Quad.getType(datatype, predicate))

    override def toString = render(true, true)

    def render(turtle: Boolean, quad: Boolean) : String =
    {
      val triple = new TripleBuilder(turtle)
      triple.uri(subject).uri(predicate).value(value, datatype, language)
      if (quad) triple.uri(context)
      triple.close().toString()
    }
}

object Quad
{
  private def getType(datatype : Datatype, predicate : OntologyProperty): Datatype =
  {
    if (datatype != null) datatype
    else predicate.range match {
      case datatype: Datatype => datatype
      case _ => null
    }
  }
}

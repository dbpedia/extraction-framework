package org.dbpedia.extraction.destinations

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.util.Language

/**
 * Represents a statement.
 */
class Quad( val language: Language,
            val dataset: Dataset,
            val subject: String,
            val predicate: String,
            val value: String,
            val context: String,
            val datatype: Datatype )
{
    def this(language : Language,
             dataset : Dataset,
             subject : String,
             predicate : OntologyProperty,
             value : String,
             context : String,
             datatype : Datatype = null) = this(language, dataset, subject, predicate.uri, value, context, Quad.getType(datatype, predicate))

    //Validate input
    if (subject == null) throw new NullPointerException("subject")
    if (predicate == null) throw new NullPointerException("predicate")
    if (value == null) throw new NullPointerException("value")
    if (context == null) throw new NullPointerException("context")
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

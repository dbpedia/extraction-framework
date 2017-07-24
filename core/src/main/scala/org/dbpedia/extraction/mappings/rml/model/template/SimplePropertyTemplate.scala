package org.dbpedia.extraction.mappings.rml.model.template

import org.dbpedia.extraction.ontology.OntologyProperty
import org.dbpedia.extraction.ontology.datatypes.Datatype

/**
  * Created by wmaroy on 24.07.17.
  */
class SimplePropertyTemplate(val property : String,
                             val ontologyProperty : OntologyProperty,
                             val select : String,
                             val prefix : String,
                             val suffix : String,
                             val transform : String,
                             val unit : Datatype,
                             val factor : Double) {

  override def toString = {
    val builder = new StringBuilder
    builder.append("Simple Property Template:\n" )
    builder.append(createIfNotNull("Property", property))
    builder.append(createIfNotNull("Ontology Property", ontologyProperty.name))
    builder.append(createIfNotNull("Select", select))
    builder.append(createIfNotNull("Prefix", prefix))
    builder.append(createIfNotNull("Suffix", suffix))
    if(unit != null) {
      builder.append(createIfNotNull("unit", unit.name))
    }
    builder.append(createIfNotNull("factor", factor.toString))
    builder.toString()
  }

  private def createIfNotNull(name : String, value : String) : String = {
    if(value != "null") name + ": " + value + "\n" else ""
  }

}

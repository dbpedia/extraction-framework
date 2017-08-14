package org.dbpedia.extraction.mappings.rml.model.voc

import org.dbpedia.extraction.ontology.RdfNamespace

/**
  * Created by wmaroy on 30.07.17.
  */
object Property {

  val PREDICATE = RdfNamespace.RR.namespace + "predicate"

  val PARENTTRIPLESMAP = RdfNamespace.RR.namespace + "parentTriplesMap"

  val OBJECTMAP = RdfNamespace.RR.namespace + "objectMap"

  val SUBJECTMAP = RdfNamespace.RR.namespace + "subjectMap"

  val PREDICATEOBJECTMAP = RdfNamespace.RR.namespace + "predicateObjectMap"

  val REFERENCE = RdfNamespace.RML.namespace + "reference"

  val ITERATOR = RdfNamespace.RML.namespace + "iterator"

  val FUNCTIONVALUE = RdfNamespace.FNML.namespace +"functionValue"

  val OBJECT = RdfNamespace.RR.namespace + "object"

  val EXECUTES = RdfNamespace.FNO.namespace + "executes"

  val EQUAL_CONDITION = RdfNamespace.CRML + "equalCondition"

  val FALLBACK_MAP = RdfNamespace.CRML + "fallbackMap"

  val TYPE = RdfNamespace.RDF +"type"

}

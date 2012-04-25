package org.dbpedia.extraction.ontology

import datatypes._

/**
 * Represents an ontology.
 *
 * @param classes The classes of this ontology
 * @param properties The properties of this ontology
 * @param datatypes The datatypes of this ontology
 * @param specializations Map of all ontology properties which are specialized to a specific datatype.
 * Example: The entry (Person, height) -> centimetre denotes a specialized property Person/height which has the range centimetres.
 */
class Ontology ( 
  val classes : Map[String, OntologyClass],
  val properties : Map[String, OntologyProperty],
  val datatypes : Map[String, Datatype],
  val specializations : Map[(OntologyClass, OntologyProperty), UnitDatatype] 
)
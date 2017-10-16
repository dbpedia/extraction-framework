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
  val specializations : Map[(OntologyClass, OntologyProperty), UnitDatatype],
  val wikidataPropertiesMap : Map[String,Set[OntologyProperty]],
  val wikidataClassesMap : Map[String,Set[OntologyClass]]
){

  private def resolveId(id: String) = if(id == null)
      ""
    else if(id.contains("/"))
      id.substring(id.lastIndexOf("/")+1)
    else if(id.startsWith("dbo:"))
      id.substring(4)
    else
      id

  def getOntologyClass(id: String): Option[OntologyClass] ={
    classes.get(resolveId(id))
  }

  def getOntologyProperty(id: String): Option[OntologyProperty] ={
    properties.get(resolveId(id))
  }

  def getOntologyDatatype(id: String): Option[Datatype] ={
    datatypes.get(resolveId(id))
  }

  def getWikidataClass(id: String): Option[Set[OntologyClass]] ={
    wikidataClassesMap.get(resolveId(id))
  }

  def getWikidataProperty(id: String): Option[Set[OntologyProperty]] ={
    wikidataPropertiesMap.get(resolveId(id))
  }
}
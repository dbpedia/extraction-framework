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
) extends java.io.Serializable {
  def getOntologyClass(id: String): Option[OntologyClass] ={
    val iid = if(id.contains("/")) id.substring(id.lastIndexOf("/")+1) else id
    classes.get(iid)
  }

  def getOntologyProperty(id: String): Option[OntologyProperty] ={
    val iid = if(id.contains("/")) id.substring(id.lastIndexOf("/")+1) else id
    properties.get(iid)
  }

  def getOntologyDatatype(id: String): Option[Datatype] ={
    val iid = if(id.contains("/")) id.substring(id.lastIndexOf("/")+1) else id
    datatypes.get(iid)
  }

  def getWikidataClass(id: String): Option[Set[OntologyClass]] ={
    val iid = if(id.contains("/")) id.substring(id.lastIndexOf("/")+1) else id
    wikidataClassesMap.get(iid)
  }

  def getWikidataProperty(id: String): Option[Set[OntologyProperty]] ={
    val iid = if(id.contains("/")) id.substring(id.lastIndexOf("/")+1) else id
    wikidataPropertiesMap.get(iid)
  }

  def isSubclassOf(sub: OntologyClass, sup: OntologyClass): Boolean ={
    if (sup eq sub)
      return true
    sub.baseClasses.map(isSubclassOf(_, sup)).foldLeft(false)(_||_)
  }

  def isSuperclassOf(sup: OntologyClass, sub: OntologyClass): Boolean = isSubclassOf(sub, sup)
}
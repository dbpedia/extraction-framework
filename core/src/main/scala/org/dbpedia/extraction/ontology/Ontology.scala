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
class Ontology( val classes : List[OntologyClass] = Nil,
                val properties : List[OntologyProperty] = Nil,
                val datatypes : List[Datatype] = Nil,
                val specializations : Map[(OntologyClass, OntologyProperty), UnitDatatype] = Map.empty )
{
    private val classMap = classes.map( clazz => (clazz.name, clazz) ).toMap
    private val propertyMap = properties.map( property => (property.name, property) ).toMap
    private val datatypesMap = datatypes.map( datatype => (datatype.name, datatype) ).toMap

    //TODO rename because of similarity to java.lang.Object.getClass?
    def getClass(name : String) : Option[OntologyClass] = classMap.get(name)

    def getProperty(name : String) : Option[OntologyProperty] = propertyMap.get(name)

    def getDatatype(name : String) : Option[Datatype] = datatypesMap.get(name)

    override def toString = "Ontology"
}

package org.dbpedia.extraction.ontology

import org.dbpedia.extraction.util.Language

/**
 * Represents an ontology property.
 * There are 2 sub classes of this class: OntologyObjectProperty and OntologyDatatypeProperty.
 *
 * @param name The name of this entity. e.g. foaf:name
 * @param labels The labels of this entity. Map: LanguageCode -> Label
 * @param comments Comments describing this entity. Map: LanguageCode -> Comment
 * @param range The range of this property
 * @param isFunctional Defines whether this is a functional property.
 * A functional property is a property that can have only one (unique) value y for each instance x (see: http://www.w3.org/TR/owl-ref/#FunctionalProperty-def)
 */
class OntologyProperty( name : String, labels : Map[Language, String], comments : Map[Language, String],
                        val domain : OntologyClass, val range : OntologyType, val isFunctional : Boolean = false,
                        val equivalentProperties : Set[OntologyProperty] = Set()) extends OntologyEntity(name, labels, comments)
{
    require(! RdfNamespace.validate(name) || domain != null, "missing domain for property "+name)
    require(! RdfNamespace.validate(name) || range != null, "missing range for property "+name)
    require(equivalentProperties != null, "equivalent properties are null for property "+name)
    
    val uri = RdfNamespace.fullUri(DBpediaNamespace.ONTOLOGY, name)

    val isExternalProperty = ! uri.startsWith(DBpediaNamespace.ONTOLOGY.namespace)
    
    override def toString = uri

    override def equals(other : Any) = other match
    {
        case otherProperty : OntologyProperty => (name == otherProperty.name)
        case _ => false
    }

    override def hashCode = name.hashCode
}

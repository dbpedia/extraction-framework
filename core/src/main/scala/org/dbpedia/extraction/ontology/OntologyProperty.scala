package org.dbpedia.extraction.ontology

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
class OntologyProperty( name : String, labels : Map[String, String], comments : Map[String, String],
                        val domain : OntologyClass, val range : OntologyType, val isFunctional : Boolean = false) extends OntologyEntity(name, labels, comments)
{
    require(domain != null, "domain != null")
    require(range != null, "range != null")
    
    val uri = OntologyNamespaces.getUri(name, OntologyNamespaces.DBPEDIA_PROPERTY_NAMESPACE)
    
    override def toString = uri

    override def equals(other : Any) = other match
    {
        case otherProperty : OntologyProperty => (name == otherProperty.name)
        case _ => false
    }

    override def hashCode = name.hashCode
}

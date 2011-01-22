package org.dbpedia.extraction.ontology

/**
 * Represents a object property.
 *
 * @param name The name of this property e.g. foaf:homepage
 * @param labels The labels of this entity. Map: LanguageCode -> Label
 * @param comments Comments describing this entity. Map: LanguageCode -> Comment
 * @param domain The domain of this property
 * @param range The range of this property
 * @param isFunctional Defines whether this is a functional property.
 * A functional property is a property that can have only one (unique) value y for each instance x (see: http://www.w3.org/TR/owl-ref/#FunctionalProperty-def)
 */
class OntologyObjectProperty( name : String, labels : Map[String, String], comments : Map[String, String],
                              domain : OntologyClass, override val range : OntologyClass, isFunctional : Boolean = false)
    extends OntologyProperty(name, labels, comments, domain, range, isFunctional)

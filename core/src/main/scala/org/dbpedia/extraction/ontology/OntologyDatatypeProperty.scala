package org.dbpedia.extraction.ontology

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.util.Language

/**
 * Represents a datatype property.
 *
 * @param name The name of this property e.g. foaf:name
 * @param labels The labels of this entity. Map: LanguageCode -> Label
 * @param comments Comments describing this entity. Map: LanguageCode -> Comment
 * @param domain The domain of this property
 * @param range The range of this property
 * @param isFunctional Defines whether this is a functional property.
 * A functional property is a property that can have only one (unique) value y for each instance x (see: http://www.w3.org/TR/owl-ref/#FunctionalProperty-def)
 */
class OntologyDatatypeProperty(name : String, labels : Map[Language, String], comments : Map[Language, String],
                               domain : OntologyClass, override val range : Datatype, isFunctional : Boolean = false,
                               equivalentProperties : Set[OntologyProperty] = Set(),
                               superProperties : Set[OntologyProperty] = Set())
    extends OntologyProperty(name, labels, comments, domain, range, isFunctional, equivalentProperties, superProperties)

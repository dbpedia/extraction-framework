package org.dbpedia.extraction.ontology

import org.dbpedia.extraction.util.Language

/**
 * Represents an ontology class.
 *
 * @param name The name of this class e.g. foaf:Person
 * @param labels The labels of this type. Map: LanguageCode -> Label
 * @param comments Comments describing this type. Map: LanguageCode -> Comment
 * @param subClassOf The super class of this class. May be null for owl:Thing or classes in 
 * namespaces that we don't validate. See OntologyNamespaces.skipValidation() TODO: use type ListSet, not List?
 * @param equivalentClasses
 */
class OntologyClass(name : String, labels : Map[Language, String], comments : Map[Language, String],
                    val subClassOf : List/*TODO:ListSet?*/[OntologyClass], val equivalentClasses : Set[OntologyClass]) extends OntologyType(name, labels, comments)
{
    require(name != null, "name != null")
    require(labels != null, "labels != null")
    require(comments != null, "comments != null")
    require(subClassOf != null, "subClassOf != null")
    require(name == "owl:Thing" || subClassOf.nonEmpty || OntologyNamespaces.skipValidation(name), "subClassOf.nonEmpty")
    require(equivalentClasses != null, "equivalentClasses != null")

    // FIXME: the last parameter selects IRI or URI format. This is the wrong place for that choice.
    override val uri = OntologyNamespaces.getUri(name, OntologyNamespaces.DBPEDIA_CLASS_NAMESPACE, Language.Default)

    val isExternalClass = ! uri.startsWith(OntologyNamespaces.DBPEDIA_CLASS_NAMESPACE)
}

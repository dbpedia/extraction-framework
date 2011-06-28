package org.dbpedia.extraction.ontology

/**
 * Represents an ontology class.
 *
 * @param name The name of this class e.g. foaf:Person
 * @param labels The labels of this type. Map: LanguageCode -> Label
 * @param comments Comments describing this type. Map: LanguageCode -> Comment
 * @param subClassOf The super class of this class. owl:Thing is the only class for which this may be null.
 * @param equivalentClasses
 */
class OntologyClass(name : String, labels : Map[String, String], comments : Map[String, String],
                    val subClassOf : List[OntologyClass], val equivalentClasses : Set[OntologyClass]) extends OntologyType(name, labels, comments)
{
    require(name != null, "name != null")
    require(labels != null, "labels != null")
    require(comments != null, "comments != null")
    require(name == "owl:Thing" || OntologyNamespaces.skipValidation(name) || subClassOf.nonEmpty, "subClassOf.nonEmpty")
    require(equivalentClasses != null, "equivalentClasses != null")

    override val uri = OntologyNamespaces.getUri(name, OntologyNamespaces.DBPEDIA_CLASS_NAMESPACE)
}

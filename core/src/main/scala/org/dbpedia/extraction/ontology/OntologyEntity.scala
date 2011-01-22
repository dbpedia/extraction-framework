package org.dbpedia.extraction.ontology


/**
 * Represents a (named) entity in the ontology
 *
 * @param name The name of this entity
 * @param labels The labels of this entity. Map: LanguageCode -> Label
 * @param comments Comments describing this entity. Map: LanguageCode -> Comment
 */
abstract class OntologyEntity(val name : String, val labels : Map[String, String], val comments : Map[String, String])
{
    require(name != null, "name != null")
    require(labels != null, "labels != null")
    require(comments != null, "comments != null")

    /**
     * The URI of this entity.
     */
    val uri : String

    override def toString = uri

    override def equals(other : Any) = other match
    {
        case otherType : OntologyType => (name == otherType.name)
        case _ => false
    }

    override def hashCode = name.hashCode
}

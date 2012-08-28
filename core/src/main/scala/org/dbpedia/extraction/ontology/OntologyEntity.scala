package org.dbpedia.extraction.ontology

import org.dbpedia.extraction.util.Language


/**
 * Represents a (named) entity in the ontology
 * 
 * TODO: an entitiy should contain its base uri and prefix.
 * 
 * @param name The name of this entity
 * @param labels The labels of this entity. Map: LanguageCode -> Label
 * @param comments Comments describing this entity. Map: LanguageCode -> Comment
 */
abstract class OntologyEntity(val name : String, val labels : Map[Language, String], val comments : Map[Language, String])
{
    require(name != null, "name of ontology entity is null")
    require(labels != null, "missing labels for ontology entity "+name)
    require(comments != null, "missing comments for ontology entity "+name)

    /**
     * The URI of this entity.
     */
    val uri : String

    override def toString = uri

    override def equals(other : Any) = other match
    {
        case that: OntologyEntity => (this.getClass == that.getClass && this.name == that.name)
        case _ => false
    }

    override def hashCode = name.hashCode
}

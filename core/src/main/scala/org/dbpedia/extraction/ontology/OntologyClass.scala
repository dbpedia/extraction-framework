package org.dbpedia.extraction.ontology

import org.dbpedia.extraction.util.Language
import scala.collection.mutable.{Buffer,ArrayBuffer}

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
                    val baseClasses : List/*TODO:ListSet?*/[OntologyClass], val equivalentClasses : Set[OntologyClass]) extends OntologyType(name, labels, comments)
{
    require(name != null, "name is null")
    require(labels != null, "labels is null")
    require(comments != null, "comments is null")
    require(baseClasses != null, "baseClasses is null")
    require(baseClasses.nonEmpty || name == "owl:Thing" || ! RdfNamespace.validate(name), "baseClasses is empty, although this class is not the root and it should be validated")
    require(equivalentClasses != null, "equivalentClasses is null")
    
    /**
     * Transitive closure of sub-class and equivalent-class relations, including this class.
     */
    lazy val relatedClasses: Seq[OntologyClass] = {
      val classes = new ArrayBuffer[OntologyClass]()
      collectClasses(classes)
      classes
    }
    
    private def collectClasses(classes: Buffer[OntologyClass]): Unit = {
      // Note: If this class was already collected, we do nothing, so we silently skip cycles in 
      // class relations here. Some cycles are allowed (for example between equivalent classes, 
      // but there are other cases), others aren't. At this point, it's not easy to distinguish valid 
      // and invalid cycles, so we ignore them all. Cycles should be checked when classes are loaded.
      // Note: set would be nicer than buffer to check contains(), but we want to keep the order.
      if (! classes.contains(this)) {
        classes += this
        equivalentClasses.foreach(_.collectClasses(classes))
        baseClasses.foreach(_.collectClasses(classes))
      }
    } 

    override val uri = RdfNamespace.fullUri(name, DBpediaNamespace.ONTOLOGY)

    val isExternalClass = ! uri.startsWith(DBpediaNamespace.ONTOLOGY.namespace)
}

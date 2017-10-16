package org.dbpedia.extraction.ontology

import org.dbpedia.extraction.util.Language

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, Buffer, ListBuffer}

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
class OntologyClass(
  name : String, 
  labels : Map[Language, String], 
  comments : Map[Language, String],
  val baseClasses : List/*TODO:ListSet?*/[OntologyClass], 
  val equivalentClasses : Set[OntologyClass],
  val disjointWithClasses : Set[OntologyClass]
) 
extends OntologyType(name, labels, comments)
{
    require(baseClasses != null, "missing base classes for class "+name)
    require(baseClasses.nonEmpty || name == "owl:Thing" || ! RdfNamespace.validate(name), "missing base classes for class "+name+", although this class is not the root and it should be validated")
    require(equivalentClasses != null, "missing equivalent classes for class "+name)
    require(disjointWithClasses != null, "missing disjointWith classes for class "+name)


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
      // Note: a set would be nicer than a buffer to check contains(), but we want to keep the order.
      if (! classes.contains(this)) {
        classes += this
        equivalentClasses.foreach(_.collectClasses(classes))
        baseClasses.foreach(_.collectClasses(classes))
      }
    }

  lazy val superClasses: Map[Int, List[OntologyClass]] = {
    val ret = new mutable.HashMap[Int, mutable.ListBuffer[OntologyClass]]()
    collectSuperClasses(ret, 1)
    ret.map(x => x._1 -> x._2.toList).toMap
  }

  private def collectSuperClasses(classMap: mutable.HashMap[Int, mutable.ListBuffer[OntologyClass]], depth: Int): Unit = {
    // Note: If this class was already collected, we do nothing, so we silently skip cycles in
    // class relations here. Some cycles are allowed (for example between equivalent classes,
    // but there are other cases), others aren't. At this point, it's not easy to distinguish valid
    // and invalid cycles, so we ignore them all. Cycles should be checked when classes are loaded.
    // Note: a set would be nicer than a buffer to check contains(), but we want to keep the order.
    if (! classMap.values.flatten.toList.contains(this)) {
      classMap.get(depth) match{
        case Some(l) => l += this
        case None => classMap += {
          val lb = new ListBuffer[OntologyClass]()
          lb += this
          depth -> lb
        }
      }
      baseClasses.foreach(_.collectSuperClasses(classMap, depth+1))
    }
  }

  def isSubclassOf(sup: OntologyClass): Boolean ={
    if (sup eq this)
      return true
    this.baseClasses.map(_.isSubclassOf(sup)).foldLeft(false)(_||_)
  }

  def isSuperclassOf(sub: OntologyClass): Boolean = sub.isSubclassOf(this)

  override val uri = RdfNamespace.fullUri(DBpediaNamespace.ONTOLOGY, name)

    val isExternalClass = ! uri.startsWith(DBpediaNamespace.ONTOLOGY.namespace)
}

object OntologyClass{
  val owlThing = new OntologyClass("owl:Thing", Map(), Map(), List(), Set(), Set())
}
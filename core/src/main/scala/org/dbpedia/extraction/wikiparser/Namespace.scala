package org.dbpedia.extraction.wikiparser

import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces
import scala.collection.mutable.HashMap
import org.dbpedia.extraction.util.Language

/**
 * Namespaces codes.
 * 
 * FIXME: This object should not exist. We must load the namespaces for a MediaWiki instance 
 * from its api.php or from a file. These values must then be injected into all objects that
 * need them.
 * 
 * FIXME: separate Wikipedia and DBpedia namespaces. We cannot even be sure that there
 * are no name clashes. "Mapping ko" may mean "Template talk" in some language...
 */
class Namespace private[wikiparser](val code: Int, name: String, dbpedia: Boolean) {
  
  def getName(lang : Language) = if (dbpedia) name else Namespaces.names(lang).getOrElse(code, throw new IllegalArgumentException("namespace number "+code+" not found for language '"+lang.wikiCode+"'")) 
  
  override def toString = code+"/"+name
}
  
/**
 * Helper object that builds namespace objects and then disappears. 
 */
private class NamespaceBuilder {
  
  // map from namespace code to namespace (all namespaces)
  val values = new HashMap[Int, Namespace]
  
  // map from language to mapping namespace (only mapping namespaces)
  val mappings = new HashMap[Language, Namespace]
  
  // map from namespace name to namespace (only dbpedia namespaces)
  val dbpedias = new HashMap[String, Namespace]
    
  def ns(code: Int, name: String, dbpedia: Boolean) : Namespace = {
    // also create 'talk'namespace, except for the first few namespaces, they are special
    if (code % 2 == 0 && code >= 2) create(code + 1, name+" talk", dbpedia)
    create(code, name, dbpedia)
  }
  
  def create(code: Int, name: String, dbpedia: Boolean) : Namespace = {
    val namespace = new Namespace(code, name, dbpedia)
    // put() returns the previous value as an option. if there was one, throw an exception
    values.put(code, namespace).foreach(previous => throw new IllegalArgumentException("duplicate namespace: ["+previous+"] and ["+namespace+"]"))
    if (dbpedia) dbpedias(name.toLowerCase(Language.Default.locale)) = namespace
    namespace
  }

  // Default MediaWiki namespaces
  val mediawiki = Map("Media"-> -2,"Special"-> -1,"Main"->0,"Talk"->1,"User"->2,"Project"->4,"File"->6,"MediaWiki"->8,"Template"->10,"Help"->12,"Category"->14)
  
  for ((name,code) <- mediawiki) ns(code, name, false)
  
  // The following are used quite differently on different wikipedias, so we use generic names.
  // Most languages use 100-113, but hu uses 90-99.
  for (code <- (90 to (112, step = 2))) ns(code, "Namespace "+code, false)
    
  // Namespaces used on http://mappings.dbpedia.org, sorted by number. 
  // see http://mappings.dbpedia.org/api.php?action=query&meta=siteinfo&siprop=namespaces
  ns(200, "OntologyClass", true)
  ns(202, "OntologyProperty", true)
  ns(206, "Datatype", true)
  
  val map = Map(
    "en"->204,"de"->208,"fr"->210,"it"->212,"es"->214,"nl"->216,"pt"->218,"pl"->220,"ru"->222,
    "cs"->224,"ca"->226,"bn"->228,"hi"->230,"hu"->238,"ko"->242,"tr"->246,"ar"->250,"bg"->264,
    "sl"->268,"eu"->272,"hr"->284,"el"->304,"ga"->396
  )
  
  for ((lang,code) <- map) mappings(Language(lang)) = ns(code, "Mapping "+lang, true)
}

/**
 * Helper class that lets us build the namespace objects without retaining any references 
 * to the builder or other temporary data. Using the builder as constructor parameter instead 
 * of val does the trick.
 */
private[wikiparser] class NamespaceBuilderDisposer(builder: NamespaceBuilder) {
  
  /**
   * Immutable map from namespace code to namespace containing all MediaWiki and DBpedia namespaces.
   */
  val values: Map[Int, Namespace] = builder.values.toMap // toMap makes immutable

  /**
   * Immutable map from language to namespace containing only the mapping namespaces on http://mappings.dbpedia.org.
   */
  val mappings: Map[Language, Namespace]  = builder.mappings.toMap // toMap makes immutable
  
  /**
   * Immutable map from namespace name to namespace containing only the DBpedia namespaces.
   */
  val dbpedias: Map[String, Namespace] = builder.dbpedias.toMap // toMap makes immutable
}

object Namespace extends NamespaceBuilderDisposer(new NamespaceBuilder) {
  
  val Main = values(0)
  val File = values(6)
  val Template = values(10)
  val Category = values(14)

  val OntologyClass = values(200)
  val OntologyProperty = values(202)
  
  def get(lang : Language, name : String) : Option[Namespace] = {
    dbpedias.get(name.toLowerCase(Language.Default.locale)) match {
      case None => Namespaces.codes(lang).get(name.toLowerCase(lang.locale)) match {
        case None => None
        case Some(code) => values.get(code)
      }
      case some => some // return what we found, don't un-wrap and re-wrap Some(namespace)
    }
  }

}

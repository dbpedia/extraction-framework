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
 * are no name clashes. "Mapping de" may mean "Template talk" in some language...
 */
class Namespace private[wikiparser](val code: Int, name: String, dbpedia: Boolean) {
  
  def getName(lang : Language) = if (dbpedia) name else Namespaces.names(lang).getOrElse(code, throw new IllegalArgumentException("namespace number "+code+" not found for language '"+lang.wikiCode+"'")) 
  
  override def toString = code+"/"+name
}
  
/**
 * Helper object that builds namespace objects and then disappears. 
 */
private class NamespaceBuilder {
  
  val values = new HashMap[Int, Namespace]
  val mappings = new HashMap[Language, Namespace]
  val dbpedias = new HashMap[String, Namespace]
    
  def ns(code: Int, name: String, dbpedia: Boolean) : Namespace = {
    val namespace : Namespace = new Namespace(code, name, dbpedia)
    values(code) = namespace
    if (dbpedia) dbpedias(name.toLowerCase(Language.Default.locale)) = namespace
    namespace
  }

  // Default MediaWiki namespaces
  val mediawiki = Map(
    "Media"-> -2,"Special"-> -1,"Main"->0,"Talk"->1,"User"->2,"User talk"->3,"Project"->4,"Project talk"->5,
    "File"->6,"File talk"->7,"MediaWiki"->8,"MediaWiki talk"->9,"Template"->10,"Template talk"->11,"Help"->12,
    "Help talk"->13,"Category"->14,"Category talk"->15
  )
  
  for ((name,code) <- mediawiki) ns(code, name, false)
  
  // The following are used quite differently on different wikipedias, so we use generic names.
  // Most languages use 100-113, but hu uses 90-99.
  for (code <- (90 to 113)) ns(code, "Namespace "+code, false)
    
  // Namespaces used on http://mappings.dbpedia.org, sorted by number. 
  // see http://mappings.dbpedia.org/api.php?action=query&meta=siteinfo&siprop=namespaces
  ns(200, "OntologyClass", true)
  ns(202, "OntologyProperty", true)
  
  val map = Map(
    "en"->204,"de"->208,"fr"->210,"it"->212,"es"->214,"nl"->216,"pt"->218,"pl"->220,"ru"->222,"cs"->224,"ca"->226,
    "bn"->228,"hi"->230,"hu"->238,"ko"->242,"tr"->246,"ar"->250,"sl"->268,"eu"->272,"hr"->284,"el"->304,"ga"->396
  )
  
  for ((lang,code) <- map) {
    mappings(Language(lang)) = ns(code, if (lang == "en") "Mapping" else "Mapping "+lang, true)
  }
}

/**
 * Helper class that lets us build the namespace objects without retaining any references 
 * to temporary data. "private[this] val" does the trick.
 */
private[wikiparser] class NamespaceBuilderDisposer(private[this] val builder: NamespaceBuilder) {
  
  def this() = this(new NamespaceBuilder)

  /**
   * Immutable map containing all MediaWiki and DBpedia namespaces.
   */
  val values = builder.values.toMap // toMap makes immutable

  /**
   * Immutable map containing the mapping namespaces on http://mappings.dbpedia.org.
   */
  val mappings = builder.mappings.toMap // toMap makes immutable
  
  /**
   * Immutable map containing the DBpedia namespaces.
   */
  protected val dbpedias = builder.dbpedias.toMap // toMap makes immutable
}

object Namespace extends NamespaceBuilderDisposer {
  
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

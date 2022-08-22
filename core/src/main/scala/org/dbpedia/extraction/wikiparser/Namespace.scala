package org.dbpedia.extraction.wikiparser

import java.io.File

import org.dbpedia.extraction.config.Config
import org.dbpedia.extraction.sources.XMLSource
import org.dbpedia.extraction.util.{JsonConfig, Language}
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces

import scala.collection.mutable

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
class Namespace private[wikiparser](val code: Int, val name: String, dbpedia: Boolean) extends java.io.Serializable {
  
  def name(lang : Language): String = 
    if (dbpedia) name
    else Namespaces.names(lang).getOrElse(code, throw new IllegalArgumentException("namespace number "+code+" not found for language '"+lang.wikiCode+"'")) 
  
  override def toString: String = code+"/"+name
  
  override def equals(other: Any): Boolean = other match {
    case that: Namespace => code == that.code && name == that.name
    case _ => false
  }
}

object Namespace {

  // map from namespace code to namespace (all namespaces)
  private val _values = new mutable.HashMap[Int, Namespace]

  // map from language to mapping namespace (only mapping namespaces)
  private val _mappings = new mutable.HashMap[Language, Namespace]

  // map from chapter language to mapping namespace
  private val _chapters = new mutable.HashMap[Language, Namespace]

  // map from namespace name to namespace (only dbpedia namespaces)
  private val _dbpedias = new mutable.HashMap[String, Namespace]

  private def create(code: Int, name: String, dbpedia: Boolean) : Namespace = {
    val namespace = new Namespace(code, name, dbpedia)
    val previous = _values.put(code, namespace)
    require(previous.isEmpty, "duplicate namespace: ["+previous.get+"] and ["+namespace+"]")
    if (dbpedia)
      _dbpedias(name.toLowerCase(Language.Mappings.locale)) = namespace
    namespace
  }

  private def ns(code: Int, name: String, dbpedia: Boolean) : Namespace = {
    // also create 'talk'namespace, except for the first few namespaces, they are special
    if (code % 2 == 0 && code >= 2)
      create(code + 1, name+" talk", dbpedia)
    create(code, name, dbpedia)
  }

  // Default MediaWiki namespaces
  private val mediawiki = Map("Media"-> -2,"Special"-> -1,"Main"->0,"Talk"->1,"User"->2,"Project"->4,"File"->6,"MediaWiki"->8,"Template"->10,"Help"->12,"Category"->14)

  for ((name,code) <- mediawiki)
    ns(code, name, dbpedia = false)

  // The following are used quite differently on different wikipedias, so we use generic names.
  // Most languages use 100-113, but hu uses 90-99.
  // en added 446,447,710,711 in late 2012. Let's go up to 999 to prepare for future additions.
  // wikidata added 120-123, 1198,1199 in early 2013. Let's go up to 1999 to prepare for future additions.
  // en added 2600 in July 2014. Let's go up to 2999. Namespaces > 3000 are discouraged according to
  // https://www.mediawiki.org/wiki/Extension_default_namespaces
  for (code <- (90 to 148 by 2) ++ (400 to 2998 by 2))
    ns(code, "Namespace "+code, dbpedia = false)

  // Namespaces used on http://mappings.dbpedia.org, sorted by number.
  // see http://mappings.dbpedia.org/api.php?action=query&meta=siteinfo&siprop=namespaces
  ns(200, "OntologyClass", dbpedia = true)
  ns(202, "OntologyProperty", dbpedia = true)
  ns(206, "Datatype", dbpedia = true)

  private val mappingsFile: JsonConfig = new JsonConfig(this.getClass.getClassLoader.getResource("mappinglanguages.json"))

  for (lang <- mappingsFile.keys()) {
    val language = Language(lang)
    val properties = mappingsFile.getMap(lang)
    val nns : Namespace = ns(new Integer(properties("code").asText), "Mapping " + lang, dbpedia = true)

    //test if mappings language has actual mappings!
    //load mappings to check if this language does have actual mappings!
    val file = new File(Config.universalConfig.mappingsDir, nns.name(Language.Mappings).replace(' ', '_') + ".xml")
    if (XMLSource.fromFile(file, language).nonEmpty){
      _mappings(language) = nns
      properties.get("chapter") match {
        case Some(b) => if (java.lang.Boolean.parseBoolean(b.asText)) _chapters(language) = nns
        case None =>
      }
    }
  }

  //public stuff
  val values: Map[Int, Namespace] = _values.toMap

  val dbpedias: Map[String, Namespace] = _dbpedias.toMap

  val mappings: Map[Language, Namespace] = _mappings.toMap
  def mappingLanguages: Set[Language] = mappings.keySet

  val chapters: Map[Language, Namespace] = _chapters.toMap
  def chapterLanguages: Set[Language] = chapters.keySet


  val Main: Namespace = _values(0)
  val File: Namespace = _values(6)
  val Template: Namespace = _values(10)
  val Category: Namespace = _values(14)
  val Module: Namespace = _values(828)
  val WikidataProperty: Namespace = _values(120)
  val WikidataLexeme: Namespace = _values(146)


  val OntologyClass: Namespace = _values(200)
  val OntologyProperty: Namespace = _values(202)

  def apply(lang: Language, name: String): Namespace = {
    get(lang, name) match {
      case Some(namespace) => namespace
      case None => throw new IllegalArgumentException("unknown namespace name '"+name+"' for language '"+lang.wikiCode+"'")
    }
  }

  def get(lang: Language, name: String): Option[Namespace] = {
    _dbpedias.get(name.toLowerCase(Language.Mappings.locale)) match {
      // TODO: name.toLowerCase(lang.locale) doesn't quite work. On the other hand, MediaWiki
      // upper / lower case namespace names don't make sense either. Example: http://tr.wikipedia.org/?oldid=13637892
      case None => Namespaces.codes(lang).get(name.toLowerCase(lang.locale)) match {
        case None => None
        case Some(code) => _values.get(code)
      }
      case some => some // return what we found, don't un-wrap and re-wrap Some(namespace)
    }
  }
}

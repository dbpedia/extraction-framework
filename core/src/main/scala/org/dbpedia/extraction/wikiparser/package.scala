package org.dbpedia.extraction

import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces
import java.util.Locale
import scala.collection.mutable.HashMap
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.util.WikiUtil
import org.dbpedia.extraction.util.StringUtils._
import java.net.URLEncoder


package object wikiparser {
  
/**
 * Namespaces
 * 
 * FIXME: get them from
 * http://meta.wikimedia.org/w/api.php?action=query&meta=siteinfo&siprop=interwikimap (probably the same for all WikiMedia wikis)
 * http://en.wikipedia.org/w/api.php?action=query&meta=siteinfo&siprop=namespaces|namespacealiases
 * http://de.wikipedia.org/w/api.php?action=query&meta=siteinfo&siprop=namespaces|namespacealiases
 * etc. and store them in (human-readable) config files, not in class files.
 *  
 * The api.php approach is probably simpler and more stable than getting them from
 * PHP and other config files. PHP files in SVN may not yet be installed in Wikipedia instances.
 * For that 'static' approach we would need to parse:
 * http://svn.wikimedia.org/svnroot/mediawiki/trunk/phase3/includes/Defines.php // NS_ constants
 * http://svn.wikimedia.org/svnroot/mediawiki/trunk/phase3/languages/messages/MessagesEn.php // MediaWiki namespace names
 * http://noc.wikimedia.org/conf/InitialiseSettings.php.txt // WikiMedia namespace aliases
 * ??? // InterWiki prefixes
 *
 * TODO: we only need six of these as constants: 
 * - Main, File, Template, Category
 * - OntologyClass, OntologyProperty
 * (- Mapping is used in one class in live)
 * 
 * The rest are
 * - not used as constants, but retrieved from a language map (mappings)
 * - only used by the title parser and also retrieved from a map
 */

/*
 * So you're wondering why Namespace isn't in Namespace.scala but in this weird *package object*
 * (not a plain old *package*, mind you). Here's why:
 * 
 * - The type of a namespace value like Namespace.Main is Namespace.Value, not Namespace
 * - We don't like that type name. We want to be able to refer to that type as just 'Namespace'.
 * - So we add a type definition: type Namespace = Namespace.Value
 * - But type definitions can only live inside *objects*, not in *packages*. 
 *     (Excursus: Didn't they say everything is an object in Scala? Well, they lied. :-) )
 * - So I tried to leave the *object* Namespace in Namespace.scala, but put the *type* Namespace
 * in package.scala. Looked good, but the Scala compiler crashed, both in Eclipse 
 * (2.9.2.rdev-2769-2011-12-13-g2dd83da) and in Maven (2.15.2).
 * - I fiddled around until I found this solution. Looks weird, but seems to work. I'm not
 * sure though what it means that the Namespace object now lives in an package object, not
 * in a package. jc@sahnwaldt.de 2012-03-28
 */
  
type Namespace = Namespace.Value
  
object Namespace extends Enumeration
{
    val Special = Value(-1)
    val Media = Value(-2)

    val Main = Value(0)
    val Talk = Value(1)
    val User = Value(2)
    val UserTalk = Value(3)
    val Project = Value(4)
    val ProjectTalk = Value(5)
    val File = Value(6)
    val FileTalk = Value(7)
    val MediaWiki = Value(8)
    val MediaWikiTalk = Value(9)
    val Template = Value(10)
    val TemplateTalk = Value(11)
    val Help = Value(12)
    val HelpTalk = Value(13)
    val Category = Value(14)
    val CategoryTalk = Value(15)

    // the following are used quite differently on different wikipedias, so we use generic names
    val Namespace100 = Value(100)
    val Namespace101 = Value(101)
    val Namespace102 = Value(102)
    val Namespace103 = Value(103)
    val Namespace104 = Value(104)
    val Namespace105 = Value(105)
    val Namespace106 = Value(106)
    val Namespace107 = Value(107)
    val Namespace108 = Value(108)
    val Namespace109 = Value(109)
    val Namespace110 = Value(110)
    val Namespace111 = Value(111)
    val Namespace112 = Value(112)
    val Namespace113 = Value(113)
    
    // Namespaces used on http://mappings.dbpedia.org , sorted by number
    // see http://mappings.dbpedia.org/api.php?action=query&meta=siteinfo&siprop=namespaces
    val OntologyClass = Value(200)
    val OntologyProperty = Value(202)
    val Mapping = Value(204)
    val Mapping_de = Value(208)
    val Mapping_fr = Value(210)
    val Mapping_it = Value(212)
    val Mapping_es = Value(214)
    val Mapping_nl = Value(216)
    val Mapping_pt = Value(218)
    val Mapping_pl = Value(220)
    val Mapping_ru = Value(222)
    val Mapping_cs = Value(224)
    val Mapping_ca = Value(226)
    val Mapping_bn = Value(228)
    val Mapping_hi = Value(230)
    val Mapping_hu = Value(238)
    val Mapping_ko = Value(242)
    val Mapping_tr = Value(246)
    val Mapping_ar = Value(250)
    val Mapping_sl = Value(268)
    val Mapping_eu = Value(272)
    val Mapping_hr = Value(284)
    val Mapping_el = Value(304)
    val Mapping_ga = Value(396)
    
    
    def getNamespace(language : Language, name : String) : Option[Namespace] =
    {
        // Note: we used capitalizeLocale(Locale.ENGLISH) here, but that will fail
        // for some languages, e.g. namespace "İstifadəçi" in language "az"
        // See http://az.wikipedia.org/wiki/İstifadəçi:Chrisahn/Sandbox
        val normalizedName = name.capitalizeLocale(language.locale)
    
        for(namespace <- customNamespaces.get(normalizedName))
        {
            return Some(namespace)
        }
    
        for(namespace <- Namespaces(language, normalizedName))
        {
            return Some(Namespace(namespace))
        }
    
        None
    }
    
    /**
     * 
     */
    def getNamespaceName(language : Language, code : Namespace) : String =
    {
        for(name <- reverseCustomNamespaces.get(code))
        {
            return name
        }
    
        Namespaces.getNameForNamespace(language, code)
    }
    
    private val mappingNamespaces = new HashMap[Language, Namespace]
    private val customNamespaces = new HashMap[String, Namespace]
    private val reverseCustomNamespaces = new HashMap[Namespace, String]
    
    for (ns <- Namespace.values)
    {
        if (ns.id >= 200)
        {
            val name = WikiUtil.wikiDecode(ns.toString)
            if (name == "Mapping") mappingNamespaces.put(Language.Default, ns)
            else if (name.startsWith("Mapping ")) mappingNamespaces.put(Language(name.substring(8)), ns)
            customNamespaces.put(name, ns)
            reverseCustomNamespaces.put(ns, name)
        }
    }
    
    def mappingNamespace(language : Language) : Option[Namespace] =
    {
        mappingNamespaces.get(language)
    }
    
}

}
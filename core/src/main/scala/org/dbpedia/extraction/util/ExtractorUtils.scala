package org.dbpedia.extraction.util

import org.dbpedia.extraction.mappings.Extractor
import org.dbpedia.extraction.wikiparser._
import java.util.Properties
import scala.collection.JavaConversions.asScalaSet
import scala.collection.immutable.Map
import scala.collection.immutable.SortedMap
import org.dbpedia.extraction.util.Language.wikiCodeOrdering
import org.dbpedia.extraction.util.ConfigUtils.{getStrings}
import org.dbpedia.extraction.util.RichString.wrapString

import java.net.URLDecoder
import java.math.BigInteger
import java.security.MessageDigest

/**
 * User: Dimitris Kontokostas
 * Various utils for loading Extractors
 * I don't like this so much but it's the only way to reuse extraction configuration code on multiple modules (dump / server)
 * Created: 5/19/14 11:06 AM
 */
object ExtractorUtils {

  /*
   * Get an Extractor class from it's name.
   * if name starts with . use "org.dbpedia.extraction.mappings" as prefix
   * */
  def loadExtractorClass(name: String): Class[_ <: Extractor[_]] = {
    val className = if (name.startsWith(".")) classOf[Extractor[_]].getPackage.getName+name else name
    // TODO: class loader of Extractor.class is probably wrong for some users.
    classOf[Extractor[_]].getClassLoader.loadClass(className).asSubclass(classOf[Extractor[_]])
  }

  def loadExtractorClassSeq(names: Seq[String]) : Seq[Class[_ <: Extractor[_]]] = {
    if (names == null || names.isEmpty)
      Seq.empty
    else
      names.toList.map(ExtractorUtils.loadExtractorClass)
  }

  /** creates a map of languages and extractors from a Properties file
    * examples are as follows
    * # default for all languages
    * extractors=.ArticleCategoriesExtractor,.ArticleTemplatesExtractor,
    *
    * # custom per language
    * extractors.bg=.MappingExtractor
    *
    * Note: If a language xx is not defined but extractor.xx exists
    * extractor.xx extractors will be skipped
    * */
  def loadExtractorsMapFromConfig(languages: Seq[Language], config: Properties): Map[Language, Seq[Class[_ <: Extractor[_]]]] = {

    val stdExtractors = loadExtractorClassSeq(getStrings(config, "extractors", ',', false))

    val classes =
      (for(language <- languages)
        yield(
          language,
          // Standard extractors from "extractors" plus custom defined extractors from "extractors.xx"
          (stdExtractors ++ getStrings(config, "extractors."+language.wikiCode, ',', false).map(loadExtractorClass)).distinct)
      ).toMap

    // Sort keys
    SortedMap(classes.toSeq: _*)
  }
  
  /**
   * List of namespaces in the Commons that might contain metadata.
   * These should be processed by the appropriate mappings, but
   * should probably be moved into a configuration file somewhere.
   */
  val commonsNamespacesContainingMetadata:Set[Namespace] = try {
      Set[Namespace](
        Namespace.Main,
        Namespace.File,
        Namespace.Category,
        Namespace.Template,
        Namespace.get(Language.Commons, "Creator").get,
        Namespace.get(Language.Commons, "Institution").get
      )
    } catch {
      case ex: java.util.NoSuchElementException =>
        throw new RuntimeException("Commons namespace not correctly set up: " +
            "make sure namespaces 'Creator' and 'Institution' are defined in " + 
            "settings/commonswiki-configuration.xml")
    }

  /**
   * Check if this WikiTitle is (1) on the Commons, and (2) contains metadata.
   */
  def titleContainsCommonsMetadata(title: WikiTitle):Boolean =
    (title.language == Language.Commons && commonsNamespacesContainingMetadata.contains(title.namespace))

  /**
   * Determine the file URL for a filename.
   * @param filename the name of the file.
   * @param language the wiki on which the file exists.
   * @return the file URL
   */
  def getFileURL(filename: String, language: Language):String = 
      language.baseUri + "/wiki/Special:FilePath/" + filename

  /**
   * Determine the thumbnail URL given a filename. Note that this is meaningless
   * for non-image files: MediaWiki will return the raw file whatever width is
   * provided.
   * @param filename the name of the file.
   * @param language the wiki on which this file exists.
   * @return the thumbnail URL
   */
  def getThumbnailURL(filename: String, language: Language):String =
      language.baseUri + "/wiki/Special:FilePath/" + filename + "?width=300"

  /**
    * Determine the file URL on DBpedia for a filename.
    *
    * @param filename the name of the file.
    * @param language the wiki on which the file exists.
    * @return the file URL
    */
  def getDbpediaFileURL(filename: String, language: Language): String = {
    val fileNamespaceIdentifier = Namespace.File.name(language)
    language.dbpediaUri + "/resource/" + fileNamespaceIdentifier + ":" + filename
  }

  /**
    * Collects all internal links from a Node
    */
  def collectInternalLinksFromNode(node : Node) : List[InternalLinkNode] =
  {
    node match
    {
      case linkNode : InternalLinkNode => List(linkNode)
      case _ => node.children.flatMap(collectInternalLinksFromNode)
    }
  }

  def collectParserFunctionsFromNode(node : Node) : List[ParserFunctionNode] =
  {
    node match
    {
      case parserFunctionNode : ParserFunctionNode => List(parserFunctionNode) ++ node.children.flatMap(collectParserFunctionsFromNode)
      case _ => node.children.flatMap(collectParserFunctionsFromNode)
    }
  }

  def collectTemplateParametersFromNode(node : Node) : List[TemplateParameterNode] =
  {
    node match
    {
      case templateParameterNode : TemplateParameterNode => List(templateParameterNode) ++ node.children.flatMap(collectTemplateParametersFromNode)
      case _ => node.children.flatMap(collectTemplateParametersFromNode)
    }
  }

  def collectTemplatesFromNodeTransitive(node: Node): List[TemplateNode] = {
    node match {
      case templateNode: TemplateNode => List(templateNode) ++ node.children.flatMap(collectTemplatesFromNodeTransitive)
      case _ => node.children.flatMap(collectTemplatesFromNodeTransitive)
    }
  }
    
}

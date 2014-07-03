package org.dbpedia.extraction.util

import org.dbpedia.extraction.mappings.Extractor
import org.dbpedia.extraction.wikiparser.{Namespace, WikiTitle}
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
   * Determine the image URL given a filename or page title. 
   *    - language: the language on which this Wiki exists (most should be on Language.Commons).
   *    - filename: the name of the file; usually aWikiTitle.encoded
   * TODO: replace filename with a WikiTitle: this will require fixing mappings.ImageExtractor. 
   * Returns both the image URL and the thumbnail URL.
   */
  def getImageURL(language: Language, filename: String):(String, String) = {
      val urlPrefix = "http://upload.wikimedia.org/wikipedia/" + language.wikiCode + "/"

      // TODO: URLDecoder.decode() translates '+' to space. Is that correct here?
      val decoded = URLDecoder.decode(filename, "UTF-8")
      
      val md = MessageDigest.getInstance("MD5")
      val messageDigest = md.digest(decoded.getBytes("UTF-8"))
      var md5 = (new BigInteger(1, messageDigest)).toString(16)

      // If the lenght of the MD5 hash is less than 32, then we should pad leading zeros to it, as converting it to
      // BigInteger will result in removing all leading zeros.
      // FIXME: this is the least efficient way of building a string.
      while (md5.length < 32)
        md5 = "0" + md5;

      val hash1 = md5.substring(0, 1)
      val hash2 = md5.substring(0, 2);

      val urlPart = hash1 + "/" + hash2 + "/" + filename
      val ext = if (filename.toLowerCase.endsWith(".svg")) ".png" else ""
      // TODO: add ".tif" as an extension that should have ".png" added to the end of the thumbnail.

      val imageUrl = urlPrefix + urlPart
      val thumbnailUrl = urlPrefix + "thumb/" + urlPart + "/200px-" + filename + ext

      (imageUrl, thumbnailUrl)
  }
}

package org.dbpedia.extraction.mappings

import java.util.logging.Logger
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.config.mappings.DisambiguationExtractorConfig
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import scala.language.reflectiveCalls

/**
 * Extracts disambiguation links.
 */
class DisambiguationExtractor(
  context : {
    def disambiguations : Disambiguations
    def ontology        : Ontology
    def language        : Language
  }
)
extends PageNodeExtractor
{
  @transient private val logger = Logger.getLogger(classOf[DisambiguationExtractor].getName)

  private val language = context.language

  // disambiguationTitlePartMap only covers the languages whose disambiguation title suffix is
  // known; a language missing from it (anything enabled for extraction/the live server that
  // hasn't been added here yet) must not crash extractor construction for every page of that
  // language -- fall back to stripping nothing rather than guessing a suffix.
  private val replaceString = DisambiguationExtractorConfig.disambiguationTitlePartMap.getOrElse(language.wikiCode, {
    logger.warning("No disambiguation title suffix configured for language '" + language.wikiCode +
      "'; disambiguation page titles for this language will not have their suffix stripped.")
    ""
  })

  val wikiPageDisambiguatesProperty = context.ontology.properties("wikiPageDisambiguates")

  override val datasets = Set(DBpediaDatasets.DisambiguationLinks)

  override def extract(page : PageNode, subjectUri : String) : Seq[Quad] =
  {
    if (page.title.namespace == Namespace.Main && (page.isDisambiguation || context.disambiguations.isDisambiguation(page.id)))
    {
      val allLinks = collectInternalLinks(page)
      
      // use upper case to be case-insensitive. this also means we regard all titles as acronyms.
      val cleanPageTitle = page.title.decoded.replace(replaceString, "").toUpperCase(language.locale)

      // Extract only links that contain the page title or that spell out the acronym page title
      val disambigLinks = allLinks.filter { linkNode =>
        val cleanLink = linkNode.destination.decoded.toUpperCase(language.locale)
        cleanLink.contains(cleanPageTitle)|| isAcronym(cleanPageTitle, cleanLink)
      }

      return disambigLinks.map { link =>
        new Quad(
          language,
          DBpediaDatasets.DisambiguationLinks,
          subjectUri,
          wikiPageDisambiguatesProperty,
          language.resourceUri.append(link.destination.decodedWithNamespace),
          link.sourceIri,
          null
        )
      }
    }

    Seq.empty
  }

  private def collectInternalLinks(node : Node) : List[InternalLinkNode] = node match
  {
    case linkNode : InternalLinkNode => List(linkNode)
    case _ => node.children.flatMap(collectInternalLinks)
  }

  private def isAcronym(acronym : String, destination : String) : Boolean =
  {
    val destinationWithoutDash = destination.replace("-", " ")
    
    val destinationList =
      if (destinationWithoutDash.contains(" ")) destinationWithoutDash.split(" ")
      else destinationWithoutDash.split("")
      
    var matchCount = 0
    for (word <- destinationList) {
      if (word.toUpperCase(language.locale).startsWith(acronym(matchCount).toString)) matchCount += 1
      if (matchCount == acronym.length) return true 
    }
    
    false
  }

}


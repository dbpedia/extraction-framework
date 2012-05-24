package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.config.mappings.DisambiguationExtractorConfig
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 * Extracts disambiguation links.
 */
class DisambiguationExtractor(
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends Extractor
{
  private val language = context.language

  private val replaceString = DisambiguationExtractorConfig.disambiguationTitlePartMap(language.wikiCode)

  val wikiPageDisambiguatesProperty = context.ontology.properties("wikiPageDisambiguates")

  override val datasets = Set(DBpediaDatasets.DisambiguationLinks)

  override def extract(page : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    if (page.title.namespace == Namespace.Main && page.isDisambiguation)
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
          link.sourceUri,
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


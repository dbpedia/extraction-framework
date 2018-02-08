package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.config.mappings.DisambiguationExtractorConfig
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

import scala.language.reflectiveCalls

/**
 * Extracts disambiguation links.
 */
@SoftwareAgentAnnotation(classOf[DisambiguationExtractor], AnnotationType.Extractor)
class DisambiguationExtractor(
  context : {
    def disambiguations : Disambiguations
    def ontology        : Ontology
    def language        : Language
  }
)
extends PageNodeExtractor
{
  private val language = context.language

  private val replaceString = DisambiguationExtractorConfig.disambiguationTitlePartMap(language.wikiCode)

  val wikiPageDisambiguatesProperty = context.ontology.properties("wikiPageDisambiguates")

  override val datasets = Set(DBpediaDatasets.DisambiguationLinks)

  private val qb = QuadBuilder(language, DBpediaDatasets.DisambiguationLinks, wikiPageDisambiguatesProperty, null)

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
        val lineText = linkNode.root.getOriginWikiText(linkNode.line).toUpperCase(language.locale).trim
        val titleSplits = cleanPageTitle.split(" ")

        // link text contains at least half of all word in the title
        val condition1a = titleSplits.count(s => cleanLink.contains(s)) >= titleSplits.size.toDouble / 2d
        // the link appears at the start of the line and the line contains more than half of the title words
        val condition1b = lineText.startsWith("[[" + cleanLink + "]]") && titleSplits.count(s => lineText.contains(s)) > titleSplits.size.toDouble / 2d
        // acronym appears in the whole line
        val condition1c = isAcronym(cleanPageTitle, lineText)
        // disallow redirects to page sections
        val condition2 = ! cleanLink.contains("#")

        condition2 && (condition1a || condition1b || condition1c)
      }

      //construct Quad
      qb.setSubject(subjectUri)
      qb.setExtractor(this.softwareAgentAnnotation)

      return disambigLinks.map { link => {
        val qbc = qb.clone
        qbc.setNodeRecord(link.getNodeRecord)
        qbc.setSourceUri(link.sourceIri)
        qbc.setValue(language.resourceUri.append(link.destination.decodedWithNamespace))
        qbc.getQuad
      }
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
    val destinationWithoutDash = destination.replace("-", " ").replaceAll("[^\\w\\s]", "")
    
    val destinationList =
      if (destinationWithoutDash.contains(" "))
        destinationWithoutDash.split(" ")
      else
        destinationWithoutDash.split("")
      
    var matchCount = 0
    for (word <- destinationList) {
      if (word.toUpperCase(language.locale).startsWith(acronym(matchCount).toString))
        matchCount += 1
      //acronyms matches minus one, if longer than 3 (this will allow for example: Harmful_algal_blooms for acronym HABs)
      if(acronym.length > 3 && matchCount == acronym.length-1)
        return true
      if (matchCount == acronym.length)
        return true
    }
    
    false
  }

}


package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Graph, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.config.mappings.DisambiguationExtractorConfig
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

/**
 * Extracts disambiguation links.
 */
class DisambiguationExtractor( context : {
                                   def ontology : Ontology
                                   def language : Language } ) extends Extractor
{
    private val language = context.language.wikiCode

    require(DisambiguationExtractorConfig.supportedLanguages.contains(language))

    private val replaceString = DisambiguationExtractorConfig.disambiguationTitlePartMap.getOrElse(language, "")

    val wikiPageDisambiguatesProperty = context.ontology.properties("wikiPageDisambiguates")

    override def extract(page : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if (page.title.namespace == Namespace.Main && page.isDisambiguation)
        {
            val allLinks = collectInternalLinks(page)
            val cleanPageTitle = page.title.decoded.replace(replaceString, "")

            // Extract only links that contain the page title or that spell out the acronym page title
            val disambigLinks = allLinks.filter(linkNode => linkNode.destination.decoded.contains(cleanPageTitle)
                                                            || isAcronym(cleanPageTitle, linkNode.destination.decoded))

            val quads = disambigLinks.map{link =>
                new Quad(context.language,
                         DBpediaDatasets.DisambiguationLinks,
                         subjectUri,
                         wikiPageDisambiguatesProperty,
                         context.language.resourceUri.append(link.destination.decodedWithNamespace),
                         link.sourceUri,
                         null)
            }

            return new Graph(quads)
        }

        new Graph()
    }

    private def collectInternalLinks(node : Node) : List[InternalLinkNode] = node match
    {
        case linkNode : InternalLinkNode => List(linkNode)
        case _ => node.children.flatMap(collectInternalLinks)
    }

    private def isAcronym(acronym : String, destination : String) : Boolean =
    {
        if (acronym != acronym.toUpperCase)
        {
            return false
        }

        val destinationWithoutDash = destination.replace("-", " ")
        val destinationList =
            if(destinationWithoutDash.contains(" "))
            {
                destinationWithoutDash.split(" ")
            }
            else
            {
                destinationWithoutDash.split("")
            }

        acronym.length == destinationList.foldLeft(0){ (matchCount, word) =>
            if ((matchCount < acronym.length) && word.toUpperCase.startsWith(acronym(matchCount).toString))
            {
                matchCount + 1
            }
            else
            {
                matchCount
            }
        }

    }

}


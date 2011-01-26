package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.OntologyNamespaces
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Graph, Quad}
import org.dbpedia.extraction.wikiparser._

/**
 * Extracts disambiguation links.
 */
class DisambiguationExtractor(extractionContext : ExtractionContext) extends Extractor
{
    val language = extractionContext.language.wikiCode

    val disambiguationTitlePart = Map(
        "en" -> " (disambiguation)",
        "el" -> " (αποσαφήνιση)",
        "de" -> " (Begriffsklärung)"
    )

    //require(Set("en", "el").contains(language))
    require(disambiguationTitlePart.keySet.contains(language))

    val wikiPageDisambiguatesProperty = extractionContext.ontology.getProperty("wikiPageDisambiguates")
                                        .getOrElse(throw new NoSuchElementException("Ontology property 'wikiPageDisambiguates' does not exist in DBpedia Ontology."))

    override def extract(page : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if (page.title.namespace == WikiTitle.Namespace.Main && page.isDisambiguation)
        {
            val allLinks = collectInternalLinks(page)
            val cleanPageTitle = page.title.decoded.replace(disambiguationTitlePart(language), "")

            // Extract only links that contain the page title or that spell out the acronym page title
            val disambigLinks = allLinks.filter(linkNode => linkNode.destination.decoded.contains(cleanPageTitle)
                                                            || isAcronym(cleanPageTitle, linkNode.destination.decoded))

            val quads = disambigLinks.map{link =>
                new Quad(extractionContext,
                         DBpediaDatasets.DisambiguationLinks,
                         subjectUri,
                         wikiPageDisambiguatesProperty,
                         OntologyNamespaces.getUri(link.destination.encoded, OntologyNamespaces.DBPEDIA_INSTANCE_NAMESPACE),
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


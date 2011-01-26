package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.Graph
import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.ontology.OntologyNamespaces
import java.io.File
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.io.OntologyReader
import org.dbpedia.extraction.util.Language

/**
 * The base class of all extractors.
 * Concrete extractors override the extract() method.
 * Each implementing class must be thread-safe.
 */
trait Extractor extends (PageNode => Graph)
{
    /**
     * Processes a wiki page and returns the extracted data.
     *
     * @param page The source page
     * @return A graph holding the extracted data
     */
    final def apply(page : PageNode) : Graph =
    {
        //If the page is not english, retrieve the title of the corresponding english article
        val enTitle = retrieveEnglishTitle(page).getOrElse(return new Graph())
        //Generate the page URI
        val uri = OntologyNamespaces.getUri(enTitle.encodedWithNamespace, OntologyNamespaces.DBPEDIA_INSTANCE_NAMESPACE)
        //Extract
        extract(page, uri, new PageContext())
    }

    /**
     * This function performs the extraction.
     *
     * @param page The source page
     * @param subjectUri The subject URI of the generated triples
     * @param context The page context which holds the state of the extraction.
     * @return A graph holding the extracted data
     */
    def extract(page : PageNode, subjectUri : String, context : PageContext) : Graph

    /**
     * Retrieves the corresponding english title of a non-english page.
     * If the given page is in english, its own title is returned.
     */
    private def retrieveEnglishTitle(page : PageNode) : Option[WikiTitle] =
    {
        return Some(page.title)
        if(page.title.language.wikiCode == "en")
        {
            return Some(page.title)
        }

        for(InterWikiLinkNode(destination, _, _) <- page.children.reverse if destination.isInterlanguageLink && destination.language.wikiCode == "en")
        {
            return Some(destination)
        }

        None
    }
}

/**
 * Creates new extractors.
 */
object Extractor
{
    /**
     * Creates a new extractor.
     *
     * @param ontologySource Source containing the ontology definitions
     * @param mappingsSource Source containing the mapping defintions
     * @param commonsSource Source containing the pages from Wikipedia Commons
     * @param articlesSource Source containing all articles
     * @param extractors List of extractor classes to be instantiated
     * @param language The language
     * @return The extractor
     */
    def load(ontologySource : Source, mappingsSource : Source, commonsSource : Source, articlesSource : Source,
             extractors : List[Class[Extractor]], language : Language) : Extractor =
    {
        val ontology = new OntologyReader().read(ontologySource)
        val redirects = Redirects.load(articlesSource, language)
        val context = new ExtractionContext(ontology, language, redirects, mappingsSource, commonsSource, articlesSource)

        val extractorInstances = extractors.map(_.getConstructor(classOf[ExtractionContext]).newInstance(context))

        new CompositeExtractor(extractorInstances)
    }
}

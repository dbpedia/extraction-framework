package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.Graph
import org.dbpedia.extraction.ontology.OntologyNamespaces
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.util.Language

/**
 * The base class of all extractors.
 * Concrete extractors override the extract() method.
 * Each implementing class must be thread-safe.
 * 
 * TODO: improve class hierarchy and object tree. It doesn't smell right that the apply method
 * is only called on the root of a tree of extractors, and extract is called on all others.
 * The code that generates the subject URI should be moved to PageNode. 
 * See Node.sourceUriPrefix which is somewhat similar to retrieveTitle below.
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
        val title = retrieveTitle(page).getOrElse(return new Graph())
        //Generate the page URI
        val uri = OntologyNamespaces.getResource(title.encodedWithNamespace, page.title.language)
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
     * Retrieves the corresponding title of a page.
     */
    private def retrieveTitle(page : PageNode) : Option[WikiTitle] =
    {
        //#int if all titles true, original name implied true
        val retrieveAllTitles=true
        
        //#int if all titles false option to extract with original name
        val retrieveOriginalName=true

        if (retrieveAllTitles || page.title.language.wikiCode == "en") return Some(page.title)

        // TODO comment: why reverse? probably just a performance thing. interwiki links are usually at the end.
        // TODO: are InterWikiLinkNode always top-level children? 
        for(InterWikiLinkNode(destination, _, _, _) <- page.children.reverse if destination.isInterLanguageLink && destination.language == Language.Default)
        {
            if (retrieveOriginalName) return Some(page.title)
            else return Some(destination)
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
     * TODO: using AnyRef here loses compile-time type safety.
     *
     * @param extractors List of extractor classes to be instantiated
     * @param context Any type of object that implements the required parameter methods for the extractors
     */
    def load(extractors : Traversable[Class[_ <: Extractor]], context : AnyRef) : Extractor =
    {
        val extractorInstances = extractors.map(_.getConstructor(classOf[AnyRef]).newInstance(context))
        new CompositeExtractor(extractorInstances)
    }
}

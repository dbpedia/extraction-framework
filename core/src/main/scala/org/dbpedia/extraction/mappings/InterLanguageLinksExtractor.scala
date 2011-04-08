package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.OntologyNamespaces
import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle, InterWikiLinkNode}


/**
 * Extracts interwiki links and creates owl:sameAs triples
 */
class InterLanguageLinksExtractor(extractionContext : ExtractionContext) extends Extractor
{
    private val language = extractionContext.language.wikiCode

    //extractionContext.ontology.getProperty("owl:sameAs").get //does not exist in ontology
    private val interLanguageLinksProperty = "http://dbpedia.org/ontology/interLanguageLinks" // extractionContext.ontology.getProperty("interLanguageLinks")" +
        //.getOrElse(throw new NoSuchElementException("Ontology property 'interLanguageLinks' does not exist in DBpedia Ontology."))

    private val sameAsMap = Map(
        "en" -> Set("el", "de", "co"),
        "el" -> Set("en", "de"),
        "de" -> Set("en", "el")
    )

    require( sameAsMap.keySet.contains(language), "Interlanguage Links supports the following languages: " + sameAsMap.keySet.mkString(", ")+"; not "+language)

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != WikiTitle.Namespace.Main) return new Graph()
        
        var quads = List[Quad]()

        retrieveTranslationTitles (node,sameAsMap(language)).foreach { tuple:(String, WikiTitle) =>
            val (tlang, title) = tuple
            quads ::= new Quad(extractionContext, DBpediaDatasets.SameAs, subjectUri, interLanguageLinksProperty,
                OntologyNamespaces.getResource(title.encodedWithNamespace,tlang), title.sourceUri, null)
        }
        new Graph(quads)
    }

    private def retrieveTranslationTitles(page : PageNode, trans_lang : Set[String]) : Map[String, WikiTitle] =
    {
        var results = Map[String, WikiTitle]()

        for(InterWikiLinkNode(destination, _, _) <- page.children.reverse if destination.isInterlanguageLink && trans_lang.contains(destination.language.wikiCode) )
        {
            results += (destination.language.wikiCode -> destination)
        }
        results
    }
}
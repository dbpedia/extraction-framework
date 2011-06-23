package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle, InterWikiLinkNode}
import org.dbpedia.extraction.config.mappings.InterLanguageLinksExtractorConfig
import org.dbpedia.extraction.ontology.{Ontology, OntologyNamespaces}
import org.dbpedia.extraction.util.Language


/**
 * Extracts interwiki links and creates owl:sameAs triples
 */
class InterLanguageLinksExtractor( extractionContext : {
                                       val ontology : Ontology
                                       val language : Language } ) extends Extractor
{
    private val language = extractionContext.language.wikiCode

    require( InterLanguageLinksExtractorConfig.supportedLanguages.contains(language), "Interlanguage Links supports the following languages: " + InterLanguageLinksExtractorConfig.supportedLanguages.mkString(", ")+"; not "+language)

    private val interLanguageLinksProperty = extractionContext.ontology.getProperty("owl:sameAs")
                                             .getOrElse(throw new NoSuchElementException("Ontology property 'owl:sameAs' does not exist in DBpedia Ontology."))
    //extractionContext.ontology.getProperty("interLanguageLinks")


    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != WikiTitle.Namespace.Main) return new Graph()
        
        var quads = List[Quad]()

        retrieveTranslationTitles (node,InterLanguageLinksExtractorConfig.intLinksMap(language)).foreach { tuple:(String, WikiTitle) =>
            val (tlang, title) = tuple
            quads ::= new Quad(extractionContext.language, DBpediaDatasets.SameAs, subjectUri, interLanguageLinksProperty,
                OntologyNamespaces.getResource(title.encodedWithNamespace,tlang), title.sourceUri, null)
        }
        new Graph(quads)
    }

    private def retrieveTranslationTitles(page : PageNode, trans_lang : Set[String]) : Map[String, WikiTitle] =
    {
        var results = Map[String, WikiTitle]()

        for(InterWikiLinkNode(destination, _, _, _) <- page.children.reverse if destination.isInterlanguageLink && trans_lang.contains(destination.language.wikiCode) )
        {
            results += (destination.language.wikiCode -> destination)
        }
        results
    }
}
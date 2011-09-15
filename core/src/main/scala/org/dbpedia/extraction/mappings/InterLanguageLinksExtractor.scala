package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle, InterWikiLinkNode}
import org.dbpedia.extraction.config.mappings.InterLanguageLinksExtractorConfig
import org.dbpedia.extraction.ontology.{Ontology, OntologyNamespaces}
import org.dbpedia.extraction.util.Language


/**
 * Extracts interwiki links and creates owl:sameAs triples
 */
class InterLanguageLinksExtractor( context : {
                                       def ontology : Ontology
                                       def language : Language } ) extends Extractor
{
    private val language = context.language.wikiCode

    require( InterLanguageLinksExtractorConfig.supportedLanguages.contains(language), "Interlanguage Links supports the following languages: " + InterLanguageLinksExtractorConfig.supportedLanguages.mkString(", ")+"; not "+language)

    private val interLanguageLinksProperty = context.ontology.getProperty("owl:sameAs")
                                             .getOrElse(throw new NoSuchElementException("Ontology property 'owl:sameAs' does not exist in DBpedia Ontology."))
    //context.ontology.getProperty("interLanguageLinks")


    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != WikiTitle.Namespace.Main) return new Graph()
        
        var quads = List[Quad]()

        retrieveTranslationTitles (node,InterLanguageLinksExtractorConfig.intLinksMap(language)).foreach { tuple:(Language, WikiTitle) =>
            val (tlang, title) = tuple
            quads ::= new Quad(context.language, DBpediaDatasets.SameAs, subjectUri, interLanguageLinksProperty,
                OntologyNamespaces.getResource(title.encodedWithNamespace, tlang), title.sourceUri, null)
        }
        new Graph(quads)
    }

    private def retrieveTranslationTitles(page : PageNode, trans_lang : Set[String]) : Map[Language, WikiTitle] =
    {
        var results = Map[Language, WikiTitle]()

        for(InterWikiLinkNode(destination, _, _, _) <- page.children.reverse if destination.isInterlanguageLink && trans_lang.contains(destination.language.wikiCode) )
        {
            results += (destination.language -> destination)
        }
        results
    }
}
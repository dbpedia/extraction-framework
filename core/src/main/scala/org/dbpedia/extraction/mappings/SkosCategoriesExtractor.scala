package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.OntologyNamespaces
import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser.{PageNode, Node, InternalLinkNode, WikiTitle}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces

/**
 * Extracts information about which concept is a category and how categories are related using the SKOS Vocabulary.
 */
class SkosCategoriesExtractor(extractionContext : ExtractionContext) extends Extractor
{
    private val language = extractionContext.language.wikiCode

    private val rdfTypeProperty = extractionContext.ontology.getProperty("rdf:type").get
    private val skosConceptClass = extractionContext.ontology.getClass("skos:Concept").get
    private val skosPrefLabelProperty = extractionContext.ontology.getProperty("skos:prefLabel").get
    private val skosBroaderProperty = extractionContext.ontology.getProperty("skos:broader").get
    private val skosRelatedProperty = extractionContext.ontology.getProperty("skos:related").get

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != WikiTitle.Namespace.Category) return new Graph()

        var quads = List[Quad]()

        quads ::= new Quad(extractionContext, DBpediaDatasets.SkosCategories, subjectUri, rdfTypeProperty, skosConceptClass.uri, node.sourceUri)
        quads ::= new Quad(extractionContext, DBpediaDatasets.SkosCategories, subjectUri, skosPrefLabelProperty, node.title.decoded, node.sourceUri, new Datatype("xsd:string"))

        for(link <- collectCategoryLinks(node))
        {
            val property =
                if(link.retrieveText.getOrElse("").startsWith(":"))
                {
                    skosRelatedProperty
                }
                else
                {
                    skosBroaderProperty
                }

            quads ::= new Quad(extractionContext, DBpediaDatasets.SkosCategories, subjectUri, property, getUri(link.destination), link.sourceUri)
        }

        new Graph(quads)
    }

    private def collectCategoryLinks(node : Node) : List[InternalLinkNode] =
    {
        node match
        {
            case linkNode : InternalLinkNode if linkNode.destination.namespace == WikiTitle.Namespace.Category => List(linkNode)
            case _ => node.children.flatMap(collectCategoryLinks)
        }
    }

    private def getUri(destination : WikiTitle) : String =
    {
        val categoryNamespace = Namespaces.getNameForNamespace(extractionContext.language, WikiTitle.Namespace.Category)
        
        OntologyNamespaces.getUri(categoryNamespace + ":" + destination.encoded, OntologyNamespaces.DBPEDIA_INSTANCE_NAMESPACE)
    }
}
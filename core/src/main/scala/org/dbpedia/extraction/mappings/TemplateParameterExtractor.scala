package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.OntologyNamespaces
import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle, TemplateParameterNode, Node}

/**
 * Extracts template variables from template pages (see http://en.wikipedia.org/wiki/Help:Template#Handling_parameters)
 */
class TemplateParameterExtractor(extractionContext : ExtractionContext) extends Extractor
{
    val templateParameterProperty = "http://dbpedia.org/ontology/templateUsesParameter"   // extractionContext.ontology.getProperty("tempalateUsesVariable")" +
        //.getOrElse(throw new NoSuchElementException("Ontology property 'wikiPageWikiLink' does not exist in DBpedia Ontology."))

    private val ignoreTemplates = Set("redirect", "seealso", "see_also", "main", "cquote", "chess diagram", "ipa", "lang")
    private val ignoreTemplatesRegex = List("cite.*".r, "citation.*".r, "assessment.*".r, "zh-.*".r, "llang.*".r, "IPA-.*".r)

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != WikiTitle.Namespace.Template ||
            ignoreTemplates.contains(node.title.decoded) ||
            ignoreTemplatesRegex.exists(regex => regex.unapplySeq(node.title.decoded).isDefined)
        ) return new Graph()
        
        var quads = List[Quad]()
        var parameters = List[String]()

        for (templatePar <- collectTemplateParameters(node) )  {
            parameters ::= templatePar.parameter
        }

        parameters.distinct.foreach(v => {
            quads ::= new Quad(extractionContext, DBpediaDatasets.TemplateVariables, subjectUri, templateParameterProperty,v,
                            node.sourceUri, extractionContext.ontology.getDatatype("xsd:string").get )
        })
        new Graph(quads)
    }

    private def collectTemplateParameters(node : Node) : List[TemplateParameterNode] =
    {
        node match
        {
            case tVar : TemplateParameterNode => List(tVar)
            case _ => node.children.flatMap(collectTemplateParameters)
        }
    }



}
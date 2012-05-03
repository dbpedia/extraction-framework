package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.{Ontology, OntologyNamespaces}
import org.dbpedia.extraction.util.Language

/**
 * Extracts template variables from template pages (see http://en.wikipedia.org/wiki/Help:Template#Handling_parameters)
 */
class TemplateParameterExtractor(context: { def ontology : Ontology; def language : Language } ) extends Extractor
{
    private val templateParameterProperty = OntologyNamespaces.getProperty("templateUsesParameter", context.language)

    val parameterRegex = """(?s)\{\{\{([^|}{<>]*)[|}<>]""".r
    
    override def extract(page : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if (page.title.namespace != Namespace.Template || page.isRedirect) return new Graph()

        var quads = List[Quad]()
        var parameters = List[String]()
        var linkParameters = List[String]()

        //try to get parameters inside internal links
        for (linkTemplatePar <- collectInternalLinks(page) )  {
            linkParameters ::= linkTemplatePar.toWikiText
        }

        linkParameters.distinct.foreach( link => {
            parameterRegex findAllIn link foreach (_ match {
                case parameterRegex (param) => parameters ::= param
                case _ => // ignore
            })
        })

        for (templatePar <- collectTemplateParameters(page) )  {
            parameters ::= templatePar.parameter
        }

        for (parameter <- parameters.distinct if parameter.nonEmpty) {
            // TODO: page.sourceUri does not include the line number
            quads ::= new Quad(context.language, DBpediaDatasets.TemplateVariables, subjectUri, templateParameterProperty, 
                parameter, page.sourceUri, context.ontology.datatypes("xsd:string"))
        }
        
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

    //TODO check inside links e.g. [[Image:{{{flag}}}|125px|border]]
    private def collectInternalLinks(node : Node) : List[InternalLinkNode] =
    {
        node match
        {
            case linkNode : InternalLinkNode => List(linkNode)
            case _ => node.children.flatMap(collectInternalLinks)
        }
    }

}
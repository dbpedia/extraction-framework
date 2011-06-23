package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{Graph, DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle, TemplateParameterNode, InternalLinkNode, Node}
import org.dbpedia.extraction.config.mappings.TemplateParameterExtractorConfig
import org.dbpedia.extraction.ontology.{Ontology, OntologyNamespaces}
import org.dbpedia.extraction.util.Language

/**
 * Extracts template variables from template pages (see http://en.wikipedia.org/wiki/Help:Template#Handling_parameters)
 */
class TemplateParameterExtractor( extractionContext : {
                                      val ontology : Ontology
                                      val language : Language }  ) extends Extractor
{
    private val templateParameterProperty = OntologyNamespaces.getProperty("templateUsesParameter", extractionContext.language.wikiCode)

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != WikiTitle.Namespace.Template ||
            TemplateParameterExtractorConfig.ignoreTemplates.contains(node.title.decoded) ||
            TemplateParameterExtractorConfig.ignoreTemplatesRegex.exists(regex => regex.unapplySeq(node.title.decoded).isDefined ||
            node.isRedirect)
        ) return new Graph()

        var quads = List[Quad]()
        var parameters = List[String]()
        var linkParameters = List[String]()

        //try to get parameters inside internal links
        for (linkTemplatePar <- collectInternalLinks(node) )  {
            linkParameters ::= linkTemplatePar.toWikiText
        }

        val parameterRegex = """(?s)\{\{\{([^|^}^{^<^>]*)[|}<>]""".r
        linkParameters.distinct.foreach( link => {
            parameterRegex findAllIn link foreach (_ match {
                case parameterRegex (param) => parameters::= param //.replace("}","").replace("|","")
                case _ => parameters
            })
        })

        for (templatePar <- collectTemplateParameters(node) )  {
            parameters ::= templatePar.parameter
        }

        parameters.distinct.foreach(v => {
            quads ::= new Quad(extractionContext.language, DBpediaDatasets.TemplateVariables, subjectUri, templateParameterProperty,v,
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
package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

/**
 * Extracts template variables from template pages (see http://en.wikipedia.org/wiki/Help:Template#Handling_parameters)
 */
@SoftwareAgentAnnotation(classOf[TemplateParameterExtractor], AnnotationType.Extractor)
class TemplateParameterExtractor(
  context: { 
    def ontology: Ontology
    def language : Language 
  } 
) 
extends PageNodeExtractor
{
  private val templateParameterProperty = context.language.propertyUri.append("templateUsesParameter")

  private val parameterRegex = """(?s)\{\{\{([^|}{<>]*)[|}<>]""".r
  
  override val datasets = Set(DBpediaDatasets.TemplateParameters)

  override def extract(page : PageNode, subjectUri : String): Seq[Quad] =
  {
    if (page.title.namespace != Namespace.Template || page.isRedirect) return Seq.empty

    val quads = new ArrayBuffer[Quad]()
    val parameters = new ArrayBuffer[String]()
    val linkParameters = new ArrayBuffer[String]()

    //try to get parameters inside internal links
    for (linkTemplatePar <- collectInternalLinks(page) )  {
      linkParameters += linkTemplatePar.toWikiText
    }

    linkParameters.distinct.foreach( link => {
      parameterRegex findAllIn link foreach (_ match {
          case parameterRegex (param) => parameters += param
          case _ => // ignore
      })
    })

    for (templatePar <- collectTemplateParameters(page) )  {
      parameters += templatePar.parameter
    }

    for (parameter <- parameters.distinct if parameter.nonEmpty) {
      // TODO: page.sourceUri does not include the line number
      quads += new Quad(context.language, DBpediaDatasets.TemplateParameters, subjectUri, templateParameterProperty,
          parameter, page.sourceIri, context.ontology.datatypes("xsd:string"))
    }

    quads
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
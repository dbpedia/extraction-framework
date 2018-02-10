package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
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
  private val xsdString = context.ontology.datatypes("xsd:string")
  
  override val datasets = Set(DBpediaDatasets.TemplateParameters)
  private val qb = QuadBuilder.stringPredicate(context.language, DBpediaDatasets.TemplateParameters, templateParameterProperty)
  qb.setExtractor(this.softwareAgentAnnotation)

  override def extract(page : PageNode, subjectUri : String): Seq[Quad] =
  {
    if (page.title.namespace != Namespace.Template || page.isRedirect) return Seq.empty


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


    val quads = collectTemplateParameters(page).groupBy(tp => tp.parameter).filter(tp => tp._1.nonEmpty).map(tp =>{
      val tps = tp._2.head
      qb.setNodeRecord(tps.getNodeRecord)
      qb.setSourceUri(tps.sourceIri)
      qb.setSubject(subjectUri)
      qb.setValue(tps.parameter)
      qb.getQuad
    })

    quads.toSeq
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
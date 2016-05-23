package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{Dataset, DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{ExtractorUtils, Language}
import org.dbpedia.extraction.wikiparser._

import scala.language.reflectiveCalls

/**
  * Extracts template variables from template pages (see http://en.wikipedia.org/wiki/Help:Template#Handling_parameters)
  */
class DraftMappingExtractor(context: {
  def ontology: Ontology
  def language : Language
}
                           )

  extends PageNodeExtractor
{
  private val templateParameterProperty = context.language.propertyUri.append("templateUsesWikidataProperty")

  val hintDatasetInst = new Dataset("template_mapping_hints_instance")
  val hintDataset = new Dataset("template_mapping_hints")
  val mapDataset = new Dataset("template_mappings")
  override val datasets = Set(hintDataset, mapDataset)

  override def extract(page : PageNode, subjectUri : String, pageContext : PageContext): Seq[Quad] = {
    if (!List(Namespace.Template, Namespace.Main).contains(page.title.namespace) || page.isRedirect) return Seq.empty

    //return getPropertyParserFunctions(page, subjectUri)

    val parserFunctions = ExtractorUtils.collectParserFunctionsFromNode(page)

    val propertyParserFuncions = parserFunctions.filter(p => (p.title.equalsIgnoreCase("#property") && p.children.nonEmpty && !p.children.head.toString.contains("from")))
    val propertyParserFuncionsHints = propertyParserFuncions.map(_.children.head.toString)
    val propertyParserFuncionsMappings = getTemplateMappingsFromPropertyParserFunc(propertyParserFuncions)


    val invokeFunc = parserFunctions.filter(p => p.title.equalsIgnoreCase("#invoke"))
    val wikidataParserFunc = invokeFunc.filter(p => p.children.headOption.get.toPlainText.toLowerCase.startsWith("wikidata"))
    val propertyLinkParserFunc = invokeFunc.filter(p => p.children.headOption.get.toPlainText.toLowerCase.startsWith("propertyLink"))

    val mappingQuads = propertyParserFuncionsMappings.map( p => {
      val value = p._1.toString + "=>" + p._2.toString
      new Quad(context.language, mapDataset, subjectUri, templateParameterProperty,
        value, page.sourceUri, context.ontology.datatypes("xsd:string")) })

    val parserFuncQuads = (propertyParserFuncions ++ wikidataParserFunc ++ propertyLinkParserFunc).map( p =>
      new Quad(context.language, hintDataset, subjectUri, templateParameterProperty,
        p.toWikiText, page.sourceUri, context.ontology.datatypes("xsd:string"))
    )

    val templateQuads = ExtractorUtils.collectTemplatesFromNodeTransitive(page)
      .filter(t => List("conditionalurl",/* "official_website",*/ "wikidatacheck").contains(t.title.encoded.toString.toLowerCase))
      .map(t => new Quad(context.language, hintDataset, subjectUri, templateParameterProperty,
        t.toWikiText, page.sourceUri, context.ontology.datatypes("xsd:string")))



    parserFuncQuads ++ templateQuads ++ mappingQuads

  }

  def getPropertyParserFunctions(page : PageNode, subjectUri : String) : List[Quad] = {
    val parserFunctions = ExtractorUtils.collectParserFunctionsFromNode(page)

    val propertyParserFuncions = parserFunctions.filter(p => (p.title.equalsIgnoreCase("#property") && p.children.nonEmpty && !p.children.head.toString.contains("from")))
    val propertyParserFuncionsHints = propertyParserFuncions.map(_.children.head.toString)

    val parserFuncQuads = (propertyParserFuncions ).map( p =>
      new Quad(context.language, hintDataset, subjectUri, templateParameterProperty,
        p.toWikiText, page.sourceUri, context.ontology.datatypes("xsd:string"))
    )

    parserFuncQuads
  }

  private def getTemplateMappingsFromPropertyParserFunc(propertyFunctions: Seq[ParserFunctionNode]) : Seq[(String, String)] = {

    for { p <- propertyFunctions;
          if (p.parent != null && p.parent.children.size >= 2);
          parameterSiblings = ExtractorUtils.collectTemplateParametersFromNode(p.parent);
          if (parameterSiblings.size == 1)


    } yield (parameterSiblings.head.parameter -> p.children.head.toPlainText)

  }

  def getPropertyTuples(node: Node) : String= {
    ""
  }



}
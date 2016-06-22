package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.dataparser.InfoboxMappingsExtractorConfig._
import org.dbpedia.extraction.destinations.{Dataset, Quad}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{InfoboxMappingsUtils, ExtractorUtils, Language}
import org.dbpedia.extraction.wikiparser.{Namespace, TextNode, ParserFunctionNode, PageNode}
import org.dbpedia.extraction.mappings.InfoboxMappingsExtractor

import scala.collection.mutable.ListBuffer

/**
  * Created by aditya on 6/21/16.
  */
class InfoboxMappingsTemplateExtractor (context: {
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

    val parserFunctions = ExtractorUtils.collectParserFunctionsFromNode(page)

    val propertyParserFuncions = parserFunctions.filter(p => (p.title.equalsIgnoreCase("#property") && p.children.nonEmpty && !p.children.head.toString.contains("from")))
    val propertyParserFuncionsHints = propertyParserFuncions.map(_.children.head.toString)
    val propertyParserFuncionsMappings = getTemplateMappingsFromPropertyParserFunc(propertyParserFuncions)
    val try12 = InfoboxExtractor.collectTemplates(page)
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

  def ltrim(s: String) = s.replaceAll("^\\s+", "")
  def rtrim(s: String) = s.replaceAll("\\s+$", "")

  def getTuplesFromConditionalExpressions(page : PageNode, lang : Language) : List[(String, String, String)] = {

    val templateNodes = ExtractorUtils.collectTemplatesFromNodeTransitive(page)
    val infoboxes = templateNodes.filter(p => p.title.toString().contains(infoboxNameMap.get(lang.wikiCode).getOrElse("Infobox")))
    var answer = ListBuffer[(String, String, String)]()

    // Loop through all infoboxes
    infoboxes.foreach(f = infobox => {
      // Loop through each property
      for (propertyNode <- infobox.children) {
        // The child should be a ParserFunctionNode with title #if...
        if (propertyNode.children.head.isInstanceOf[ParserFunctionNode] && propertyNode.children.head.asInstanceOf[ParserFunctionNode].title.substring(0, 3) == "#if") {
          // Get list of all equivalent terms in the conditional expression foreg {{#ifeq: "string1" | "String2" | {{#property:p193}} | "string3"} }}
          // should return string1, string2, string3 along with the associated property
          var temp_answer = getListOfEquivalentTermsAndProperty(propertyNode.children.head.asInstanceOf[ParserFunctionNode])
          var cleansed_list = temp_answer._1.filter(str => !ltrim(rtrim(str)).isEmpty)
          if (temp_answer._2 != "ERROR" && temp_answer._2 != "") {
            for (term <- cleansed_list) {
              answer += new Tuple3(infobox.title.decoded, rtrim(ltrim(term)), temp_answer._2)
            }
          }

        }
      }
    })

    answer.toList
  }

  // returns a list of equivalent terms in a nested conditional expression
  def getListOfEquivalentTermsAndProperty(parserNode : ParserFunctionNode) : (Array[String], String) = {

    var answerList = Array[String]()
    var property = ""

    // To recurse only if the ParserFunctionNode is a conditional expression node
    if (parserNode.title.substring(0,3) != "#if"){
      if( parserNode.title == "#invoke")
        property = InfoboxMappingsUtils.extract_property(parserNode.children.head.toWikiText, parserNode.title)
      else if (parserNode.title == "#property")
        property = InfoboxMappingsUtils.extract_property(parserNode.toWikiText, parserNode.title)
      return (answerList, property)
    }

    // Cases which have 4 parts  eg {{#ifeq: string 1 | string 2 | value if equal | value if unequal }}
    for( child <- parserNode.children){
      if( child.isInstanceOf[TextNode]){
        answerList = answerList ++ child.asInstanceOf[TextNode].toWikiText.split('|')
      } else if( child.isInstanceOf[ParserFunctionNode]) {
        var child_answer =  getListOfEquivalentTermsAndProperty(child.asInstanceOf[ParserFunctionNode])
        answerList = answerList ++ child_answer._1

        // There should be only a single property
        if ( property != "" && child_answer._2 != ""){
          property = "ERROR"
        } else if (property == ""){
          property = child_answer._2
        }
      }
    }

    (answerList.filter(str => str != "" && str != " " ), property)
  }

  private def getTemplateMappingsFromPropertyParserFunc(propertyFunctions: Seq[ParserFunctionNode]) : Seq[(String, String)] = {

    for { p <- propertyFunctions;
          if (p.parent != null && p.parent.children.size >= 2);
          parameterSiblings = ExtractorUtils.collectTemplateParametersFromNode(p.parent);
          if (parameterSiblings.size == 1)


    } yield (parameterSiblings.head.parameter -> p.children.head.toPlainText)

  }

}

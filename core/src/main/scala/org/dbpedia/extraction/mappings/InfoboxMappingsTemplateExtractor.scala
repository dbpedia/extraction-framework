package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.dataparser.InfoboxMappingsExtractorConfig._
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, Dataset}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{ExtractorUtils, InfoboxMappingsUtils, Language}
import org.dbpedia.extraction.wikiparser._

import scala.language.reflectiveCalls
import scala.collection.mutable.ArrayBuffer

@SoftwareAgentAnnotation(classOf[InfoboxMappingsTemplateExtractor], AnnotationType.Extractor)
class InfoboxMappingsTemplateExtractor (context: {
  def ontology: Ontology
  def language : Language
  def redirects: Redirects
}
                                       )

  extends WikiPageExtractor
{
  private val templateParameterProperty = context.language.propertyUri.append("templateUsesWikidataProperty")

  val hintDatasetInst = DBpediaDatasets.TemplateMappingsHintsInstance
  val hintDataset = DBpediaDatasets.TemplateMappingsHints
  val mapDataset = DBpediaDatasets.TemplateMappings
  override val datasets = Set(hintDataset, mapDataset)

  override def extract(page : WikiPage, subjectUri : String): Seq[Quad] = {
    if (!List(Namespace.Template, Namespace.Main).contains(page.title.namespace) ) return Seq.empty

    val simpleParser = WikiParser.getInstance("simple")
    val parserFunctions = ExtractorUtils.collectParserFunctionsFromNode(simpleParser.apply(page, context.redirects).orNull)

    val propertyParserFuncions = parserFunctions.filter(p => p.title.equalsIgnoreCase("#property") && p.children.nonEmpty && !p.children.head.toString.contains("from"))
    val propertyParserFuncionsHints = propertyParserFuncions.map(_.children.head.toString)
    val propertyParserFuncionsMappings = getTemplateMappingsFromPropertyParserFunc(propertyParserFuncions)
    val invokeFunc = parserFunctions.filter(p => p.title.equalsIgnoreCase("#invoke"))
    val wikidataParserFunc = invokeFunc.filter(p => p.children.headOption.get.toPlainText.toLowerCase.startsWith("wikidata"))
    val propertyLinkParserFunc = invokeFunc.filter(p => p.children.headOption.get.toPlainText.toLowerCase.startsWith("propertyLink"))


    val mappingQuads = propertyParserFuncionsMappings.map( p => {
      val value = p._1.toString + "=>" + p._2.toString
      new Quad(context.language, mapDataset, subjectUri, templateParameterProperty,
        value, page.sourceIri, context.ontology.datatypes("xsd:string")) })

    val parserFuncQuads = (propertyParserFuncions ++ wikidataParserFunc ++ propertyLinkParserFunc).map( p =>
      new Quad(context.language, hintDataset, subjectUri, templateParameterProperty,
        p.toWikiText, page.sourceIri, context.ontology.datatypes("xsd:string"))
    )

    val templateQuads = ExtractorUtils.collectTemplatesFromNodeTransitive(simpleParser.apply(page, context.redirects).orNull)
      .filter(t => List("conditionalurl",/* "official_website",*/ "wikidatacheck").contains(t.title.encoded.toString.toLowerCase))
      .map(t => new Quad(context.language, hintDataset, subjectUri, templateParameterProperty,
        t.toWikiText, page.sourceIri, context.ontology.datatypes("xsd:string")))

    parserFuncQuads ++ templateQuads ++ mappingQuads

  }

  def ltrim(s: String) = s.replaceAll("^\\s+", "")
  def rtrim(s: String) = s.replaceAll("\\s+$", "")


  def checkForPropertySyntax(str : String) : Boolean = {
    if( str.length > 0 && (str.charAt(0) == 'p' || str.charAt(0) == 'P') && InfoboxMappingsUtils.isNumber(str.substring(1))){
      return true
    }
    return false
  }

  def isBlackListed( str : String) : Boolean = {
    // List all the black listed words in lower case only
    var blacklistWords = Set("fetch_wikidata", "getvalue", "wikidata", "both", "property")
    if (blacklistWords.contains(str.toLowerCase))
      return true
    return false
  }
  def getTuplesFromConditionalExpressions(page : WikiPage, lang : Language) : List[(String, String, String)] = {

    var simpleParser = WikiParser.getInstance("simple")
    var swebleParser = WikiParser.getInstance("sweble")
    var tempPageSweble = new WikiPage(page.title, page.source)
    var tempPageSimple = new WikiPage(page.title, cleanUp(page.source))
    val templateNodesSweble = ExtractorUtils.collectTemplatesFromNodeTransitive(swebleParser.apply(tempPageSweble, context.redirects).orNull)
    val templateNodesSimple = ExtractorUtils.collectTemplatesFromNodeTransitive(simpleParser.apply(tempPageSimple, context.redirects).orNull)

    val infoboxesSweble = templateNodesSweble.filter(p => p.title.toString().contains(infoboxNameMap.getOrElse(lang.wikiCode, "Infobox")))
    val infoboxesSimple = templateNodesSimple.filter(p => p.title.toString().contains(infoboxNameMap.getOrElse(lang.wikiCode, "Infobox")))

    var answerSweble = Set[(String, String, String)]()
    var answerSimple = Set[(String, String, String)]()


    // Loop through all infoboxes
    infoboxesSweble.foreach(infobox => {
      // Loop through each property
      for (propertyNode <- infobox.children) {

        for( child <- propertyNode.children){
          // Get list of all equivalent terms in the conditional expression foreg {{#ifeq: "string1" | "String2" | {{#property:p193}} | "string3"} }}
          // should return string1, string2, string3 along with the associated property
          var temp_answer = getListOfEquivalentTermsAndPropertySweble(child, false)
          var cleansed_list = temp_answer._1.filter(str => !ltrim(rtrim(str)).isEmpty)
          if (temp_answer._2 != "ERROR" && temp_answer._2 != "" && checkForPropertySyntax(temp_answer._2)) {
            for (term <- cleansed_list) {
              if ( !isBlackListed(term))
                answerSweble += new Tuple3(infobox.title.decoded, rtrim(ltrim(term)), temp_answer._2)
            }
          }
        }
      }
    })
    // Loop through all infoboxes
    infoboxesSimple.foreach(infobox => {
      // Loop through each property
      for (propertyNode <- infobox.children) {
        // The child should be a ParserFunctionNode with title #if...
        if (propertyNode.children.length > 0 && propertyNode.children.head.isInstanceOf[ParserFunctionNode] && propertyNode.children.head.asInstanceOf[ParserFunctionNode].title.substring(0, 3) == "#if") {
          // Get list of all equivalent terms in the conditional expression foreg {{#ifeq: "string1" | "String2" | {{#property:p193}} | "string3"} }}
          // should return string1, string2, string3 along with the associated property
          var temp_answer = getListOfEquivalentTermsAndPropertySimple(propertyNode.children.head.asInstanceOf[ParserFunctionNode])
          var cleansed_list = temp_answer._1.filter(str => !ltrim(rtrim(str)).isEmpty)
          if (temp_answer._2 != "ERROR" && temp_answer._2 != "") {
            for (term <- cleansed_list) {
              if ( !isBlackListed(term))
              answerSimple += new Tuple3(infobox.title.decoded, rtrim(ltrim(term)), temp_answer._2)
            }
          }

        }
      }
    })

      (answerSimple ++ answerSweble).toList
  }

  // tempNode is the node whose children is to be processed
  // answerList is the list of terms extracted
  // getProp is flag when set indicates to only extract the property and no more terms
  def proccessChildren(tempNode : Node, answerList : ArrayBuffer[String], prop : String, getProp : Boolean) :  String = {

    var property = prop
    for( child <- tempNode.children){

      var child_answer =  getListOfEquivalentTermsAndPropertySweble(child, getProp)
      answerList ++= child_answer._1

      // There should be only a single property
      if ( property != "" && child_answer._2 != "" && property != child_answer._2){
        property = "ERROR"
      } else if (property == ""){
        property = child_answer._2
      }

    }

    return  property
  }
  // returns a list of equivalent terms in a nested conditional expression
  def getListOfEquivalentTermsAndPropertySweble(node : Node, getProp : Boolean) : (Array[String], String) = {

    var answerList = ArrayBuffer[String]()
    var property = ""

     if ( node.isInstanceOf[PropertyNode]){
      var propertyNode = node.asInstanceOf[PropertyNode]
       if ( !InfoboxMappingsUtils.isNumber(propertyNode.key) && !getProp ){
         answerList += propertyNode.key
       }
      property = proccessChildren(propertyNode, answerList, property, getProp)

     } else if (node.isInstanceOf[TemplateNode]){
      var templateNode = node.asInstanceOf[TemplateNode]

       if ( templateNode.title.decoded.charAt(0) == '#' && templateNode.title.decoded.substring(0,3) != "#if" ) {
         var index = templateNode.title.decoded.indexOf(':')
         if(checkForPropertySyntax(templateNode.title.decoded.substring(index+1)) )
          property = templateNode.title.decoded.substring(index+1)
         else
           property = proccessChildren(templateNode, answerList, property, true)

       } else {

         // Tricky, if the template name should be included in the list or not
         // Since sometimes template names are uninformative words like "both" etc
         if ( templateNode.title.decoded.substring(0,3) != "#if" && !getProp)
          answerList += templateNode.title.decoded

         property = proccessChildren(templateNode, answerList, property, getProp)
       }

     } else if (node.isInstanceOf[TemplateParameterNode]){
      var templateParameterNode = node.asInstanceOf[TemplateParameterNode]
       if ( !getProp)
        answerList += templateParameterNode.parameter
      property = proccessChildren(templateParameterNode, answerList, property, getProp)


     } else if( node.isInstanceOf[ParserFunctionNode]){
       var parserNode = node.asInstanceOf[ParserFunctionNode]
       property = proccessChildren(parserNode, answerList, property, getProp)
     } else if ( node.isInstanceOf[TextNode]){
      var text : String = ltrim(rtrim(node.asInstanceOf[TextNode].text))
      if(text == null || text == "" || text.length < 2 ){
        return (answerList.toArray, property)
      } else {
        if ( text.contains("|") && !getProp){
          answerList = answerList ++ text.split('|').filter(str => !checkForPropertySyntax(str))

          var propertyArr =  text.split('|').filter(str => checkForPropertySyntax(str))
          if ( propertyArr.length  == 1) property = propertyArr.head
          else if (propertyArr.length > 1) property = "ERROR"
        } else {
          if(checkForPropertySyntax(text) )
            property += text
          else if(!getProp)
            answerList += text
        }
        return (answerList.toArray, property)
      }

    }

    (answerList.toArray.filter(str => str != "" && str != " " ), property)
  }

  // returns a list of equivalent terms in a nested conditional expression
  def getListOfEquivalentTermsAndPropertySimple(parserNode : ParserFunctionNode) : (Array[String], String) = {

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
        var child_answer =  getListOfEquivalentTermsAndPropertySimple(child.asInstanceOf[ParserFunctionNode])
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


  def cleanUp(str : String) : String = {
    var text = str
    var  pattern1 = """\{\{\{([0-9A-Za-z\_]+)\|?\}\}\}"""
    var  pattern2 = """\{\{\{([0-9A-Za-z\_]+)\|?\}\}\}\}\}\}"""
    text  = str.replaceAll(pattern2, "$1")
    text.replaceAll(pattern1, "$1")
  }

  private def getTemplateMappingsFromPropertyParserFunc(propertyFunctions: Seq[ParserFunctionNode]) : Seq[(String, String)] = {

    for { p <- propertyFunctions;
          if (p.parent != null && p.parent.children.size >= 2);
          parameterSiblings = ExtractorUtils.collectTemplateParametersFromNode(p.parent);
          if (parameterSiblings.size == 1)


    } yield (parameterSiblings.head.parameter -> p.children.head.toPlainText)

  }

}

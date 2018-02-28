package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.dataparser.InfoboxMappingsExtractorConfig._
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, Dataset}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
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
  val hintDatasetInst: Dataset = DBpediaDatasets.TemplateMappingsHintsInstance
  val hintDataset: Dataset = DBpediaDatasets.TemplateMappingsHints
  val mapDataset: Dataset = DBpediaDatasets.TemplateMappings
  override val datasets = Set(hintDataset, mapDataset)

  val xsdString = context.ontology.datatypes("xsd:string")
  private val templateParameterProperty = context.language.propertyUri.append("templateUsesWikidataProperty")
  private val qb = QuadBuilder.stringPredicate(context.language, hintDataset, templateParameterProperty)
  qb.setExtractor(this.softwareAgentAnnotation)
  qb.setDatatype(xsdString)

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

    qb.setSourceUri(page.sourceIri)
    qb.setNodeRecord(page.getNodeRecord)
    qb.setSubject(subjectUri)

    val mappingQuads = propertyParserFuncionsMappings.map( p => {
      val qbs = qb.clone
      qbs.setDataset(mapDataset)
      qbs.setValue(p._1.toString + "=>" + p._2.toString)
      qbs.getQuad
    })

    val parserFuncQuads = (propertyParserFuncions ++ wikidataParserFunc ++ propertyLinkParserFunc).map( p => {
      qb.setValue(p.toWikiText)
      qb.getQuad
    })

    val templateQuads = ExtractorUtils.collectTemplatesFromNodeTransitive(simpleParser.apply(page, context.redirects).orNull)
      .filter(t => List("conditionalurl",/* "official_website",*/ "wikidatacheck").contains(t.title.encoded.toString.toLowerCase))
      .map(t => {
        qb.setValue(t.toWikiText)
        qb.getQuad
      })

    parserFuncQuads ++ templateQuads ++ mappingQuads
  }

  private def getTemplateMappingsFromPropertyParserFunc(propertyFunctions: Seq[ParserFunctionNode]) : Seq[(String, String)] = {

    for {p <- propertyFunctions
         if p.parent != null && p.parent.children.size >= 2
         parameterSiblings = ExtractorUtils.collectTemplateParametersFromNode(p.parent)
         if parameterSiblings.size == 1


    } yield parameterSiblings.head.parameter -> p.children.head.toPlainText

  }

  def ltrim(s: String): String = s.replaceAll("^\\s+", "")

  def rtrim(s: String): String = s.replaceAll("\\s+$", "")

  def checkForPropertySyntax(str : String) : Boolean = {
    if( str.length > 0 && (str.charAt(0) == 'p' || str.charAt(0) == 'P') && InfoboxMappingsUtils.isNumber(str.substring(1))){
      return true
    }
    false
  }

  def isBlackListed( str : String) : Boolean = {
    // List all the black listed words in lower case only
    val blacklistWords = Set("fetch_wikidata", "getvalue", "wikidata", "both", "property")
    if (blacklistWords.contains(str.toLowerCase))
      return true
    false
  }
  // tempNode is the node whose children is to be processed
  // answerList is the list of terms extracted
  // getProp is flag when set indicates to only extract the property and no more terms
  def proccessChildren(tempNode : Node, answerList : ArrayBuffer[String], prop : String, getProp : Boolean) :  String = {

    var property = prop
    for( child <- tempNode.children){

      val child_answer =  getListOfEquivalentTermsAndPropertySweble(child, getProp)
      answerList ++= child_answer._1

      // There should be only a single property
      if ( property != "" && child_answer._2 != "" && property != child_answer._2){
        property = "ERROR"
      } else if (property == ""){
        property = child_answer._2
      }

    }
    property
  }

  // returns a list of equivalent terms in a nested conditional expression
  def getListOfEquivalentTermsAndPropertySweble(node : Node, getProp : Boolean) : (Array[String], String) = {

    var answerList = ArrayBuffer[String]()
    var property = ""

     if ( node.isInstanceOf[PropertyNode]){
      val propertyNode = node.asInstanceOf[PropertyNode]
       if ( !InfoboxMappingsUtils.isNumber(propertyNode.key) && !getProp ){
         answerList += propertyNode.key
       }
      property = proccessChildren(propertyNode, answerList, property, getProp)

     } else if (node.isInstanceOf[TemplateNode]){
       val templateNode = node.asInstanceOf[TemplateNode]

       if ( templateNode.title.decoded.charAt(0) == '#' && templateNode.title.decoded.substring(0,3) != "#if" ) {
         val index = templateNode.title.decoded.indexOf(':')
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
       val templateParameterNode = node.asInstanceOf[TemplateParameterNode]
       if ( !getProp)
        answerList += templateParameterNode.parameter
      property = proccessChildren(templateParameterNode, answerList, property, getProp)


     } else if( node.isInstanceOf[ParserFunctionNode]){
       val parserNode = node.asInstanceOf[ParserFunctionNode]
       property = proccessChildren(parserNode, answerList, property, getProp)
     } else if ( node.isInstanceOf[TextNode]){
      var text : String = ltrim(rtrim(node.asInstanceOf[TextNode].text))
      if(text == null || text == "" || text.length < 2 ){
        return (answerList.toArray, property)
      }
      else {
        if ( text.contains("|") && !getProp){
          val propertyArr = text.split('|').filter(str => checkForPropertySyntax(str))
          if ( propertyArr.length  == 1)
            property = propertyArr.head
          else if (propertyArr.length > 1)
            property = "ERROR"
          answerList = answerList ++ propertyArr
        }
        else {
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
        val child_answer =  getListOfEquivalentTermsAndPropertySimple(child.asInstanceOf[ParserFunctionNode])
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
    val  pattern1 = """\{\{\{([0-9A-Za-z\_]+)\|?\}\}\}"""
    val  pattern2 = """\{\{\{\{\{([0-9A-Za-z\_]+)\|?\}\}\}\}\}\}"""
    str.replaceAll(pattern2, "$1").replaceAll(pattern1, "$1")
  }

}

package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.dataparser.InfoboxMappingsExtractorConfig._
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, Dataset}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{ExtractorUtils, InfoboxMappingsUtils, Language}
import org.dbpedia.extraction.wikiparser.Node
import org.dbpedia.extraction.wikiparser._

import scala.collection.mutable.ListBuffer
import scala.language.reflectiveCalls
import scala.util.matching.Regex

/**
  * Extracts template variables from template pages (see http://en.wikipedia.org/wiki/Help:Template#Handling_parameters)
  */
@SoftwareAgentAnnotation(classOf[InfoboxMappingsExtractor], AnnotationType.Extractor)
class InfoboxMappingsExtractor(context: {
  def ontology: Ontology
  def language : Language
}
                           )

  extends PageNodeExtractor
{
  private val templateParameterProperty = context.language.propertyUri.append("templateUsesWikidataProperty")

  val hintDatasetInst = DBpediaDatasets.TemplateMappingsHintsInstance
  val hintDataset = DBpediaDatasets.TemplateMappingsHints
  val mapDataset = DBpediaDatasets.TemplateMappings
  override val datasets = Set(hintDataset, mapDataset)

  override def extract(page : PageNode, subjectUri : String): Seq[Quad] = {
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
        value, page.sourceIri, context.ontology.datatypes("xsd:string")) })

    val parserFuncQuads = (propertyParserFuncions ++ wikidataParserFunc ++ propertyLinkParserFunc).map( p =>
      new Quad(context.language, hintDataset, subjectUri, templateParameterProperty,
        p.toWikiText, page.sourceIri, context.ontology.datatypes("xsd:string"))
    )

    val templateQuads = ExtractorUtils.collectTemplatesFromNodeTransitive(page)
      .filter(t => List("conditionalurl",/* "official_website",*/ "wikidatacheck").contains(t.title.encoded.toString.toLowerCase))
      .map(t => new Quad(context.language, hintDataset, subjectUri, templateParameterProperty,
        t.toWikiText, page.sourceIri, context.ontology.datatypes("xsd:string")))



    parserFuncQuads ++ templateQuads ++ mappingQuads

  }

  def extractTuples(page : PageNode, lang: Language ) : List[(String,String, String)] = {
    val allProperties = InfoboxMappingsUtils.getAllPropertiesInInfobox(page, lang)
    var completedTuples = getDirectTemplateWikidataMappings(page, lang)
    completedTuples = completedTuples ++ getInvokeTuples(page)
    completedTuples = completedTuples ++ getPropertyTuples(page)

    var incompleteTuples = ListBuffer[(String, String, String)]()

    for ( prop <- allProperties){
      incompleteTuples += new Tuple3(prop._1, "?", prop._2 )
    }
    completedTuples ++ incompleteTuples.toList
  }

  def reduceChildrenToString(propertyNode: PropertyNode) : String = {
    var answer = ""
    propertyNode.children.foreach(x => answer += x.toWikiText)
    return answer
  }

  def getPropertyTuples(page : PageNode) : List[(String,String, String)] = {
    val parserFunctions = ExtractorUtils.collectParserFunctionsFromNode(page)

    val propertyParserFunctions = parserFunctions.filter(p => (p.title.equalsIgnoreCase("#property") &&  //To filter out parser functions with title #property
      p.children.nonEmpty && // Ignore those that have no children
      !p.children.head.toString.contains("from") && // Ignore those that have "from" in them for eg {#property:P1308|from=Q824910}
      p.parent.isInstanceOf[PropertyNode])) // Parent needs to be a PropertyNode to get the key

     (propertyParserFunctions ).map( p =>
      new Tuple3(p.parent.asInstanceOf[PropertyNode].parent.asInstanceOf[TemplateNode].title.decoded, p.parent.asInstanceOf[PropertyNode].key, InfoboxMappingsUtils.extract_property(p.toWikiText, "#property")))

  }

   def getInvokeTuples(page : PageNode) : List[(String, String, String)] = {
    val parserFunctions = ExtractorUtils.collectParserFunctionsFromNode(page)
    var invokeFunc = parserFunctions.filter(p => ( p.title.equalsIgnoreCase("#invoke")))
     invokeFunc = invokeFunc.filter(p => InfoboxMappingsUtils.extract_property(p.children.head.toWikiText, "#invoke") != "" &&  p.parent.isInstanceOf[PropertyNode])
    invokeFunc.map( p => new Tuple3(p.parent.asInstanceOf[PropertyNode].parent.asInstanceOf[TemplateNode].title.decoded, p.parent.asInstanceOf[PropertyNode].key, InfoboxMappingsUtils.extract_property(p.children.head.toWikiText, "#invoke")))
  }

  def checkDirectTemplateWikidataMappings(propertyNode : PropertyNode, lang: Language) : Boolean = {

    for( x <- directTemplateMapsToWikidata.getOrElse(lang.wikiCode, Map())){
      if (reduceChildrenToString(propertyNode).contains(x._1))
        return true
    }

    return false

  }

  def getDirectTemplateWikidataMappings(page : PageNode, lang : Language) : List[(String, String, String)] = {
    val templateNodes = ExtractorUtils.collectTemplatesFromNodeTransitive(page)
    val infoboxes = templateNodes.filter(p => p.title.toString().contains(infoboxNameMap.get(lang.wikiCode).getOrElse("Infobox")))

    var website_rows = ListBuffer[PropertyNode]()
    infoboxes.foreach(x => {
      website_rows = website_rows ++ x.children.filter(p => checkDirectTemplateWikidataMappings(p, lang) )
    })
    var answer = ListBuffer[(String, String, String)]()
    for ( x <- website_rows){
     answer += new Tuple3(x.parent.asInstanceOf[TemplateNode].title.decoded, x.key.toString, directTemplateMapsToWikidata.getOrElse(lang.wikiCode, Map()).getOrElse(x.children.head.asInstanceOf[TemplateNode].title.decoded, "") )
    }
    answer.toList
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


package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.{ExtractionRecorder, RecordCause}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.transform.Quad

import collection.mutable.{ArrayBuffer, ListBuffer}
import org.dbpedia.extraction.ontology.datatypes.{Datatype, DimensionDatatype}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.dataparser._
import org.dbpedia.extraction.util.RichString.wrapString
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.config.mappings.InfoboxExtractorConfig
import org.dbpedia.extraction.config.dataparser.DataParserConfig
import org.dbpedia.iri.UriUtils

import scala.collection.mutable
import scala.language.reflectiveCalls
import scala.reflect.ClassTag

/**
 * This extractor extracts all properties from all infoboxes.
 * Extracted information is represented using properties in the http://xx.dbpedia.org/property/
 * namespace (where xx is the language code).
 * The names of the these properties directly reflect the name of the Wikipedia infobox property.
 * Property names are not cleaned or merged.
 * Property types are not part of a subsumption hierarchy and there is no consistent ontology for the infobox dataset.
 * The infobox extractor performs only a minimal amount of property value clean-up, e.g., by converting a value like “June 2009” to the XML Schema format “2009–06”.
 * You should therefore use the infobox dataset only if your application requires complete coverage of all Wikipeda properties and you are prepared to accept relatively noisy data.
 */
@SoftwareAgentAnnotation(classOf[InfoboxExtractor], AnnotationType.Extractor)
class InfoboxExtractor(
  context : {
    def ontology : Ontology
    def language : Language
    def redirects : Redirects
    def recorder[T: ClassTag] : ExtractionRecorder[T]
  } 
) 
extends PageNodeExtractor
{
    private val recorder = context.recorder[PageNode]
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private val ontology = context.ontology
    
    private val language = context.language

    private val wikiCode = language.wikiCode

    //private val minPropertyCount = InfoboxExtractorConfig.minPropertyCount

    //private val minRatioOfExplicitPropertyKeys = InfoboxExtractorConfig.minRatioOfExplicitPropertyKeys

    private val ignoreTemplates = InfoboxExtractorConfig.ignoreTemplates

    private val ignoreTemplatesRegex = InfoboxExtractorConfig.ignoreTemplatesRegex

    private val ignoreProperties = InfoboxExtractorConfig.ignoreProperties

    private val labelProperty = ontology.properties("rdfs:label")
    private val typeProperty = ontology.properties("rdf:type")
    private val propertyClass = ontology.classes("rdf:Property")
    private val rdfLangStrDt = ontology.datatypes("rdf:langString")

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Regexes
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    // TODO: i18n
    private val RankRegex = InfoboxExtractorConfig.RankRegex

    private val SplitWordsRegex = InfoboxExtractorConfig.SplitWordsRegex

    private val TrailingNumberRegex = InfoboxExtractorConfig.TrailingNumberRegex

    private val splitPropertyNodeRegexInfobox = if (DataParserConfig.splitPropertyNodeRegexInfobox.contains(wikiCode))
                                                  DataParserConfig.splitPropertyNodeRegexInfobox(wikiCode)
                                                else DataParserConfig.splitPropertyNodeRegexInfobox("en")
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Parsers
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private val unitValueParsers = ontology.datatypes.values
                                   .filter(_.isInstanceOf[DimensionDatatype])
                                   .map(dimension => new UnitValueParser(context, dimension, true))

    private val intParser = new IntegerParser(context, true, validRange = i => i % 1 == 0)

    private val doubleParser = new DoubleParser(context, true)

    private val dateTimeParsers = List("xsd:date", "xsd:gMonthYear", "xsd:gMonthDay", "xsd:gMonth" /*, "xsd:gYear", "xsd:gDay"*/)
                                  .map(datatype => new DateTimeParser(context, new Datatype(datatype), true))

    private val singleGeoCoordinateParser = new SingleGeoCoordinateParser(context)
                                  
    private val objectParser = new ObjectParser(context, true)

    private val linkParser = new LinkParser(true)

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // State
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private val seenProperties = mutable.HashSet[String]()
    
    override val datasets = Set(DBpediaDatasets.InfoboxProperties, DBpediaDatasets.InfoboxPropertiesExtended, DBpediaDatasets.InfoboxTest, DBpediaDatasets.InfoboxPropertyDefinitions)

    override def extract(node : PageNode, subjectUri : String) : Seq[Quad] =
    {
      if(node.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(node.title)) return Seq.empty

      val quads = new ArrayBuffer[Quad]()

      /** Retrieve all templates on the page which are not ignored */
      for { template <- InfoboxExtractor.collectTemplates(node)
        resolvedTitle = context.redirects.resolve(template.title).decoded.toLowerCase
        if !ignoreTemplates.contains(resolvedTitle)
        if !ignoreTemplatesRegex.exists(regex => regex.unapplySeq(resolvedTitle).isDefined)
      }
      {
        val propertyList = template.children.filterNot(property => ignoreProperties.getOrElse(wikiCode, ignoreProperties("en")).contains(property.key.toLowerCase))

        // check how many property keys are explicitly defined
        val countExplicitPropertyKeys = propertyList.count(property => !property.key.forall(_.isDigit))
        if ((countExplicitPropertyKeys >= InfoboxExtractorConfig.minPropertyCount)
          && (countExplicitPropertyKeys.toDouble / propertyList.size) > InfoboxExtractorConfig.minRatioOfExplicitPropertyKeys)
        {
          for(property <- propertyList; if !property.key.forall(_.isDigit)) {
            // TODO clean HTML

            val cleanedPropertyNode = NodeUtil.removeParentheses(property)

            val splitPropertyNodes = NodeUtil.splitPropertyNode(cleanedPropertyNode, splitPropertyNodeRegexInfobox)
            for(splitNode <- splitPropertyNodes)
            {
              val zw = extractValue(splitNode)
              for(parseResults <- zw; if parseResults.nonEmpty) {
                val propertyUri = getPropertyUri(property.key)
                for (pr <- parseResults) {
                  try {

                    //only the first result will be forwarded to the official infobox properties dataset
                    if(pr == parseResults.head){
                      quads += new Quad(language, DBpediaDatasets.InfoboxProperties, subjectUri, propertyUri, pr.value, splitNode.sourceIri, pr.unit.orNull)
                      quads += new Quad(language, DBpediaDatasets.InfoboxPropertiesExtended, subjectUri, propertyUri, pr.value, splitNode.sourceIri, pr.unit.orNull)
                    }
                    else
                      quads += new Quad(language, DBpediaDatasets.InfoboxPropertiesExtended, subjectUri, propertyUri, pr.value, splitNode.sourceIri, pr.unit.orNull)

                    if (InfoboxExtractorConfig.extractTemplateStatistics) {
                      val stat_template = language.resourceUri.append(template.title.decodedWithNamespace)
                      val stat_property = property.key.replace("\n", " ").replace("\t", " ").trim
                      quads += new Quad(language, DBpediaDatasets.InfoboxTest, subjectUri, stat_template,
                        stat_property, node.sourceIri, ontology.datatypes("xsd:string"))
                    }
                  }
                  catch {
                    case ex: IllegalArgumentException => recorder.failedRecord(node, ex, node.title.language)
                  }
                }
                seenProperties.synchronized {
                  if (!seenProperties.contains(propertyUri)) {
                    val propertyLabel = getPropertyLabel(property.key)
                    seenProperties += propertyUri
                    quads += new Quad(language, DBpediaDatasets.InfoboxPropertyDefinitions, propertyUri, typeProperty, propertyClass.uri, splitNode.sourceIri)
                    quads += new Quad(language, DBpediaDatasets.InfoboxPropertyDefinitions, propertyUri, labelProperty, propertyLabel, splitNode.sourceIri, rdfLangStrDt)
                  }
                }
              }
            }
          }
        }
      }

      quads
    }

    private def extractValue(node : PropertyNode) : List[List[ParseResult[String]]] =
    {
      var results = new ListBuffer[List[ParseResult[String]]]()

      // TODO don't convert to SI units (what happens to {{convert|25|kg}} ?)
      extractUnitValue(node).foreach(result => results += List(result))
      extractDates(node) match
      {
          case dates if dates.nonEmpty => results += dates
          case _ =>
      }
      extractSingleCoordinate(node).foreach(result =>  results += List(result))
      extractNumber(node).foreach(result =>  results += List(result))
      extractRankNumber(node).foreach(result => results += List(result))
      extractLinks(node) match
      {
          case links if links.nonEmpty => results += links
          case _ =>
      }
      val stringRes = StringParser.parseWithProvenance(node).map(value => ParseResult(value.value, None, Some(rdfLangStrDt)))
      results += stringRes.toList

      results.toList
    }

    private def extractUnitValue(node : PropertyNode) : Option[ParseResult[String]] =
    {
        val unitValues =
        for (unitValueParser <- unitValueParsers;
             pr <- unitValueParser.parseWithProvenance(node) )
             yield pr

        if (unitValues.size > 1)
        {
            StringParser.parseWithProvenance(node).map(value => ParseResult(value.value, None, Some(rdfLangStrDt)))
        }
        else if (unitValues.size == 1)
        {
            val pr = unitValues.head
            Some(ParseResult(pr.value.toString, None, pr.unit))
        }
        else
        {
            None
        }
    }

    private def extractNumber(node : PropertyNode) : Option[ParseResult[String]] =
    {
        intParser.parseWithProvenance(node).foreach(value => return Some(ParseResult(value.value.toString, None, Some(new Datatype("xsd:integer")))))
        doubleParser.parseWithProvenance(node).foreach(value => return Some(ParseResult(value.value.toString, None, Some(new Datatype("xsd:double")))))
        None
    }

    private def extractRankNumber(node : PropertyNode) : Option[ParseResult[String]] =
    {
        StringParser.parseWithProvenance(node).getOrElse(return None).value.toString match
        {
            case RankRegex(number) => Some(ParseResult(number, None, Some(new Datatype("xsd:integer"))))
            case _ => None
        }
    }
    
    private def extractSingleCoordinate(node : PropertyNode) : Option[ParseResult[String]] =
    {
        singleGeoCoordinateParser.parseWithProvenance(node).foreach(value => return Some(ParseResult(value.value.toDouble.toString, None, Some(new Datatype("xsd:double")))))
        None
    }

    private def extractDates(node : PropertyNode) : List[ParseResult[String]] =
    {
        for(date <- extractDate(node))
        {
            return List(date)
        }

        //Split the node. Note that even if some of these hyphens are looking similar, they represent different Unicode numbers.
        val splitNodes = NodeUtil.splitPropertyNode(node, "(—|–|-|&mdash;|&ndash;|,|;)")

        splitNodes.flatMap(extractDate) match
        {
            case dates if dates.size == splitNodes.size => dates
            case _ => List.empty
        }
    }
    
    private def extractDate(node : PropertyNode) : Option[ParseResult[String]] =
    {
        for (dateTimeParser <- dateTimeParsers;
             date <- dateTimeParser.parseWithProvenance(node))
        {
            return Some(ParseResult(date.value.toString, None, Some(date.value.datatype)))
        }
        None
    }

    private def extractLinks(node : PropertyNode) : List[ParseResult[String]] =
    {
        val splitNodes = NodeUtil.splitPropertyNode(node, """\s*\W+\s*""")

        splitNodes.flatMap(splitNode => objectParser.parseWithProvenance(splitNode)) match
        {
            // TODO: explain why we check links.size == splitNodes.size
            case links if links.size == splitNodes.size =>
              return links
            case _ => List.empty
        }
        
        splitNodes.flatMap(splitNode => linkParser.parseWithProvenance(splitNode)) match
        {
            // TODO: explain why we check links.size == splitNodes.size
            case links if links.size == splitNodes.size =>
              links.map(x => UriUtils.cleanLink(x.value)).collect{case Some(link) => ParseResult(link)}
            case _ => List.empty
        }
    }
    
    private def getPropertyUri(key : String) : String =
    {
        // convert property key to camelCase
        var result = key.toLowerCase(language.locale).trim
        result = result.toCamelCase(SplitWordsRegex, language.locale)

        // Rename Properties like LeaderName1, LeaderName2, ... to LeaderName
        result = TrailingNumberRegex.replaceFirstIn(result, "")

        result = WikiUtil.cleanSpace(result)

        language.propertyUri.append(result)
    }

    private def getPropertyLabel(key : String) : String =
    {
        // convert property key to camelCase
        var result = key

        result = result.replace("_", " ")
        
        // Rename Properties like LeaderName1, LeaderName2, ... to LeaderName
        result = TrailingNumberRegex.replaceFirstIn(result, "")

        result
    }


}

object InfoboxExtractor {

    def collectTemplates(node : Node) : List[TemplateNode] =
    {
        node match
        {
            case templateNode : TemplateNode => List(templateNode)
            case _ => node.children.flatMap(collectTemplates)
        }
    }

    def collectProperties(node : Node) : List[PropertyNode] =
    {
        node match
        {
            case propertyNode : PropertyNode => List(propertyNode)
            case _ => node.children.flatMap(collectProperties)
        }
    }
}

package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.{ExtractionLogger, ExtractionRecorder}
import org.dbpedia.extraction.config.provenance.{DBpediaDatasets, ExtractorRecord}
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}

import collection.mutable.{ArrayBuffer, ListBuffer}
import org.dbpedia.extraction.ontology.datatypes.{Datatype, DimensionDatatype}
import org.dbpedia.extraction.wikiparser.{Node, _}
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
@SoftwareAgentAnnotation(classOf[ExtendedInfoboxExtractor], AnnotationType.Extractor)
class ExtendedInfoboxExtractor(
  context : {
    def ontology : Ontology
    def language : Language
    def redirects : Redirects
  }
)
extends PageNodeExtractor
{

  private val logger = ExtractionLogger.getLogger(getClass, context.language)

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
    private val xsdString = ontology.datatypes("xsd:string")

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

      val graph = extractNode(node, subjectUri)
      graph
    }

  /**
    *
    * @param template
    * @param subjectUri
    * @param writeToBaseDestination
    * @return
    */
  private def extractTemplate(template: TemplateNode, subjectUri: String, writeToBaseDestination: Boolean): Seq[Quad] = {
    val quads: ArrayBuffer[Quad] = new ArrayBuffer[Quad]()
    val propertyList = template.children.filterNot(property => ignoreProperties.getOrElse(wikiCode, ignoreProperties("en")).contains(property.key.toLowerCase))

    // check how many property keys are explicitly defined
    val countExplicitPropertyKeys = propertyList.count(property => !property.key.forall(_.isDigit))
    if ((countExplicitPropertyKeys >= InfoboxExtractorConfig.minPropertyCount)
      && (countExplicitPropertyKeys.toDouble / propertyList.size) > InfoboxExtractorConfig.minRatioOfExplicitPropertyKeys) {
      for (property <- propertyList; if !property.key.forall(_.isDigit)) {
        // TODO clean HTML

        val propertyUri = getPropertyUri(property.key)
        val cleanedPropertyNode = NodeUtil.removeParentheses(property)
        val baseQuadBuilder = new QuadBuilder(Some(subjectUri), Some(propertyUri), None, None, Some(language), None, None)

        val splitPropertyNodes = NodeUtil.splitPropertyNode(cleanedPropertyNode, splitPropertyNodeRegexInfobox)
        for (splitNode <- splitPropertyNodes) {
          baseQuadBuilder.setNodeRecord(splitNode.getNodeRecord)
          baseQuadBuilder.setSourceUri(splitNode.sourceIri)

          val zw = extractValue(splitNode)
          for (parseResults <- zw; if parseResults.nonEmpty) {
            for (pr <- parseResults) {
              //set extractor metadata
              val er = ExtractorRecord(
                this.softwareAgentAnnotation,
                pr.provenance.toList,
                Some(splitPropertyNodes.size),
                Some(property.key),
                Some(template.title),
                splitNode.containedTemplateNames()
              )

              try {
                //only the first result will be forwarded to the official infobox properties dataset
                if (pr == parseResults.head) {
                  val qb1 = baseQuadBuilder.clone
                  er.setParseRecord(pr.provenance)
                  qb1.setExtractor(er)
                  qb1.setValue(pr.value)
                  qb1.setDatatype(pr.unit.orNull)
                  qb1.setDataset(DBpediaDatasets.InfoboxProperties)

                  //only if so instructed do we write to DBpediaDatasets.InfoboxProperties
                  if(writeToBaseDestination)
                    quads ++= List(qb1.getQuad)

                  //this quad moves into InfoboxPropertiesExtended
                  val qb2 = qb1.clone
                  qb2.setDataset(DBpediaDatasets.InfoboxPropertiesExtended)
                  quads ++= List(qb2.getQuad)
                }
                else {
                  val qbs = baseQuadBuilder.clone
                  er.setParseRecord(pr.provenance)
                  qbs.setExtractor(er)
                  qbs.setValue(pr.value)
                  qbs.setDatatype(pr.unit.orNull)
                  qbs.setDataset(DBpediaDatasets.InfoboxPropertiesExtended)
                  quads += qbs.getQuad
                }

                if (InfoboxExtractorConfig.extractTemplateStatistics) {
                  val stat_template = language.resourceUri.append(template.title.decodedWithNamespace)
                  val stat_property = property.key.replace("\n", " ").replace("\t", " ").trim
                  //no metadata needed here
                  val qbs = baseQuadBuilder.clone
                  qbs.setExtractor(this.softwareAgentAnnotation)
                  qbs.setDataset(DBpediaDatasets.InfoboxTest)
                  qbs.setPredicate(stat_template)
                  qbs.setValue(stat_property)
                  qbs.setDatatype(xsdString)
                  quads += qbs.getQuad
                }
              }
              catch {
                case ex: IllegalArgumentException => logger.debug(template.root, language, ex)
              }
            }
            seenProperties.synchronized {
              if (!seenProperties.contains(propertyUri)) {
                val propertyLabel = getPropertyLabel(property.key)
                seenProperties += propertyUri

                val er = ExtractorRecord(
                  this.softwareAgentAnnotation,
                  Seq(),
                  None,
                  Some(property.key),
                  Some(template.title)
                )
                val qb1 = new QuadBuilder(Some(propertyUri), None, None, Some(splitNode.sourceIri), language, None, None, None)
                qb1.setExtractor(er)
                qb1.setNodeRecord(splitNode.getNodeRecord)
                qb1.setPredicate(typeProperty)
                qb1.setValue(propertyClass.uri)
                qb1.setDataset(DBpediaDatasets.InfoboxPropertyDefinitions)

                val qb2 = qb1.clone
                qb2.setPredicate(labelProperty)
                qb2.setValue(propertyLabel)
                qb2.setDatatype(rdfLangStrDt)

                quads ++= List(qb1.getQuad, qb2.getQuad)
              }
            }
          }
        }
      }
    }
    quads
  }

  /**
    * Extracts a data from a node.
    * Recursively traverses it children if the node itself does not contain any useful data.
    */
  private def extractNode(node : Node, subjectUri : String) : Seq[Quad] =
  {
    //Try to extract data from the node itself
    var graph = node match
    {
      case templateNode : TemplateNode =>
        val resolvedTitle = context.redirects.resolve(templateNode.title).decoded.toLowerCase
        if( !ignoreTemplates.contains(resolvedTitle) && !ignoreTemplatesRegex.exists(regex => regex.unapplySeq(resolvedTitle).isDefined)) {
          extractTemplate(templateNode, subjectUri, templateNode.children.exists(c => c.line != templateNode.line))
        }
        else
          Seq.empty
      case _ => Seq.empty
    }

    //Check the result and return it if non-empty.
    //Otherwise continue with extracting the children of the current node.
    graph ++= node.children.flatMap(child => extractNode(child, subjectUri))
    graph
  }

  /**
    * Will try to execute every parser for the given property node.
    * Depending on the switch parameter, this will return either
    * a List of the first successful result or a list of all successful parse results
    * @param node - the property node to parse
    * @return
    */
  protected def extractValue(node : PropertyNode) : List[List[ParseResult[String]]] = extractValue(node, extractOnlyFirstSuccessfulResult = false)


  /**
    * Will try to execute every parser for the given property node.
    * Depending on the switch parameter, this will return either
    * a List of the first successful result or a list of all successful parse results
    * @param node - the property node to parse
    * @param extractOnlyFirstSuccessfulResult
    * @return
    */
  protected def extractValue(node : PropertyNode, extractOnlyFirstSuccessfulResult: Boolean) : List[List[ParseResult[String]]] =
    {
      var results = new ListBuffer[List[ParseResult[String]]]()

      /**
        * Check method, which returns as soon as the first result was successfully parsed
        */
      val successBreak = (res: List[ParseResult[String]]) => {
        if(extractOnlyFirstSuccessfulResult && res != null && res.nonEmpty)
          return List(res)
      }

      // TODO don't convert to SI units (what happens to {{convert|25|kg}} ?)
      extractUnitValue(node).foreach(result => {
        successBreak(List(result))
        results += List(result)
      })
      extractDates(node) match
      {
          case dates if dates.nonEmpty =>
            successBreak(dates)
            results += dates
          case _ =>
      }
      extractSingleCoordinate(node).foreach(result => {
        successBreak(List(result))
        results += List(result)
      })
      extractNumber(node).foreach(result => {
        successBreak(List(result))
        results += List(result)
      })
      extractRankNumber(node).foreach(result => {
        successBreak(List(result))
        results += List(result)
      })
      extractLinks(node) match
      {
          case links if links.nonEmpty =>
            successBreak(links)
            results += links
          case _ =>
      }
      val stringRes = StringParser.parseWithProvenance(node).map(value => ParseResult(value.value, None, Some(rdfLangStrDt), value.provenance))
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
            StringParser.parseWithProvenance(node).map(value => ParseResult(value.value, None, Some(rdfLangStrDt), value.provenance))
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
        intParser.parseWithProvenance(node).foreach(value => return Some(ParseResult(value.value.toString, None, Some(new Datatype("xsd:integer")), value.provenance)))
        doubleParser.parseWithProvenance(node).foreach(value => return Some(ParseResult(value.value.toString, None, Some(new Datatype("xsd:double")), value.provenance)))
        None
    }

    private def extractRankNumber(node : PropertyNode) : Option[ParseResult[String]] =
    {
        StringParser.parseWithProvenance(node) match{
          case Some(pr) => pr.value.toString match {
            case RankRegex(number) => Some(ParseResult(number, None, Some(new Datatype("xsd:integer")), pr.provenance))
            case _ => None
          }
          case None => None
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

object ExtendedInfoboxExtractor {

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

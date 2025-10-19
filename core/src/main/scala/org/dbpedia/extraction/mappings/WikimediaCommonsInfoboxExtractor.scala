package org.dbpedia.extraction.mappings
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.transform.Quad

import collection.mutable.HashSet
import org.dbpedia.extraction.ontology.datatypes.{Datatype, DimensionDatatype}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.dataparser._
import org.dbpedia.extraction.util.RichString.wrapString
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.config.mappings.InfoboxExtractorConfig

import scala.collection.mutable.ArrayBuffer
import org.dbpedia.extraction.config.dataparser.DataParserConfig
import org.dbpedia.iri.UriUtils

import scala.language.reflectiveCalls
/**
  Wikimedia Commmons Extractor extracts data
  from Wikimedia Commons articles infoboxes.
  It is based on Infobox Extractor (this is the
  copy of it but with permissions extraction)
 **/
class WikimediaCommonsInfoboxExtractor(context : {
  def ontology : Ontology
  def language : Language
  def redirects : Redirects
}
                               )
  extends PageNodeExtractor
{
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Configuration
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private val ontology = context.ontology

  private val language = context.language

  private val wikiCode = language.wikiCode

  private val minPropertyCount = InfoboxExtractorConfig.minPropertyCount

  private val minRatioOfExplicitPropertyKeys = InfoboxExtractorConfig.minRatioOfExplicitPropertyKeys

  private val ignoreTemplates = InfoboxExtractorConfig.ignoreTemplates

  private val ignoreTemplatesRegex = InfoboxExtractorConfig.ignoreTemplatesRegex

  private val ignoreProperties = InfoboxExtractorConfig.ignoreProperties
  private val xsdStringDt = ontology.datatypes("xsd:string")

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
    DataParserConfig.splitPropertyNodeRegexInfobox.get(wikiCode).get
  else DataParserConfig.splitPropertyNodeRegexInfobox.get("en").get
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // Parsers
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private val unitValueParsers = ontology.datatypes.values
    .filter(_.isInstanceOf[DimensionDatatype])
    .map(dimension => new UnitValueParser(context, dimension, true))

  private val intParser = new IntegerParser(context, true, validRange = (i => i%1==0))

  private val doubleParser = new DoubleParser(context, true)

  private val dateTimeParsers = List("xsd:date", "xsd:gMonthYear", "xsd:gMonthDay", "xsd:gMonth" /*, "xsd:gYear", "xsd:gDay"*/)
    .map(datatype => new DateTimeParser(context, new Datatype(datatype), true))

  private val singleGeoCoordinateParser = new SingleGeoCoordinateParser(context)

  private val objectParser = new ObjectParser(context, true)

  private val linkParser = new LinkParser(true)

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  // State
  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private val seenProperties = HashSet[String]()

  override val datasets = Set(DBpediaDatasets.InfoboxProperties, DBpediaDatasets.InfoboxTest, DBpediaDatasets.InfoboxPropertyDefinitions)

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
      val propertyList = template.children.filterNot(property => ignoreProperties.get(wikiCode).getOrElse(ignoreProperties("en")).contains(property.key.toLowerCase))

      var propertiesFound = false

      // check how many property keys are explicitly defined
      val countExplicitPropertyKeys = propertyList.count(property => !property.key.forall(_.isDigit))
      if ((countExplicitPropertyKeys >= minPropertyCount) && (countExplicitPropertyKeys.toDouble / propertyList.size) > minRatioOfExplicitPropertyKeys)
      {
        for(property <- propertyList; if (!property.key.forall(_.isDigit))) {
          // TODO clean HTML

          val cleanedPropertyNode = NodeUtil.removeParentheses(property)

          val splitPropertyNodes = NodeUtil.splitPropertyNode(cleanedPropertyNode, splitPropertyNodeRegexInfobox)

          //for(splitNode <- splitPropertyNodes; pr <- extractValue(splitNode); if pr.unit.nonEmpty)
          //sh: removed pr.unit.nonEmpty as it kicked out all objectproperty triples from wikilinks,
          // didn't test for further side-effects seems to work
          for(splitNode <- splitPropertyNodes; pr <- extractValue(splitNode))
          {
            val propertyUri = getPropertyUri(property.key)
            try
            {
              //sh: pr.unit should be empty (null) for objects
              quads += new Quad(language, DBpediaDatasets.InfoboxProperties, subjectUri, propertyUri, pr.value, splitNode.sourceIri, pr.unit.getOrElse(null))

              if (InfoboxExtractorConfig.extractTemplateStatistics)
              {
                val stat_template = language.resourceUri.append(template.title.decodedWithNamespace)
                val stat_property = property.key.replace("\n", " ").replace("\t", " ").trim
                quads += new Quad(language, DBpediaDatasets.InfoboxTest, subjectUri, stat_template,
                  stat_property, node.sourceIri, ontology.datatypes("xsd:string"))
              }
            }
            catch
              {
                case ex : IllegalArgumentException => println(ex)
              }
            propertiesFound = true
            seenProperties.synchronized
            {
              if (!seenProperties.contains(propertyUri))
              {
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

    quads
  }

  private def extractValue(node : PropertyNode) : List[ParseResult[String]] =
  {
    // TODO don't convert to SI units (what happens to {{convert|25|kg}} ?)
    extractUnitValue(node).foreach(result => return List(result))
    extractPermission(node).foreach(result => return List(result))
    extractDates(node) match
    {
      case dates if dates.nonEmpty => return dates
      case _ =>
    }
    extractSingleCoordinate(node).foreach(result =>  return List(result))
    extractNumber(node).foreach(result =>  return List(result))
    extractRankNumber(node).foreach(result => return List(result))
    extractLinks(node) match
    {
      case links if links.nonEmpty => { return links}
      case _ =>
    }
    val res = StringParser.parse(node).map(value => ParseResult(value.value, None, Some(rdfLangStrDt))).toList
    res
  }

  private def extractPermission(node: PropertyNode) : Option[ParseResult[String]] = {
      if (node.key.contains("permission")) {
        if(node.children.nonEmpty){
          node.children.head match {
            case item: TemplateNode => {
              if (!item.title.decoded.isEmpty) {
                val permissionLink = "http://purl.oclc.org/NET/rdflicense/" + item.title.decoded
                return Some(ParseResult(permissionLink))
              }
            }
          }
        }
      }
      None
  }

  private def extractUnitValue(node : PropertyNode) : Option[ParseResult[String]] =
  {
    val unitValues =
      for (unitValueParser <- unitValueParsers;
           pr <- unitValueParser.parse(node) )
        yield pr

    if (unitValues.size > 1)
    {
      StringParser.parse(node).map(value => ParseResult(value.value, None, Some(rdfLangStrDt)))
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
    intParser.parse(node).foreach(value => return Some(ParseResult(value.value.toString, None, Some(new Datatype("xsd:integer")))))
    doubleParser.parse(node).foreach(value => return Some(ParseResult(value.value.toString, None, Some(new Datatype("xsd:double")))))
    None
  }

  private def extractRankNumber(node : PropertyNode) : Option[ParseResult[String]] =
  {
    StringParser.parse(node) match
    {
      case Some(RankRegex(number)) => Some(ParseResult(number, None, Some(new Datatype("xsd:integer"))))
      case _ => None
    }
  }

  private def extractSingleCoordinate(node : PropertyNode) : Option[ParseResult[String]] =
  {
    singleGeoCoordinateParser.parse(node).foreach(value => return Some(ParseResult(value.value.toDouble.toString, None, Some(new Datatype("xsd:double")))))
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

    splitNodes.flatMap(extractDate(_)) match
    {
      case dates if dates.size == splitNodes.size => dates
      case _ => List.empty
    }
  }

  private def extractDate(node : PropertyNode) : Option[ParseResult[String]] =
  {
    for (dateTimeParser <- dateTimeParsers;
         date <- dateTimeParser.parse(node))
    {
      return Some(ParseResult(date.value.toString, None, Some(date.value.datatype)))
    }
    None
  }

  private def extractLinks(node : PropertyNode) : List[ParseResult[String]] =
  {
    val splitNodes = NodeUtil.splitPropertyNode(node, """\s*\W+\s*""")

    splitNodes.flatMap(splitNode => objectParser.parse(splitNode)) match
    {
      // TODO: explain why we check links.size == splitNodes.size
      case links if links.size == splitNodes.size => return links
      case _ => List.empty
    }

    splitNodes.flatMap(splitNode => linkParser.parse(splitNode)) match
    {
      // TODO: explain why we check links.size == splitNodes.size
      case links if links.size == splitNodes.size => links.map(x => UriUtils.cleanLink(x.value)).collect{case Some(link) => ParseResult(link)}
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

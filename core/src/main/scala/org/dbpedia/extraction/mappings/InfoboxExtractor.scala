package org.dbpedia.extraction.mappings

import collection.mutable.HashSet
import org.dbpedia.extraction.ontology.datatypes.{Datatype, DimensionDatatype}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.dataparser._
import org.dbpedia.extraction.util.RichString.wrapString
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.config.mappings.InfoboxExtractorConfig
import scala.collection.mutable.ArrayBuffer
import org.dbpedia.extraction.config.dataparser.DataParserConfig
import scala.language.reflectiveCalls

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
class InfoboxExtractor(
  context : {
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

    private val labelProperty = ontology.properties("rdfs:label")
    private val typeProperty = ontology.properties("rdf:type")
    private val propertyClass = ontology.classes("rdf:Property")

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

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
    {
        if(node.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(node.title)) return Seq.empty
        
        val quads = new ArrayBuffer[Quad]()

        /** Retrieve all templates on the page which are not ignored */
        for { template <- collectTemplates(node)
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
                    for(splitNode <- splitPropertyNodes; (value, datatype) <- extractValue(splitNode))
                    {
                        val propertyUri = getPropertyUri(property.key)
                        try
                        {
                            quads += new Quad(language, DBpediaDatasets.InfoboxProperties, subjectUri, propertyUri, value, splitNode.sourceUri, datatype)

                            if (InfoboxExtractorConfig.extractTemplateStatistics) 
                            {
                            	val stat_template = language.resourceUri.append(template.title.decodedWithNamespace)
                            	val stat_property = property.key.replace("\n", " ").replace("\t", " ").trim
                            	quads += new Quad(language, DBpediaDatasets.InfoboxTest, subjectUri, stat_template,
                                               stat_property, node.sourceUri, ontology.datatypes("xsd:string"))
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
                                quads += new Quad(language, DBpediaDatasets.InfoboxPropertyDefinitions, propertyUri, typeProperty, propertyClass.uri, splitNode.sourceUri)
                                quads += new Quad(language, DBpediaDatasets.InfoboxPropertyDefinitions, propertyUri, labelProperty, propertyLabel, splitNode.sourceUri, new Datatype("rdf:langString"))
                            }
                        }
                    }

                }
            }
        }
        
        quads
    }

    private def extractValue(node : PropertyNode) : List[(String, Datatype)] =
    {
        // TODO don't convert to SI units (what happens to {{convert|25|kg}} ?)
        extractUnitValue(node).foreach(result => return List(result))
        extractDates(node) match
        {
            case dates if !dates.isEmpty => return dates
            case _ => 
        }
        extractSingleCoordinate(node).foreach(result =>  return List(result))
        extractNumber(node).foreach(result =>  return List(result))
        extractRankNumber(node).foreach(result => return List(result))
        extractLinks(node) match
        {
            case links if !links.isEmpty => return links
            case _ =>
        }
        StringParser.parse(node).map(value => (value, new Datatype("rdf:langString"))).toList
    }

    private def extractUnitValue(node : PropertyNode) : Option[(String, Datatype)] =
    {
        val unitValues =
        for (unitValueParser <- unitValueParsers;
             (value, unit) <- unitValueParser.parse(node) )
             yield (value, unit)

        if (unitValues.size > 1)
        {
            StringParser.parse(node).map(value => (value, new Datatype("rdf:langString")))
        }
        else if (unitValues.size == 1)
        {
            val (value, unit) = unitValues.head
            Some((value.toString, unit))
        }
        else
        {
            None
        }
    }

    private def extractNumber(node : PropertyNode) : Option[(String, Datatype)] =
    {
        intParser.parse(node).foreach(value => return Some((value.toString, new Datatype("xsd:integer"))))
        doubleParser.parse(node).foreach(value => return Some((value.toString, new Datatype("xsd:double"))))
        None
    }

    private def extractRankNumber(node : PropertyNode) : Option[(String, Datatype)] =
    {
        StringParser.parse(node) match
        {
            case Some(RankRegex(number)) => Some((number, new Datatype("xsd:integer")))
            case _ => None
        }
    }
    
    private def extractSingleCoordinate(node : PropertyNode) : Option[(String, Datatype)] =
    {
        singleGeoCoordinateParser.parse(node).foreach(value => return Some((value.toDouble.toString, new Datatype("xsd:double"))))
        None
    }

    private def extractDates(node : PropertyNode) : List[(String, Datatype)] =
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
    
    private def extractDate(node : PropertyNode) : Option[(String, Datatype)] =
    {
        for (dateTimeParser <- dateTimeParsers;
             date <- dateTimeParser.parse(node))
        {
            return Some((date.toString, date.datatype))
        }
        None
    }

    private def extractLinks(node : PropertyNode) : List[(String, Datatype)] =
    {
        val splitNodes = NodeUtil.splitPropertyNode(node, """\s*\W+\s*""")

        splitNodes.flatMap(splitNode => objectParser.parse(splitNode)) match
        {
            // TODO: explain why we check links.size == splitNodes.size
            case links if links.size == splitNodes.size => return links.map(link => (link, null))
            case _ => List.empty
        }
        
        splitNodes.flatMap(splitNode => linkParser.parse(splitNode)) match
        {
            // TODO: explain why we check links.size == splitNodes.size
            case links if links.size == splitNodes.size => links.map(UriUtils.cleanLink).collect{case Some(link) => (link, null)}
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

    private def collectTemplates(node : Node) : List[TemplateNode] =
    {
        node match
        {
            case templateNode : TemplateNode => List(templateNode)
            case _ => node.children.flatMap(collectTemplates)
        }
    }

    private def collectProperties(node : Node) : List[PropertyNode] =
    {
        node match
        {
            case propertyNode : PropertyNode => List(propertyNode)
            case _ => node.children.flatMap(collectProperties)
        }
    }
}

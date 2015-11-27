package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.config.dataparser.DataParserConfig
import org.dbpedia.extraction.config.mappings.InfoboxExtractorConfig
import org.dbpedia.extraction.dataparser._
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.datatypes.{Datatype, DimensionDatatype}
import org.dbpedia.extraction.util.RichString.wrapString
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.wikiparser._

import scala.collection.mutable.{ArrayBuffer, HashSet}
import scala.language.reflectiveCalls

/**
 * This extractor extract citation data from articles
 * to boostrap this it is based on the infoboxExtractor
 */
class CitationExtractor(
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

    private val citationTemplatesRegex = List("cite.*".r, "citation.*".r) //TODO make I18n

    private val typeProperty = ontology.properties("rdf:type")
    //private val rdfLangStrDt = ontology.datatypes("rdf:langString")
    private val xsdStringDt = ontology.datatypes("xsd:string")
    private val isCitedProperty = context.language.propertyUri.append("isCitedBy")

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

    //TODO remove number parsers for now, they mess with isbn etc
    //private val intParser = new IntegerParser(context, true, validRange = (i => i%1==0))

    //private val doubleParser = new DoubleParser(context, true)

    private val dateTimeParsers = List("xsd:date", "xsd:gMonthYear", "xsd:gMonthDay", "xsd:gMonth" /*, "xsd:gYear", "xsd:gDay"*/)
                                  .map(datatype => new DateTimeParser(context, new Datatype(datatype), true))

    private val singleGeoCoordinateParser = new SingleGeoCoordinateParser(context)
                                  
    private val objectParser = new ObjectParser(context, true)

    private val linkParser = new LinkParser(true)

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // State
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private val seenProperties = HashSet[String]()
    
    override val datasets = Set(DBpediaDatasets.CitationData, DBpediaDatasets.CitationLinks /*, DBpediaDatasets.CitationTypes*/)


    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
    {
        if(node.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(node.title)) return Seq.empty
        
        val quads = new ArrayBuffer[Quad]()

        /** Retrieve all templates on the page which are not ignored */
        for { template <- InfoboxExtractor.collectTemplates(node)
          resolvedTitle = context.redirects.resolve(template.title).decoded.toLowerCase
          if citationTemplatesRegex.exists(regex => regex.unapplySeq(resolvedTitle).isDefined)
        }
        {

            for (citationIri <- getCitationIRI(template) )
            {

                quads += new Quad(language, DBpediaDatasets.CitationLinks, citationIri, isCitedProperty, subjectUri, template.sourceUri, null)

                for (property <- template.children; if (!property.key.forall(_.isDigit))) {
                    // exclude numbered properties
                    // TODO clean HTML

                    val cleanedPropertyNode = NodeUtil.removeParentheses(property)

                    val splitPropertyNodes = NodeUtil.splitPropertyNode(cleanedPropertyNode, splitPropertyNodeRegexInfobox)
                    for (splitNode <- splitPropertyNodes; (value, datatype) <- extractValue(splitNode)) {
                        val propertyUri = getPropertyUri(property.key)
                        try {
                            quads += new Quad(language, DBpediaDatasets.CitationData, citationIri, propertyUri, value, splitNode.sourceUri, datatype)


                        }
                        catch {
                            case ex: IllegalArgumentException => println(ex)
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
        //extractNumber(node).foreach(result =>  return List(result)) //TODO remove number parsing for now
        extractRankNumber(node).foreach(result => return List(result))
        extractLinks(node) match
        {
            case links if !links.isEmpty => return links
            case _ =>
        }
        StringParser.parse(node).map(value => (value, xsdStringDt)).toList
    }

    private def extractUnitValue(node : PropertyNode) : Option[(String, Datatype)] =
    {
        val unitValues =
        for (unitValueParser <- unitValueParsers;
             (value, unit) <- unitValueParser.parse(node) )
             yield (value, unit)

        if (unitValues.size > 1)
        {
            StringParser.parse(node).map(value => (value, xsdStringDt))
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

    /*
    private def extractNumber(node : PropertyNode) : Option[(String, Datatype)] =
    {
        intParser.parse(node).foreach(value => return Some((value.toString, new Datatype("xsd:integer"))))
        doubleParser.parse(node).foreach(value => return Some((value.toString, new Datatype("xsd:double"))))
        None
    }
    */

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
        //result = TrailingNumberRegex.replaceFirstIn(result, "")

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

    def getCitationIRI(templateNode: TemplateNode) : Option[String] =
    {
        //first try DOI
        for (p <- templateNode.children
          if (p.key.toLowerCase equals("doi"))
        ) {
            for (s <- StringParser.parse(p)) {
                return Some("http://doi.org/" + s.trim)
            }
        }

        // Then url / website
        for (p <- templateNode.children
             if List("url", "website").contains(p.key.toLowerCase)
        ) {
            return linkParser.parse(p).map(_.toString)
        }

        //ISBN
        for (p <- templateNode.children
             if (p.key.toLowerCase equals("isbn"))
        ) {
            for (s <- StringParser.parse(p)) {
                return Some("http://books.google.com/books?vid=ISBN" + s.trim.toUpperCase.replace("ISBN",""))
            }
        }

        //ISSN
        for (p <- templateNode.children
             if (p.key.toLowerCase equals("issn"))
        ) {
            for (s <- StringParser.parse(p)) {
                return Some("http://books.google.com/books?vid=ISSN" + s.trim.toUpperCase.replace("ISSN",""))
            }
        }

        return None
    }
}

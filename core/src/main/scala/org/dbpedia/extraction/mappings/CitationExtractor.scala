package org.dbpedia.extraction.mappings

import java.net.URI

import org.dbpedia.extraction.config.dataparser.DataParserConfig
import org.dbpedia.extraction.config.mappings.InfoboxExtractorConfig
import org.dbpedia.extraction.dataparser._
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.datatypes.{Datatype, DimensionDatatype}
import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.util.RichString.wrapString
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.wikiparser.impl.simple.SimpleWikiParser

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
extends WikiPageExtractor
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


    override def extract(page : WikiPage, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
    {
        if(page.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(page.title)) return Seq.empty

        val quads = new ArrayBuffer[Quad]()

        // the simple wiki parser is confused with the <ref> ... </ref> hetml tags and ignores whatever is inside them
        // with this hack we create a wikiPage close and remove all "<ref" from the source and then try to reparse the page
        val pageWithoutRefs = new WikiPage(title = page.title, redirect = page.redirect, id = page.id, revision = page.revision, timestamp = page.timestamp, contributorID = page.contributorID, contributorName = page.contributorName, source = page.source.replaceAll("<ref", ""), format = page.format)

        /** Retrieve all templates on the page which are not ignored */
        for { node <- new SimpleWikiParser().apply(pageWithoutRefs)
            template <- ExtractorUtils.collectTemplatesFromNodeTransitive(node)
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

    private def getCitationIRI(templateNode: TemplateNode) : Option[String] =
    {
        //first try DOI
        val doiId = getPropertyValueAsStringForKeys(templateNode, List("doi"))
        if (doiId.isDefined) {
            return Some("http://doi.org/" + doiId.get.trim.toLowerCase)
        }

        //then jstor
        val jstorId = getPropertyValueAsStringForKeys(templateNode, List("doi"))
        if (jstorId.isDefined) {
            return Some("https://www.jstor.org/stable/" + jstorId.get.trim.toLowerCase)

        }

        // PMC (PUBMED Central)
        val pmcId = getPropertyValueAsStringForKeys(templateNode, List("pmc"))
        if (pmcId.isDefined) {
            return Some("https://www.ncbi.nlm.nih.gov/pmc/articles/PMC" + pmcId.get.trim.toLowerCase)
        }

        // then PUBMED
        val pmidId = getPropertyValueAsStringForKeys(templateNode, List("pmid"))
        if (pmidId.isDefined) {
            return Some("https://www.ncbi.nlm.nih.gov/pubmed/" + pmidId.get.trim.toLowerCase)
        }

        // then arxiv
        val arxivId = getPropertyValueAsStringForKeys(templateNode, List("arxiv"))
        if (arxivId.isDefined) {
            return Some("http://arxiv.org/abs/" + arxivId.get.trim.toLowerCase)
        }

        // Then ISBN
        val isbn = getPropertyValueAsStringForKeys(templateNode, List("isbn"))
        if (isbn.isDefined) {
            return Some("http://books.google.com/books?vid=ISBN" + isbn.get.trim.toUpperCase.replace("ISBN",""))
        }

        // then ISSN
        val issn = getPropertyValueAsStringForKeys(templateNode, List("issn"))
        if (issn.isDefined) {
            return Some("https://www.worldcat.org/ISSN/" + issn.get.trim.toUpperCase.replace("ISSN",""))
        }

        // Then url / website
        val webIri = getPropertyValueAsLinkForKeys(templateNode, List("url", "website") )
        if (webIri.isDefined) {
            return webIri
        }

        None
    }

    private def getPropertyKeyIgnoreCase(templateNode: TemplateNode, propertyNames: List[String]) : Option[PropertyNode] =
    {
        templateNode
          .children
          .filter(p => propertyNames.contains(p.key.toLowerCase))
          .find(p => p.children.nonEmpty)  // don't get empty nodes
    }

    private def getPropertyValueAsString(propertyNode: PropertyNode): Option[String] =
    {
        StringParser.parse(propertyNode) match {
            case Some(stringValue) if stringValue.trim.nonEmpty => Some(stringValue)
            case _ => None
        }
    }

    private def getPropertyValueAsLink(propertyNode: PropertyNode): Option[String] =
    {
        StringParser.parse(propertyNode) match {
            case Some(stringValue) if stringValue.trim.nonEmpty => Some(stringValue)
            case _ => None
        }

        linkParser.parse(propertyNode) match {
            case Some(linkFromParser) => Some(linkFromParser.toString)
            case _ =>
                try {
                    val text = propertyNode.children.flatMap(_.toPlainText).mkString.trim
                    val iri = new URI(text)
                    if (iri.isAbsolute) Some(iri.toString)
                    else None
                } catch {
                    case _: Throwable => None
                }
        }

    }

    private def getPropertyValueAsStringForKeys(templateNode: TemplateNode, propertyNames: List[String]) : Option[String] =
    {
        getPropertyKeyIgnoreCase(templateNode, propertyNames) match {
            case Some(propertyNode) => getPropertyValueAsString(propertyNode)
            case _ => None
        }
    }

    private def getPropertyValueAsLinkForKeys(templateNode: TemplateNode, propertyNames: List[String]) : Option[String] =
    {
        getPropertyKeyIgnoreCase(templateNode, propertyNames) match {
            case Some(propertyNode) => getPropertyValueAsLink(propertyNode)
            case _ => None
        }
    }
}

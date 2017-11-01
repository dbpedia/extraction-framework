package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.ExtractionRecorder
import org.dbpedia.extraction.config.dataparser.DataParserConfig
import org.dbpedia.extraction.config.mappings.InfoboxExtractorConfig
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.dataparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.datatypes.{Datatype, DimensionDatatype}
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.RichString.wrapString
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.wikiparser.impl.simple.SimpleWikiParser
import org.dbpedia.iri.{IRI, UriUtils}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

/**
 * This extractor extract citation data from articles
 * to boostrap this it is based on the infoboxExtractor
 */
@SoftwareAgentAnnotation(classOf[CitationExtractor], AnnotationType.Extractor)
class CitationExtractor(
  context : {
    def ontology : Ontology
    def language : Language
    def redirects : Redirects
      def recorder[T: ClassTag] : ExtractionRecorder[T]
  } 
) 
extends WikiPageExtractor
{
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private val preliminaryDomainMap: Map[String, String] = Map(
        "doi" -> "http://doi.org/",
        "pmc" -> "https://www.ncbi.nlm.nih.gov/pmc/articles/PMC",
        "jstor" -> "https://www.jstor.org/stable/",
        "pmid" -> "https://www.ncbi.nlm.nih.gov/pubmed/",
        "arxiv" -> "http://arxiv.org/abs/",
        "isbn" -> "http://books.google.com/books?vid=ISBN",
        "issn" -> "https://www.worldcat.org/ISSN/",
        "oclc" -> "https://www.worldcat.org/oclc/"
    )
    private val ontology = context.ontology
    
    private val language = context.language

    private val wikiCode = language.wikiCode

    //FIXME put this in a config!
    private val citationTemplatesRegex = List("cite.*".r, "citation.*".r, "literatur.*".r, "internetquelle.*".r, "bib.*".r)

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
        DataParserConfig.splitPropertyNodeRegexInfobox(wikiCode)
    else DataParserConfig.splitPropertyNodeRegexInfobox("en")

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

    private val seenProperties = mutable.HashSet[String]()
    
    override val datasets = Set(DBpediaDatasets.CitationData, DBpediaDatasets.CitationLinks /*, DBpediaDatasets.CitationTypes*/)


    override def extract(page : WikiPage, subjectUri : String) : Seq[Quad] =
    {
        if (page.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(page.title)) return Seq.empty

        val quads = new ArrayBuffer[Quad]()

        // the simple wiki parser is confused with the <ref> ... </ref> hetml tags and ignores whatever is inside them
        // with this hack we create a wikiPage close and remove all "<ref" from the source and then try to reparse the page
        val pageWithoutRefs = new WikiPage(
            title = page.title,
            //redirect = page.redirect,
            id = page.id,
            revision = page.revision,
            timestamp = page.timestamp,
            contributorID = page.contributorID,
            contributorName = page.contributorName,
            source = page.source.replaceAll("<ref", ""),
            format = page.format
        )

        /** Retrieve all templates on the page which are not ignored */
        for {node <- SimpleWikiParser.apply(pageWithoutRefs, context.redirects)
             template <- ExtractorUtils.collectTemplatesFromNodeTransitive(node)
             resolvedTitle = context.redirects.resolve(template.title).decoded.toLowerCase
             if citationTemplatesRegex.exists(regex => regex.findFirstMatchIn(resolvedTitle).isDefined)
        } {

            val citationIri = getCitationIRI(template).toString

            quads += new Quad(language, DBpediaDatasets.CitationLinks, citationIri, isCitedProperty, subjectUri, template.sourceIri, null)

            for (property <- template.children; if !property.key.forall(_.isDigit)) {
                // exclude numbered properties
                // TODO clean HTML

                val cleanedPropertyNode = NodeUtil.removeParentheses(property)

                val splitPropertyNodes = NodeUtil.splitPropertyNode(cleanedPropertyNode, splitPropertyNodeRegexInfobox)
                for (splitNode <- splitPropertyNodes; pr <- extractValue(splitNode); if pr.unit.nonEmpty) {
                    val propertyUri = getPropertyUri(property.key)
                    try {
                        quads += new Quad(language, DBpediaDatasets.CitationData, citationIri, propertyUri, pr.value, splitNode.sourceIri, pr.unit.get)
                    }
                    catch {
                        case ex: IllegalArgumentException => println(ex)
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
        extractDates(node) match
        {
            case dates if dates.nonEmpty => return dates
            case _ => 
        }
        extractSingleCoordinate(node).foreach(result =>  return List(result))
        //extractNumber(node).foreach(result =>  return List(result)) //TODO remove number parsing for now
        extractRankNumber(node).foreach(result => return List(result))
        extractLinks(node) match
        {
            case links if links.nonEmpty => return links
            case _ =>
        }
        StringParser.parseWithProvenance(node).map(value => ParseResult(value.value, None, Some(xsdStringDt))).toList
    }

    private def extractUnitValue(node : PropertyNode) : Option[ParseResult[String]] =
    {
        val unitValues =
        for (unitValueParser <- unitValueParsers;
             pr <- unitValueParser.parseWithProvenance(node) )
             yield pr

        if (unitValues.size > 1)
        {
            StringParser.parseWithProvenance(node).map(value => ParseResult(value.value, None, Some(xsdStringDt)))
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

    /*
    private def extractNumber(node : PropertyNode) : Option[(String, Datatype)] =
    {
        intParser.parse(node).foreach(value => return Some((value.toString, new Datatype("xsd:integer"))))
        doubleParser.parse(node).foreach(value => return Some((value.toString, new Datatype("xsd:double"))))
        None
    }
    */

    private def extractRankNumber(node : PropertyNode) : Option[ParseResult[String]] =
    {
        StringParser.parseWithProvenance(node) match
        {
            case Some(ParseResult(RankRegex(number), _, _, _)) => Some(ParseResult(number, None, Some(new Datatype("xsd:integer"))))
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
            case links if links.size == splitNodes.size => return links
            case _ => List.empty
        }
        
        splitNodes.flatMap(splitNode => linkParser.parseWithProvenance(splitNode)) match
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
        //result = TrailingNumberRegex.replaceFirstIn(result, "")

        result = WikiUtil.cleanSpace(result)

        language.propertyUri.append(result)
    }

    private def createCitationIri(templateNode: TemplateNode, key: String): Option[IRI] ={
        getPropertyValueAsStringForKeys(templateNode, List(key)) match{
            case Some(x) => preliminaryDomainMap.get(key) match{
                case Some(domain) => UriUtils.createURI(domain + x.trim.toLowerCase).toOption
                case None => UriUtils.createURI(x.trim.toLowerCase).toOption
            }
            case None => None
        }
    }

    private def getCitationIRI(templateNode: TemplateNode) : IRI =
    {
        //first try DOI
        createCitationIri(templateNode, "doi").map(return _)

        //then jstor
        createCitationIri(templateNode, "jstor").map(return _)

        // PMC (PUBMED Central)
        createCitationIri(templateNode, "pmc").map(return _)

        // then PUBMED
        createCitationIri(templateNode, "pmid").map(return _)

        // then arxiv
        createCitationIri(templateNode, "arxiv").map(return _)

        // Then ISBN
        createCitationIri(templateNode, "isbn").map(return _)

        // then ISSN
        createCitationIri(templateNode, "issn").map(return _)

        // then oclc
        createCitationIri(templateNode, "oclc").map(return _)

        // Then url / website
        createCitationIri(templateNode, "url").map(return _)

        // Then url / website
        createCitationIri(templateNode, "website").map(return _)

        //create a hash based IRI
        createHashedCitationIRIFromTemplateNode(templateNode).get
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
        StringParser.parseWithProvenance(propertyNode) match {
            case Some(pr) if pr.value.trim.nonEmpty => Some(pr.value)
            case _ => None
        }
    }

    private def getPropertyValueAsLink(propertyNode: PropertyNode): Option[IRI] =
    {
        StringParser.parseWithProvenance(propertyNode) match {
            case Some(pr) if pr.value.trim.nonEmpty => Some(pr.value)
            case _ => None
        }

        linkParser.parseWithProvenance(propertyNode) match {
            case Some(pr) => Some(pr.value)
            case _ => UriUtils.createURI(propertyNode.children.flatMap(_.toPlainText).mkString.trim) match{
                case Success(iri) => if (iri.isAbsolute)
                        Some(iri)
                    else None
                case Failure(f) => None
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

    private def getPropertyValueAsLinkForKeys(templateNode: TemplateNode, propertyNames: List[String]) : Option[IRI] =
    {
        getPropertyKeyIgnoreCase(templateNode, propertyNames) match {
            case Some(propertyNode) => getPropertyValueAsLink(propertyNode)
            case _ => None
        }
    }

    private def createHashedCitationIRIFromTemplateNode(templateNode: TemplateNode) : Option[IRI] =
    {
        val template = new StringBuilder

        template append templateNode.title.decoded append  "{"

        templateNode.children
            .filter(_.children.nonEmpty)
            .sortWith(_.key > _.key)
            .foreach( p => {
                template append '|' append  p.key append '=' append p.children.flatMap(_.toPlainText).mkString.trim
            })

        template append "}"

        //MD5 from https://stevenwilliamalexander.wordpress.com/2012/06/11/scala-md5-hash-function-for-scala-console/
        val shaHash = java.security.MessageDigest.getInstance("SHA-256").
          digest(template.toString().getBytes())
          .map(0xFF & _).map {
            "%02x".format(_)
            }.foldLeft("") {
              _ + _
            }
        UriUtils.createURI("http://citation.dbpedia.org/hash/" + shaHash).toOption
    }
}

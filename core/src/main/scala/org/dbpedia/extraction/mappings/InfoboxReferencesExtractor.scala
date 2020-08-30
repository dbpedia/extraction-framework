package org.dbpedia.extraction.mappings

import java.util

import com.sun.xml.internal.fastinfoset.util.StringArray
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
 * This extractor extracts all properties from all infoboxes.
 * Extracted information is represented using properties in the http://xx.dbpedia.org/property/
 * namespace (where xx is the language code).
 * The names of the these properties directly reflect the name of the Wikipedia infobox property.
 * Property names are not cleaned or merged.
 * Property types are not part of a subsumption hierarchy and there is no consistent ontology for the infobox dataset.
 * The infobox extractor performs only a minimal amount of property value clean-up, e.g., by converting a value like “June 2009” to the XML Schema format “2009–06”.
 * You should therefore use the infobox dataset only if your application requires complete coverage of all Wikipeda properties and you are prepared to accept relatively noisy data.
 */
class InfoboxReferencesExtractor(
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
    
    override val datasets = Set(DBpediaDatasets.InfoboxTest)

    override def extract(node : PageNode, subjectUri : String) : Seq[Quad] =
    {

        if(node.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(node.title)) return Seq.empty

        val quads = new ArrayBuffer[Quad]()
        val node2 = new PageNode(node.title, node.id, node.revision, node.timestamp, node.contributorID, node.contributorName, node.source.replace("<ref","Temp_value_before_ref<ref").replaceAll("""\{\{[Rr]\|([^\}]+)\}\}""", "Temp_value_before_ref$1"), node.children)

        val regCom = "<\\!--.*?-->".r
        val current2 = regCom.replaceAllIn(node.source.toString,"")
        val reg = "<ref([^>]*?)>([^<]+)</ref>".r("refn","refv")
        val najti = reg.findAllMatchIn(current2).toArray
        val regz = "<ref([^>/]+)/[ ]?>".r("refz")
        val regz2 = """\{\{[Rr]\\\|([^\}]+)\}\}""".r("refc")
        var refNames = scala.collection.mutable.Map[String, String]()
        for (ref <- najti) {
            val refB = ref.group("refn")
            val refValue = ref.group("refv")
            if (refB contains "=") {
                var refName = ""
                if (refB.replaceAll(" ","") contains "name=") {
                    val regX = """name[ ]?=[ '\\"]?([^>'\\"]+)[ '\\"]?>""".r("ref1")
                    val refNameCand = regX.findFirstMatchIn(refB+">")
                    if (refNameCand.isDefined) {
                        refName=refNameCand.get.group("ref1")
                    }
                }
                if (refName!="") {refNames(refName) = refValue}
            }
        }

        var counter = 0

        var templates1lvl = scala.collection.mutable.Map[String, String]()
        var tempContent = new String
        for (c <- current2.toArray) {
            if (c.equals('{')) {
                counter += 1
            }
            if (c.equals('}')) {
                counter -= 1
            }
            if (counter>0) {
                tempContent+=c
            }
            if (counter==0) {
                if (tempContent.length>1) {
                    val checkName = tempContent.split("\\|")
                    //Checking if there is at least one "|" char => adding template to Map
                    if (checkName.length>0){
                        //Checking if there any reference based on "<ref" tag, TODO - add other options: {{R+|.... {{Harv...
                        if (tempContent contains "<ref") {
                            val templateName = checkName(0).trim.replace("{{","")
                            val toAdd = tempContent.slice(1,tempContent.length-1).split("\\|")
                            templates1lvl(templateName) = toAdd.slice(1,toAdd.length).mkString("\\|")
                        }

                    }
                    tempContent=""
                }
            }
        }
        var infoboxesRaw = scala.collection.mutable.Map[String, scala.collection.mutable.Map[String,String]]()
        for ((tempName,tempContent) <- templates1lvl) {

            var counter = 0 // for brackets { }
            var counter2 = 0 // for brackets [ ]
            var tempValue = ""
            var paramName = ""
            var ParametersRaw = scala.collection.mutable.Map[String, String]()
            val tempContentLen = tempContent.length
            var cntChar = 0
            for (c <- tempContent.toArray) {
                cntChar+=1
                tempValue += c
                if (c.equals('{')) {
                    counter += 1
                }
                if (c.equals('}')) {
                    counter -= 1
                }
                if (c.equals(']')) {
                    counter2 += 1
                }
                if (c.equals('[')) {
                    counter2 -= 1
                }

                if ((c.equals('|') && counter==0 && counter2==0) || cntChar==tempContentLen) {
                    if (tempValue contains "=") {
                        var temp1 = tempValue.split("=")
                        var paramName = temp1(0).trim
                        var paramValue = temp1.slice(1,temp1.length).mkString("=").trim
                        // TODO: to add apart "<ref", {{r| other cases
                        if ((paramValue contains "<ref") || (paramValue contains "{{r\\|") || (paramValue contains "{{R\\|")) {
                            if ((paramValue.length>0) && !(paramName contains "|") && !(paramName contains "{") && !(paramName contains "}")) {
                                ParametersRaw(paramName) = paramValue

                            }
                        }
                        tempValue=""
                    }

                }

            }
            infoboxesRaw(tempName) = ParametersRaw
        }

        /** Retrieve all templates on the page which are not ignored */
        for { template <- InfoboxReferencesExtractor.collectTemplates(node2)
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
                for(property <- propertyList) {


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
                            if (infoboxesRaw contains template.title.decoded) {
                                if (infoboxesRaw(template.title.decoded) contains property.key) {

                                    val uaU = new Datatype("rdf:langString")
                                    var property2 = new ParseResult(pr.value,None,Option(uaU))

                                    val rawParameter = infoboxesRaw(template.title.decoded)(property.key)

                                    val refWithContent = reg.findAllMatchIn(rawParameter).toArray
                                    for (ref <- refWithContent) {
                                        //val refB = ref.group("refn")
                                        val refValue = ref.group("refv")
                                        quads += new Quad(language, DBpediaDatasets.InfoboxTest, subjectUri, propertyUri, refValue, splitNode.sourceIri, rdfLangStrDt)
                                    }


                                    val refWithOnlyName = regz.findAllMatchIn(rawParameter).toArray
                                    if (refWithOnlyName.length > 0) {
                                        for (refi <- refWithOnlyName) {
                                            val nameRe = """name[ '\\"]?=[ ]?[ '\\"]?([^>'\\"]+)""".r("nm").findFirstMatchIn(refi.group("refz")).get.group("nm")
                                            if (refNames.contains(nameRe)){
                                                quads += new Quad(language, DBpediaDatasets.InfoboxTest, subjectUri, propertyUri, refNames(nameRe), splitNode.sourceIri, rdfLangStrDt)
                                            }

                                        }
                                    }


                                    val refWithOnlyName2 = regz2.findFirstMatchIn(rawParameter).toArray
                                    if (refWithOnlyName2.length > 0) {
                                        for (refi <- refWithOnlyName2) {
                                            val refK = refi.group("refc").split("\\|")
                                            for (refiz <- refK){ // why if name contains \?
                                                if (refNames.contains(refiz.replace("\\",""))){
                                                    quads += new Quad(language, DBpediaDatasets.InfoboxTest, subjectUri, propertyUri, refNames(refiz.replace("\\","")), splitNode.sourceIri, rdfLangStrDt)
                                                }

                                            }

                                        }
                                    }


                                }
                            }



                        }
                        catch
                        {
                            case ex : IllegalArgumentException => println(ex)
                        }
                        propertiesFound = true

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
        extractNumber(node).foreach(result =>  return List(result))
        extractRankNumber(node).foreach(result => return List(result))
        extractLinks(node) match
        {
            case links if links.nonEmpty => { return links}
            case _ =>
        }
        StringParser.parse(node).map(value => ParseResult(value.value, None, Some(rdfLangStrDt))).toList
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

object InfoboxReferencesExtractor {

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

package org.dbpedia.extraction.mappings

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
import scala.util.{Failure, Success}

/**
 * This extractor extract citation data from articles
 * to boostrap this it is based on the infoboxExtractor
 */
class InfoboxReferencesExtractor2(
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


    private val typeProperty = ontology.properties("rdf:type")
    private val rdfLangStrDt = ontology.datatypes("rdf:langString")
    private val xsdStringDt = ontology.datatypes("xsd:string")
    private val isCitedProperty = context.language.propertyUri.append("isCitedBy")

    private val dbpediaOntologyPrefix = "http://dbpedia.org/ontology/"

    private val rdfType = context.ontology.properties("rdf:type")

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

    override val datasets = Set(DBpediaDatasets.InfoboxTest)


    override def extract(page : WikiPage, subjectUri : String) : Seq[Quad] =
    {
        if (page.title.namespace != Namespace.Main && !ExtractorUtils.titleContainsCommonsMetadata(page.title)) return Seq.empty

        val quads = new ArrayBuffer[Quad]()

        val pageModified = new WikiPage(
            title = page.title,
            redirect = page.redirect,
            id = page.id,
            revision = page.revision,
            timestamp = page.timestamp,
            contributorID = page.contributorID,
            contributorName = page.contributorName,
            source = page.source.replaceAll("""<\!--.*?-->""", ""),
            format = page.format
        )

        /** Retrieve all templates on the page which are not ignored */
        for {node <- SimpleWikiParser.apply(pageModified)
             template <- ExtractorUtils.collectTemplatesFromNodeTransitive(node)
             } {
            val reg = "<ref([^>]*?)>([^<]+)</ref>".r("refn","refv")
            val najti = reg.findAllMatchIn(node.source).toArray
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
            for (c <- node.source.toArray) {
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

            for ((tempName,tempContent) <- templates1lvl) {
                //println(tempName)
                var counter = 0 // for brackets { }
                var counter2 = 0 // for brackets [ ]
                var tempValue = ""

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
                            if (!(paramName contains "*") && !(paramName contains "<") && (paramName.length>0)) {
                                var paramValue = temp1.slice(1,temp1.length).mkString("=").trim
                                // TODO: to add apart "<ref", {{r| other cases
                                if ((paramValue contains "<ref") || (paramValue contains "{{r\\|") || (paramValue contains "{{R\\|")) {
                                    if ((paramValue.length>0) && !(paramName contains "|") && !(paramName contains "{") && !(paramName contains "}")) {

                                        val propertyUri = getPropertyUri(paramName)
                                        val refWithContent = reg.findAllMatchIn(paramValue).toArray
                                        for (ref <- refWithContent) {
                                            //val refB = ref.group("refn")
                                            val refValue = ref.group("refv")
                                            try {
                                                quads += new Quad(language, DBpediaDatasets.InfoboxTest, subjectUri, propertyUri, refValue, template.sourceIri, rdfLangStrDt)
                                            }
                                            catch {
                                                case ex: IllegalArgumentException => println(ex)
                                            }

                                        }


                                        val refWithOnlyName = regz.findAllMatchIn(paramValue).toArray
                                        if (refWithOnlyName.length > 0) {
                                            for (refi <- refWithOnlyName) {
                                                val nameRe = """name[ '\\"]?=[ ]?[ '\\"]?([^>'\\"]+)""".r("nm").findFirstMatchIn(refi.group("refz")).get.group("nm")
                                                if (refNames.contains(nameRe)){
                                                    try {
                                                        quads += new Quad(language, DBpediaDatasets.InfoboxTest, subjectUri, propertyUri, refNames(nameRe), template.sourceIri, rdfLangStrDt)
                                                    }
                                                    catch {
                                                        case ex: IllegalArgumentException => println(ex)
                                                    }


                                                }

                                            }
                                        }


                                        val refWithOnlyName2 = regz2.findFirstMatchIn(paramValue).toArray
                                        if (refWithOnlyName2.length > 0) {
                                            for (refi <- refWithOnlyName2) {
                                                val refK = refi.group("refc").split("\\|")
                                                for (refiz <- refK){ // why if name contains \?
                                                    if (refNames.contains(refiz.replace("\\",""))){
                                                        try {
                                                            quads += new Quad(language, DBpediaDatasets.InfoboxTest, subjectUri, propertyUri, refNames(refiz.replace("\\","")), template.sourceIri, rdfLangStrDt)
                                                        }
                                                        catch {
                                                            case ex: IllegalArgumentException => println(ex)
                                                        }
                                                    }

                                                }

                                            }
                                        }

                                        //println(paramValue)
                                    }
                                }
                                tempValue=""
                            }

                        }

                    }

                }

            }

        }
        quads
    }


    private def getPropertyUri(key : String) : String =
    {
        var result = key.toLowerCase(language.locale).trim
        result = result.toCamelCase(SplitWordsRegex, language.locale)

        result = WikiUtil.cleanSpace(result)

        language.propertyUri.append(result)
    }


}

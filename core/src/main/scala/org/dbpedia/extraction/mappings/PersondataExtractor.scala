package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.dataparser.{ObjectParser, DateTimeParser, StringParser}
import org.dbpedia.extraction.config.mappings.PersondataExtractorConfig
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import scala.collection.mutable.ArrayBuffer

/**
 * Extracts information about persons (date and place of birth etc.) from the English and German Wikipedia, represented using the FOAF vocabulary.
 */
class PersondataExtractor( context : {
                               def ontology : Ontology
                               def redirects : Redirects  // redirects required by DateTimeParser
                               def language : Language } ) extends Extractor
{
    private val language = context.language.wikiCode

    require(PersondataExtractorConfig.supportedLanguages.contains(language))

    private val persondataTemplate = PersondataExtractorConfig.persondataTemplates(language)
    private val name = PersondataExtractorConfig.name(language)
    private val alternativeNames = PersondataExtractorConfig.alternativeNames(language)
    private val description = PersondataExtractorConfig.description(language)
    private val birthDate = PersondataExtractorConfig.birthDate(language)
    private val birthPlace = PersondataExtractorConfig.birthPlace(language)
    private val deathDate = PersondataExtractorConfig.deathDate(language)
    private val deathPlace = PersondataExtractorConfig.deathPlace(language)

    private val dateParser = new DateTimeParser(context, new Datatype("xsd:date"))
    private val monthYearParser = new DateTimeParser(context, new Datatype("xsd:gMonthYear"))
    private val monthDayParser = new DateTimeParser(context, new Datatype("xsd:gMonthDay"))

    private val birthDateProperty = context.ontology.properties("birthDate")
    private val birthPlaceProperty = context.ontology.properties("birthPlace")
    private val deathDateProperty = context.ontology.properties("deathDate")
    private val deathPlaceProperty = context.ontology.properties("deathPlace")

    private val rdfTypeProperty = context.ontology.properties("rdf:type")
    private val foafNameProperty = context.ontology.properties("foaf:name")
    private val foafSurNameProperty = context.ontology.properties("foaf:surname")
    private val foafGivenNameProperty = context.ontology.properties("foaf:givenName")
    private val foafPersonClass = context.ontology.classes("foaf:Person")
    private val dcDescriptionProperty = context.ontology.properties("dc:description")

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
    {
        if(node.title.namespace != Namespace.Main) return Seq.empty

        val objectParser = new ObjectParser(context)

        var quads = new ArrayBuffer[Quad]()

        val list = collectTemplates(node).filter(template =>
            persondataTemplate.contains(template.title.decoded.toLowerCase))

        list.foreach(template => {
            var nameFound = false
            val propertyList = template.children
            for(property <- propertyList)
            {
                property.key match
                {
                    case key if key == name =>
                    {
                        for(nameValue <- StringParser.parse(property))
                        {
                            val nameParts = nameValue.split(",")
                            if (nameParts.size == 2)
                            {
                                val reversedName = nameParts(1).trim + " " + nameParts(0).trim
                                quads += new Quad(context.language, DBpediaDatasets.Persondata, subjectUri, foafNameProperty, reversedName, property.sourceUri, new Datatype("xsd:string"))
                                quads += new Quad(context.language, DBpediaDatasets.Persondata, subjectUri, foafSurNameProperty, nameParts(0).trim, property.sourceUri, new Datatype("xsd:string"))
                                quads += new Quad(context.language, DBpediaDatasets.Persondata, subjectUri, foafGivenNameProperty, nameParts(1).trim, property.sourceUri, new Datatype("xsd:string"))
                            }
                            else
                            {
                                quads += new Quad(context.language, DBpediaDatasets.Persondata, subjectUri, foafNameProperty, nameValue.trim, property.sourceUri, new Datatype("xsd:string"))
                            }
                            quads += new Quad(context.language, DBpediaDatasets.Persondata, subjectUri, rdfTypeProperty, foafPersonClass.uri, template.sourceUri)
                            nameFound = true
                        }
                    }
                    case _ =>
                }
            }
            if (nameFound)
            {
                for(property <- propertyList)
                {
                    property.key match
                    {
                        case key if key == alternativeNames =>
                        {
                            for(value <- StringParser.parse(property))
                            {
                            }
                        }
                        case key if key == description =>
                        {
                            for(value <- StringParser.parse(property))
                            {
                                quads += new Quad(context.language, DBpediaDatasets.Persondata, subjectUri, dcDescriptionProperty, value, property.sourceUri, new Datatype("xsd:string"))
                            }
                        }
                        case key if key == birthDate =>
                        {
                            for ((date, datatype) <- getDate(property))
                            {
                                quads += new Quad(context.language, DBpediaDatasets.Persondata, subjectUri, birthDateProperty, date, property.sourceUri, datatype)
                            }
                        }
                        case key if key == deathDate =>
                        {
                            for ((date, datatype) <- getDate(property))
                            {
                                quads += new Quad(context.language, DBpediaDatasets.Persondata, subjectUri, deathDateProperty, date, property.sourceUri, datatype)
                            }
                        }
                        case key if key == birthPlace =>
                        {
                            for(objUri <- objectParser.parsePropertyNode(property, split=true))
                            {
                                quads += new Quad(context.language, DBpediaDatasets.Persondata, subjectUri, birthPlaceProperty, objUri.toString, property.sourceUri)
                            }
                        }
                        case key if key == deathPlace =>
                        {
                            for(objUri <- objectParser.parsePropertyNode(property, split=true))
                            {
                                quads += new Quad(context.language, DBpediaDatasets.Persondata, subjectUri, deathPlaceProperty, objUri.toString, property.sourceUri)
                            }
                        }
                        case _ =>
                    }
                }
            }
        })
        
        quads
    }

    private def getDate(node: Node) : Option[(String, Datatype)] =
    {
        for (date <- dateParser.parse(node))
        {
            return Some((date.toString, date.datatype))
        }
        for (date <- monthYearParser.parse(node))
        {
            return Some((date.toString, date.datatype))
        }
        for (date <- monthDayParser.parse(node))
        {
            return Some((date.toString, date.datatype))
        }
        None
    }

    private def collectTemplates(node : Node) : List[TemplateNode] =
    {
        node match
        {
            case templateNode : TemplateNode => List(templateNode)
            case _ => node.children.flatMap(collectTemplates)
        }
    }
}
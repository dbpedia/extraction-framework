package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Graph, Quad}
import org.dbpedia.extraction.wikiparser.{PageNode, WikiTitle, TemplateNode, Node}
import org.dbpedia.extraction.dataparser.{ObjectParser, DateTimeParser, StringParser}

/**
 * Extracts information about persons (date and place of birth etc.) from the English and German Wikipedia, represented using the FOAF vocabulary.
 */
class PersondataExtractor(extractionContext : ExtractionContext) extends Extractor
{
    private val language = extractionContext.language.wikiCode

    require(Set("en", "de").contains(language))

    private val persondataTemplates = Map("en" -> "persondata", "de" -> "personendaten")

    private val name = Map ("en" -> "NAME", "de" -> "NAME")
    private val alternativeNames = Map ("en" -> "ALTERNATIVE NAMES", "de" -> "ALTERNATIVNAMEN")
    private val description = Map ("en" -> "SHORT DESCRIPTION", "de" -> "KURZBESCHREIBUNG")
    private val birthDate = Map ("en" -> "DATE OF BIRTH", "de" -> "GEBURTSDATUM")
    private val birthPlace = Map ("en" -> "PLACE OF BIRTH", "de" -> "GEBURTSORT")
    private val deathDate = Map ("en" -> "DATE OF DEATH", "de" -> "STERBEDATUM")
    private val deathPlace = Map ("en" -> "PLACE OF DEATH", "de" -> "STERBEORT")

    private val dateParser = new DateTimeParser(extractionContext, new Datatype("xsd:date"))
    private val monthYearParser = new DateTimeParser(extractionContext, new Datatype("xsd:gMonthYear"))
    private val monthDayParser = new DateTimeParser(extractionContext, new Datatype("xsd:gMonthDay"))

    private val birthDateProperty = extractionContext.ontology.getProperty("birthDate").get
    private val birthPlaceProperty = extractionContext.ontology.getProperty("birthPlace").get
    private val deathDateProperty = extractionContext.ontology.getProperty("deathDate").get
    private val deathPlaceProperty = extractionContext.ontology.getProperty("deathPlace").get

    private val rdfTypeProperty = extractionContext.ontology.getProperty("rdf:type").get
    private val foafNameProperty = extractionContext.ontology.getProperty("foaf:name").get
    private val foafSurNameProperty = extractionContext.ontology.getProperty("foaf:surname").get
    private val foafGivenNameProperty = extractionContext.ontology.getProperty("foaf:givenName").get
    private val foafPersonClass = extractionContext.ontology.getClass("foaf:Person").get
    private val dcDescriptionProperty = extractionContext.ontology.getProperty("dc:description").get

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != WikiTitle.Namespace.Main) return new Graph()

        val objectParser = new ObjectParser(extractionContext)

        var quads = List[Quad]()

        val list = collectTemplates(node).filter(template =>
            persondataTemplates(language).contains(template.title.decoded.toLowerCase))

        list.foreach(template => {
            var nameFound = false
            val propertyList = template.children
            for(property <- propertyList)
            {
                property.key match
                {
                    case key if key == name(language) =>
                    {
                        for(nameValue <- StringParser.parse(property))
                        {
                            val nameParts = nameValue.split(",")
                            if (nameParts.size == 2)
                            {
                                val reversedName = nameParts(1).trim + " " + nameParts(0).trim
                                quads ::= new Quad(extractionContext, DBpediaDatasets.Persondata, subjectUri, foafNameProperty, reversedName, property.sourceUri, new Datatype("xsd:string"))
                                quads ::= new Quad(extractionContext, DBpediaDatasets.Persondata, subjectUri, foafSurNameProperty, nameParts(0).trim, property.sourceUri, new Datatype("xsd:string"))
                                quads ::= new Quad(extractionContext, DBpediaDatasets.Persondata, subjectUri, foafGivenNameProperty, nameParts(1).trim, property.sourceUri, new Datatype("xsd:string"))
                            }
                            else
                            {
                                quads ::= new Quad(extractionContext, DBpediaDatasets.Persondata, subjectUri, foafNameProperty, nameValue.trim, property.sourceUri, new Datatype("xsd:string"))
                            }
                            quads ::= new Quad(extractionContext, DBpediaDatasets.Persondata, subjectUri, rdfTypeProperty, foafPersonClass.uri, template.sourceUri)
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
                        case key if key == alternativeNames(language) =>
                        {
                            for(value <- StringParser.parse(property))
                            {
                            }
                        }
                        case key if key == description(language) =>
                        {
                            for(value <- StringParser.parse(property))
                            {
                                quads ::= new Quad(extractionContext, DBpediaDatasets.Persondata, subjectUri, dcDescriptionProperty, value, property.sourceUri, new Datatype("xsd:string"))
                            }
                        }
                        case key if key == birthDate(language) =>
                        {
                            for ((date, datatype) <- getDate(property))
                            {
                                quads ::= new Quad(extractionContext, DBpediaDatasets.Persondata, subjectUri, birthDateProperty, date, property.sourceUri, datatype)
                            }
                        }
                        case key if key == deathDate(language) =>
                        {
                            for ((date, datatype) <- getDate(property))
                            {
                                quads ::= new Quad(extractionContext, DBpediaDatasets.Persondata, subjectUri, deathDateProperty, date, property.sourceUri, datatype)
                            }
                        }
                        case key if key == birthPlace(language) =>
                        {
                            for(objUri <- objectParser.parsePropertyNode(property, split=true))
                            {
                                quads ::= new Quad(extractionContext, DBpediaDatasets.Persondata, subjectUri, birthPlaceProperty, objUri.toString, property.sourceUri)
                            }
                        }
                        case key if key == deathPlace(language) =>
                        {
                            for(objUri <- objectParser.parsePropertyNode(property, split=true))
                            {
                                quads ::= new Quad(extractionContext, DBpediaDatasets.Persondata, subjectUri, deathPlaceProperty, objUri.toString, property.sourceUri)
                            }
                        }
                        case _ =>
                    }
                }
            }
        })
        new Graph(quads)
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
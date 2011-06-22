package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets, Graph, Quad}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.config.mappings.PndExtractorConfig

/**
 * Extracts PND (Personennamendatei) data about a person.
 * PND is published by the German National Library.
 * For each person there is a record with his name, birth and occupation connected with a unique identifier, the PND number.
 */
class PndExtractor(extractionContext : ExtractionContext) extends Extractor
{
    private val language = extractionContext.language.wikiCode

    require(PndExtractorConfig.supportedLanguages.contains(language))

    private val individualisedPndProperty = extractionContext.ontology.getProperty("individualisedPnd")
                                            .getOrElse(throw new NoSuchElementException("Ontology property 'individualisedPnd' does not exist in DBpedia Ontology."))

    private val PndRegex = """(?i)[0-9X]+"""

    override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
    {
        if(node.title.namespace != WikiTitle.Namespace.Main) return new Graph()
        
        var quads = List[Quad]()

        val list = collectTemplates(node).filter(template =>
            PndExtractorConfig.pndTemplates.contains(template.title.decoded.toLowerCase))

        list.foreach(template => {
            template.title.decoded.toLowerCase match
            {
                case "normdaten" =>
                {
                    val propertyList = template.children.filter(property => property.key.toLowerCase == "pnd")
                    for(property <- propertyList)
                    {
                        for (pnd <- getPnd(property)) 
                        {
                            quads ::= new Quad(extractionContext.language, DBpediaDatasets.Pnd, subjectUri, individualisedPndProperty, pnd, property.sourceUri, new Datatype("xsd:string"))
                        }
                    }
                }
                case _ =>
                {
                    val propertyList = template.children.filter(property => property.key == "1")
                    for(property <- propertyList)
                    {
                        for (pnd <- getPnd(property))
                        {
                            quads ::= new Quad(extractionContext.language, DBpediaDatasets.Pnd, subjectUri, individualisedPndProperty, pnd, property.sourceUri, new Datatype("xsd:string"))
                        }
                    }
                }
            }
        })
        new Graph(quads)
    }

    private def getPnd(node : PropertyNode) : Option[String] =
    {
        node.children match
        {
            case TextNode(text, _) :: Nil if (text.trim.matches(PndRegex)) => return Some(text.trim)
            case _ => return None
        }
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
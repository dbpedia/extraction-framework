package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.config.mappings.PndExtractorConfig
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

/**
 * Extracts PND (Personennamendatei) data about a person. PND is published by the 
 * German National Library. For each person there is a record with name, birth and 
 * occupation connected with a unique identifier, the PND number.
 * TODO: also use http://en.wikipedia.org/wiki/Template:Authority_control and other templates.
 */
@SoftwareAgentAnnotation(classOf[PndExtractor], AnnotationType.Extractor)
class PndExtractor (
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends PageNodeExtractor
{
  private val language = context.language
  
  private val wikiCode = language.wikiCode

  require(PndExtractorConfig.supportedLanguages.contains(wikiCode))

  private val individualisedPndProperty = context.ontology.properties("individualisedPnd")

  private val PndRegex = """(?i)[0-9X]+"""

  override val datasets = Set(DBpediaDatasets.Pnd)

  override def extract(node : PageNode, subjectUri : String) : Seq[Quad] =
  {
    if (node.title.namespace != Namespace.Main) return Seq.empty
    
    var quads = new ArrayBuffer[Quad]()

    val list = collectTemplates(node).filter(template =>
      PndExtractorConfig.pndTemplates.contains(template.title.decoded.toLowerCase))

    list.foreach(template => {
      template.title.decoded.toLowerCase match
      {
        // FIXME: copy-and-paste programming...
        case "normdaten" =>
        {
          val propertyList = template.children.filter(property => property.key.toLowerCase == "pnd")
          for(property <- propertyList)
          {
            for (pnd <- getPnd(property)) 
            {
                quads += new Quad(context.language, DBpediaDatasets.Pnd, subjectUri, individualisedPndProperty, pnd, property.sourceIri, new Datatype("xsd:string"))
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
                quads += new Quad(context.language, DBpediaDatasets.Pnd, subjectUri, individualisedPndProperty, pnd, property.sourceIri, new Datatype("xsd:string"))
            }
          }
        }
      }
    })
    
    quads
  }

  private def getPnd(node : PropertyNode) : Option[String] =
  {
    node.children match
    {
      case TextNode(text, _, _) :: Nil if (text.trim.matches(PndRegex)) => Some(text.trim)
      case _ => None
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
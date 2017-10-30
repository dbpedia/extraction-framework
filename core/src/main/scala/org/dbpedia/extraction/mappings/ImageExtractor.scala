package org.dbpedia.extraction.mappings

import java.net.URLDecoder
import java.util.logging.Logger

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.mappings.ImageExtractorConfig
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.RichString.wrapString
import org.dbpedia.extraction.util.{ExtractorUtils, Language, WikiUtil}
import org.dbpedia.extraction.wikiparser._

import scala.collection.mutable.{ArrayBuffer, Set => MutableSet}
import scala.language.reflectiveCalls

/**
 * Extracts the first image of a Wikipedia page. Constructs a thumbnail from it, and
 * the full size image.
 * 
 * FIXME: we're sometimes dealing with encoded links, sometimes with decoded links. It's quite a mess.
 */
@deprecated("replaced by ImageExtractorNew", "2017-08")
@SoftwareAgentAnnotation(classOf[ImageExtractor], AnnotationType.Extractor)
class ImageExtractor( 
  context: {
    def ontology: Ontology
    def language: Language
    def articlesSource: Source
    def commonsSource: Source

    def freeImages: Seq[String]
    def nonFreeImages: Seq[String]
  } 
) 
extends PageNodeExtractor
{
  private val wikiCode = context.language.wikiCode
  private val language = context.language

  require(ImageExtractorConfig.supportedLanguages.contains(wikiCode), "ImageExtractor's supported languages: "+ImageExtractorConfig.supportedLanguages.mkString(", ")+"; not "+wikiCode)

  private val fileNamespaceIdentifier = Namespace.File.name(language)

  private val logger = Logger.getLogger(classOf[MappingExtractor].getName)

  private val encodedLinkRegex = """%[0-9a-fA-F][0-9a-fA-F]""".r

  private val imageClass = context.ontology.classes("Image")
  private val dbpediaThumbnailProperty = context.ontology.properties("thumbnail")
  private val foafDepictionProperty = context.ontology.properties("foaf:depiction")
  private val foafThumbnailProperty = context.ontology.properties("foaf:thumbnail")
  private val dcRightsProperty = context.ontology.properties("dc:rights")
  private val rdfType = context.ontology.properties("rdf:type")

  private val commonsLang = Language.Commons

  override val datasets = Set(DBpediaDatasets.Images)

    override def extract(node: PageNode, subjectUri: String): Seq[Quad] =
    {
        if(node.title.namespace != Namespace.Main) return Seq.empty
 
        var quads = new ArrayBuffer[Quad]()

        for ((imageFileName, sourceNode) <- searchImage(node.children, 0) if !imageFileName.toLowerCase.startsWith("replace_this_image"))
        {
            val lang = if(context.freeImages.contains(URLDecoder.decode(imageFileName, "UTF-8")))
                language else commonsLang
            val url = ExtractorUtils.getFileURL(imageFileName, lang)
            val thumbnailUrl = ExtractorUtils.getThumbnailURL(imageFileName, lang)

            quads += new Quad(language, DBpediaDatasets.Images, subjectUri, foafDepictionProperty, url, sourceNode.sourceIri)
            quads += new Quad(language, DBpediaDatasets.Images, subjectUri, dbpediaThumbnailProperty, thumbnailUrl, sourceNode.sourceIri)
            quads += new Quad(language, DBpediaDatasets.Images, url, foafThumbnailProperty, thumbnailUrl, sourceNode.sourceIri)
            quads += new Quad(language, DBpediaDatasets.Images, url, rdfType, imageClass.uri, sourceNode.sourceIri)
            quads += new Quad(language, DBpediaDatasets.Images, thumbnailUrl, rdfType, imageClass.uri, sourceNode.sourceIri)

            val wikipediaImageUrl = language.baseUri+"/wiki/"+fileNamespaceIdentifier+":"+imageFileName

            quads += new Quad(language, DBpediaDatasets.Images, url, dcRightsProperty, wikipediaImageUrl, sourceNode.sourceIri)
            quads += new Quad(language, DBpediaDatasets.Images, thumbnailUrl, dcRightsProperty, wikipediaImageUrl, sourceNode.sourceIri)
        }

        quads
    }

    private def searchImage(nodes: List[Node], sections: Int): Option[(String, Node)] = {
      var currentSections = sections
      for (node <- nodes) {
        node match {
          case SectionNode(_, _, _, _) => {
            if (currentSections > 1) return None
            currentSections += 1
          }
          case TemplateNode(_, children, _, _) => {
            for (property <- children;
                 textNode@TextNode(text, _, _) <- property.children;
                 fileName <- ImageExtractorConfig.ImageRegex.findFirstIn(text);
                 encodedFileName = if (encodedLinkRegex.findFirstIn(fileName) == None)
                   WikiUtil.wikiEncode(fileName).capitalize(language.locale)
                 else
                   fileName
                 if checkImageRights(encodedFileName)) {
                  return Some((encodedFileName, textNode))
                }
            searchImage(children, sections).foreach(s => return Some(s))
          }
          case (linkNode@InternalLinkNode(destination, _, _, _)) if destination.namespace == Namespace.File => {
            for (fileName <- ImageExtractorConfig.ImageLinkRegex.findFirstIn(destination.encoded);
                 if checkImageRights(fileName)) {
                    return Some((fileName, linkNode))
                  }
          }
          case _ => {
            searchImage(node.children, sections).foreach(s => return Some(s))
          }
        }
      }
      None
    }

    private def checkImageRights(fileName: String) = !context.nonFreeImages.contains(fileName)    //not!

        /*
        FIXME what is this? can this be deleted?
        nodes match
        {
            case SectionNode(_, _, _) :: tail =>
            {
                if (sections > 1) return None
                return searchImage(tail, sections + 1)
            }
            case TemplateNode(_, children, _, _) :: tail =>
            {
                for (property <- children;
                     textNode @ TextNode(text, _) <- property.children;
                     fileName <- ImageRegex.findFirstIn(text);
                     encodedFileName = if (encodedLinkRegex.findFirstIn(fileName) == None)
                                           WikiUtil.wikiEncode(fileName, language)
                                       else
                                           fileName
                     if checkImageRights(encodedFileName))
                {
                    return Some(encodedFileName, textNode)
                }
                searchImage(children, sections).foreach(s => return Some(s))
                return searchImage(tail, sections)
            }
            case (linkNode @ InternalLinkNode(destination, _, _)) :: tail if destination.namespace == Namespace.File =>
            {
                for (fileName <- ImageLinkRegex.findFirstIn(destination.encoded);
                     if checkImageRights(fileName))
                {
                    return Some(fileName, linkNode)
                }
                return searchImage(tail, sections)
            }
            case head :: tail =>
            {
                searchImage(head.children, sections).foreach(s => return Some(s))
                return searchImage(tail, sections)
            }
            case Nil => return None
        }
        */

}

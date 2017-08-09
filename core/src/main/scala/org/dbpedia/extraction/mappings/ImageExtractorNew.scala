package org.dbpedia.extraction.mappings

import java.util.logging.Logger

import org.dbpedia.extraction.config.mappings.ImageExtractorConfig
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{ExtractorUtils, Language, WikiUtil}
import org.dbpedia.extraction.wikiparser._

import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

/**
  * Reworked Image Extractor
  */
class ImageExtractorNew(
                      context: {
                        def ontology: Ontology
                        def language: Language
                        def nonFreeImages: Seq[String]
                      }
)
extends PageNodeExtractor
{
  private val recursionDepth = 5

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

    imageSearch(node.children, 0).foreach(_ match {
      case Some((imageFileName, sourceNode)) => {
        val lang = language
        val url = ExtractorUtils.getFileURL(imageFileName, lang)
        val thumbnailUrl = ExtractorUtils.getThumbnailURL(imageFileName, lang)

        quads += new Quad(language, DBpediaDatasets.Images, subjectUri, foafDepictionProperty, url, sourceNode.sourceIri)
        quads += new Quad(language, DBpediaDatasets.Images, subjectUri, dbpediaThumbnailProperty, thumbnailUrl, sourceNode.sourceIri)
        quads += new Quad(language, DBpediaDatasets.Images, url, foafThumbnailProperty, thumbnailUrl, sourceNode.sourceIri)
        quads += new Quad(language, DBpediaDatasets.Images, url, rdfType, imageClass.uri, sourceNode.sourceIri)
        quads += new Quad(language, DBpediaDatasets.Images, thumbnailUrl, rdfType, imageClass.uri, sourceNode.sourceIri)

        val wikipediaImageUrl = language.baseUri + "/wiki/" + fileNamespaceIdentifier + ":" + imageFileName

        quads += new Quad(language, DBpediaDatasets.Images, url, dcRightsProperty, wikipediaImageUrl, sourceNode.sourceIri)
        quads += new Quad(language, DBpediaDatasets.Images, thumbnailUrl, dcRightsProperty, wikipediaImageUrl, sourceNode.sourceIri)
      }
      case None =>
    })
    quads
  }

  /**
    * Seaches for file names of images in the List of nodes.
    * @param nodes List of nodes that will be searched
    * @return List of filename and origin node
    */
  private def imageSearch(nodes: List[Node], depth : Int) : Seq[Option[(String, Node)]] = {

    var images = ArrayBuffer[Option[(String, Node)]]()

    // Match every node for TextNode, InternalLinkNode & InterWikiLinkNode
    // for other types of node => recursive search for these types in their children
    nodes.foreach(node => node match {
      case TextNode(_,_) => {
        ImageExtractorConfig.ImageLinkRegex.findAllIn(node.toWikiText).foreach(file => {
          // remove unwanted leftovers from the wikiText
          images += processImageLink(file.split("[:=\\|]").last, node)
        })
      }
      case InternalLinkNode(_,_,_,_) => {
        ImageExtractorConfig.ImageLinkRegex.findAllIn(node.toWikiText).foreach(file => {
          images += processImageLink(file.split("[:=\\|]").last, node)
        })
      }
      case InterWikiLinkNode(_,_,_,_) => {
        ImageExtractorConfig.ImageLinkRegex.findAllIn(node.toWikiText).foreach(file => {
          images += processImageLink(file.split("[:=\\|]").last, node)
        })
      }
      case _ => if(depth < recursionDepth) images ++= imageSearch(node.children, depth + 1)
    })
    images.toSeq
  }

  /**
    * Ensures Encoding and Copyright
    * @param fileName name of an imageFile
    * @param node Source-Node of the fileName
    * @return Encoded fileName of an Non-Non-Free Image
    */
  private def processImageLink(fileName: String, node: Node) : Option[(String, Node)] = {
    // Encoding
    var encodedFileName = fileName
    if (encodedLinkRegex.findFirstIn(fileName) == None)
      encodedFileName = WikiUtil.wikiEncode(fileName)

    // Copyright Check => Exclude Non-Free Images
    if(!context.nonFreeImages.contains(fileName)) Some((encodedFileName, node))
    else None
    }

}

/* In between rework version of search image method. Leftover for reference if something goes wrong.
   Might be deleted if everything works


  private def searchImage(nodes: List[Node], sections: Int): ArrayBuffer[Option[(String, Node)]] = {
    //nodes.foreach(node => ImageExtractorConfig.ImageLinkRegex.findAllIn(node.toPlainText).foreach(println))
    var files = ArrayBuffer[Option[(String, Node)]]()
    var currentSections = sections
    imageSearch(nodes).foreach(println)
    nodes.foreach(node =>
      node match {
//        case SectionNode(_, _, _, _) => { //Why was this needed?
//          if (currentSections > 1) files += None
//          currentSections += 1
//        }
        case TemplateNode(_, propertyNodes, _, _) => {
          // extract text from the node structure and return the encoded file names,
          // for the files that we have the rights to use.
          propertyNodes.foreach(_.children.foreach(contentNode => {
            contentNode match {
              case TextNode(text, _) => {
                ImageExtractorConfig.ImageRegex.findAllIn(text).foreach(fileName => {
                  // encode if not already encoded
                  var encodedFileName = fileName
                  if (encodedLinkRegex.findFirstIn(fileName) == None)
                    encodedFileName = WikiUtil.wikiEncode(fileName)
                  files += Some((encodedFileName, contentNode))
                  //println(encodedFileName)
                })
              }
              case InternalLinkNode(destination, _, _, _) => //ImageExtractorConfig.ImageLinkRegex.findAllIn(destination.encoded).foreach(println)
              case _ => //println("Unreviewed Content Node: " + nodeTypeCheck(contentNode))
                contentNode.children.foreach(_ match {
                  case PropertyNode(key, c, currentSections) => //searchImage(c, currentSections).foreach(a => files += a)
                  case TextNode(text, _) => //ImageExtractorConfig.ImageLinkRegex.findAllIn(text).foreach(println)
                })
            }
          }))
        }
        case InternalLinkNode(destination, _, _, _) if (destination.namespace == Namespace.File) => {
          ImageExtractorConfig.ImageLinkRegex.findFirstIn(destination.encoded).foreach(fileName =>
            //if(checkImageRights(fileName))
            files += Some((fileName, node)))
        }
        case TextNode(text, _) => ImageExtractorConfig.ImageLinkRegex.findAllIn(text).foreach(println) // Wichtig!
        case ExternalLinkNode(destination, _, _, _) =>
        case InterWikiLinkNode(destination, _, _, _) => ImageExtractorConfig.ImageLinkRegex.findAllIn(destination.encoded).foreach(println)
        case _ =>
      })
    files
  }

* */


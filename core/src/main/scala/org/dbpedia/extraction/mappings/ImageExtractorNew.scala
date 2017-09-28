package org.dbpedia.extraction.mappings

import java.net.URLDecoder

import org.dbpedia.extraction.config.mappings.ImageExtractorConfig
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{ExtractorUtils, Language, WikiUtil}
import org.dbpedia.extraction.wikiparser._

import scala.collection.mutable
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
                        def freeImages: Seq[String]
                      }
)
extends PageNodeExtractor
{
  private val recursionDepth = 5

  private val wikiCode = context.language.wikiCode

  private val language = context.language
  require(ImageExtractorConfig.supportedLanguages.contains(wikiCode), "ImageExtractor's supported languages: "+ImageExtractorConfig.supportedLanguages.mkString(", ")+"; not "+wikiCode)

  private val fileNamespaceIdentifier = Namespace.File.name(language)

  private val encodedLinkRegex = """%[0-9a-fA-F][0-9a-fA-F]""".r
  private val imageClass = context.ontology.classes("Image")
  private val dbpediaThumbnailProperty = context.ontology.properties("thumbnail")
  private val foafDepictionProperty = context.ontology.properties("foaf:depiction")
  private val foafThumbnailProperty = context.ontology.properties("foaf:thumbnail")
  private val dcRightsProperty = context.ontology.properties("dc:rights")

  private val mapProperty = context.ontology.properties("foaf:depiction") //TODO
  private val signatureProperty = context.ontology.properties("foaf:depiction") //TODO
  private val coatOfArmsProperty = context.ontology.properties("foaf:depiction") //TODO
  private val mainImageProperty = context.ontology.properties("foaf:depiction") //TODO
  private val flagProperty = context.ontology.properties("foaf:depiction") //TODO

  private val rdfType = context.ontology.properties("rdf:type")

  private val commonsLang = Language.Commons

  override val datasets = Set(DBpediaDatasets.Images)

  private var mainImageFound = false

  private var flagImage : ArrayBuffer[Option[(String, Node)]] = ArrayBuffer()
  private var coatOfArmsImage : ArrayBuffer[Option[(String, Node)]] = ArrayBuffer()
  private var mapImage : ArrayBuffer[Option[(String, Node)]] = ArrayBuffer()
  private var signatureImage : ArrayBuffer[Option[(String, Node)]] = ArrayBuffer()
  private var mainImage : Option[(String, Node)] = None

  override def extract(node: PageNode, subjectUri: String): Seq[Quad] =
  {
    if(node.title.namespace != Namespace.Main) return Seq.empty

    var quads = new ArrayBuffer[Quad]()
    val duplicateMap = mutable.HashMap[String, Boolean]()
    // Each Page needs a new main image
    mainImageFound = false

    // --------------- Quad Gen: Normal Images ---------------
    imageSearch(node.children, 0).foreach(_ match {
      case Some((imageFileName, sourceNode)) =>
        // quick duplicate check
        if(duplicateMap.get(imageFileName).isEmpty) {
          duplicateMap.put(imageFileName, true)

          val lang = if(context.freeImages.contains(URLDecoder.decode(imageFileName, "UTF-8")))
            language else commonsLang
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
    // --------------- Quad Gen: Special Images ---------------
    mainImage.foreach(img => {
      val lang = if(context.freeImages.contains(URLDecoder.decode(img._1, "UTF-8")))
        language else commonsLang
      val url = ExtractorUtils.getFileURL(img._1, lang)
      quads += new Quad(language, DBpediaDatasets.Images, subjectUri, mainImageProperty, url, img._2.sourceIri)
    })
    flagImage.foreach(_.foreach(img => {
      val lang = if(context.freeImages.contains(URLDecoder.decode(img._1, "UTF-8")))
        language else commonsLang
      val url = ExtractorUtils.getFileURL(img._1, lang)
      quads += new Quad(language, DBpediaDatasets.Images, subjectUri, flagProperty, url, img._2.sourceIri)
    }))
    coatOfArmsImage.foreach(_.foreach(img => {
      val lang = if(context.freeImages.contains(URLDecoder.decode(img._1, "UTF-8")))
        language else commonsLang
      val url = ExtractorUtils.getFileURL(img._1, lang)
      quads += new Quad(language, DBpediaDatasets.Images, subjectUri, coatOfArmsProperty, url, img._2.sourceIri)
    }))
    signatureImage.foreach(_.foreach(img => {
      val lang = if(context.freeImages.contains(URLDecoder.decode(img._1, "UTF-8")))
        language else commonsLang
      val url = ExtractorUtils.getFileURL(img._1, lang)
      quads += new Quad(language, DBpediaDatasets.Images, subjectUri, signatureProperty, url, img._2.sourceIri)
    }))
    mapImage.foreach(_.foreach(img => {
      val lang = if(context.freeImages.contains(URLDecoder.decode(img._1, "UTF-8")))
        language else commonsLang
      val url = ExtractorUtils.getFileURL(img._1, lang)
      quads += new Quad(language, DBpediaDatasets.Images, subjectUri, mapProperty, url, img._2.sourceIri)
    }))

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
      case TextNode(_,_) =>
        // toWikiText is used instead of toPlainText because some images would get lost.
        ImageExtractorConfig.ImageLinkRegex.findAllIn(node.toWikiText).foreach(file => {
          // split on "[:=\\|]" removes unwanted leftovers from the wikiText
          images += processImageLink(file.split("[:=\\|]").last, node)
        })
      case InternalLinkNode(_,_,_,_) =>
          ImageExtractorConfig.ImageLinkRegex.findAllIn(node.toWikiText).foreach(file => {
            images += processImageLink(file.split("[:=\\|]").last, node)
          })
      case InterWikiLinkNode(_,_,_,_) =>
        ImageExtractorConfig.ImageLinkRegex.findAllIn(node.toWikiText).foreach(file => {
          images += processImageLink(file.split("[:=\\|]").last, node)
        })
      case _ =>
        if(depth < recursionDepth) images ++= imageSearch(node.children, depth + 1)
    })
    images
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
    if (encodedLinkRegex.findFirstIn(fileName).isEmpty)
      encodedFileName = WikiUtil.wikiEncode(fileName)

    val result = if(!context.nonFreeImages.contains(fileName)) Some((encodedFileName, node))
    else None

    // --------------- Special Images ---------------
      ImageExtractorConfig.flagRegex.findFirstIn(encodedFileName).foreach(_ => {
        flagImage += result
      })
      ImageExtractorConfig.mapRegex.findFirstIn(encodedFileName).foreach(_ => {
        mapImage += result
      })
      ImageExtractorConfig.cOARegex.findFirstIn(encodedFileName).foreach(_ => {
        coatOfArmsImage += result
      })
      ImageExtractorConfig.signatureRegex.findFirstIn(encodedFileName).foreach(_ => {
        signatureImage += result
      })
    if(!mainImageFound){
      // First Image will be defined as main Image
      mainImage = result
      mainImageFound = true
    }
    result
  }
}
package org.dbpedia.extraction.mappings

import java.util.logging.Logger
import org.dbpedia.extraction.config.mappings.ImageExtractorConfig
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.util.{ExtractorUtils, Language, WikiApi, WikiUtil}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.util.RichString.wrapString
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.language.reflectiveCalls

/**
  * Extracts the first image of a Wikipedia page. Constructs a thumbnail from it, and
  * the full size image.
  *
  * FIXME: we're sometimes dealing with encoded links, sometimes with decoded links. It's quite a mess.
  */
class ImageExtractorLive(
  context: {
    def ontology: Ontology
    def language: Language
    def articlesSource: Source
    def commonsSource: Source
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

  override def extract(node: PageNode, subjectUri: String, pageContext: PageContext): Seq[Quad] =
  {
    if(node.title.namespace != Namespace.Main) return Seq.empty

    var quads = new ArrayBuffer[Quad]()

    val api = new WikiApi(null, language)
    var firstImage = true
    for ((imageFileName, sourceNode) <- searchImages(node.children, 0).flatten if !imageFileName.toLowerCase.startsWith("replace_this_image"))
    {
      val lang = if (api.fileExistsOnWiki(imageFileName, commonsLang))
        commonsLang else language
      val url = ExtractorUtils.getDbpediaFileURL(imageFileName, lang)
      val thumbnailUrl = ExtractorUtils.getThumbnailURL(imageFileName, lang)
      val wikipediaImageUrl = language.baseUri+"/wiki/"+fileNamespaceIdentifier+":"+imageFileName

      if (firstImage) {
        if (lang == language) {
          quads += new Quad(language, DBpediaDatasets.Images, url, foafThumbnailProperty, thumbnailUrl, sourceNode.sourceUri)
        }

        quads += new Quad(language, DBpediaDatasets.Images, thumbnailUrl, rdfType, imageClass.uri, sourceNode.sourceUri)
        quads += new Quad(language, DBpediaDatasets.Images, thumbnailUrl, dcRightsProperty, wikipediaImageUrl, sourceNode.sourceUri)
        quads += new Quad(language, DBpediaDatasets.Images, subjectUri, dbpediaThumbnailProperty, thumbnailUrl, sourceNode.sourceUri)

        firstImage = false
      }

      if (lang == language) {
        quads += new Quad(language, DBpediaDatasets.Images, url, rdfType, imageClass.uri, sourceNode.sourceUri)
        quads += new Quad(language, DBpediaDatasets.Images, url, dcRightsProperty, wikipediaImageUrl, sourceNode.sourceUri)
      }

      quads += new Quad(language, DBpediaDatasets.Images, subjectUri, foafDepictionProperty, url, sourceNode.sourceUri)
    }

    quads
  }

  private def searchImages(nodes: List[Node], sections: Int): List[Option[(String, Node)]] =
  {
    var images = new ListBuffer[Option[(String, Node)]]()
    var currentSections = sections
    for (node <- nodes)
    {
      node match
      {
        case SectionNode(_, _, _, _) =>
        {
          currentSections += 1
        }
        case TemplateNode(_, children, _, _) =>
        {
          for (property <- children;
               textNode @ TextNode(text, _) <- property.children;
               fileName <- ImageExtractorConfig.ImageRegex.findFirstIn(text);
               encodedFileName = if (encodedLinkRegex.findFirstIn(fileName) == None)
                 WikiUtil.wikiEncode(fileName).capitalize(language.locale)
               else
                 fileName
               )
          {
            images += Some((encodedFileName, textNode))
          }
          searchImages(children, sections).foreach(s => images :+ Some(s))
        }
        // match Files included over galleries format("File:<filename>|<futherText>")
        case (textNode @ TextNode(text, line)) if (text.contains("|")) =>
        {
          val filestring = text.split("\\|").head.replace("\n","").replace("<gallery>","").replace("File:", "")
          val filename = ImageExtractorConfig.ImageRegex.findFirstIn(filestring)
          if (filename != None){
            images += Some((filename.get, textNode))
          }
        }
        case (linkNode @ InternalLinkNode(destination, _, _, _)) if destination.namespace == Namespace.File =>
        {
          for (fileName <- ImageExtractorConfig.ImageLinkRegex.findFirstIn(destination.encoded))
          {
            images += Some((fileName, linkNode))
          }
        }
        case _ =>
        {
          searchImages(node.children, sections).foreach(s => images += s)
        }
      }
    }
    images.toList

  }

}

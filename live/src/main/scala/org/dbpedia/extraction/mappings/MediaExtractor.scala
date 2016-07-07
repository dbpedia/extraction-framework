package org.dbpedia.extraction.mappings

import java.util.logging.Logger

import org.dbpedia.extraction.config.mappings.MediaExtractorConfig
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.{Ontology, OntologyProperty}
import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.util.{ExtractorUtils, Language, WikiApi, WikiUtil}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.util.RichString.wrapString

import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.language.reflectiveCalls

/**
  * Extracts all media files of a Wikipedia page. Constructs a thumbnail image from it, and
  * links to the resources in DBpedia Commons
  *
  * FIXME: we're sometimes dealing with encoded links, sometimes with decoded links. It's quite a mess.
  */
class MediaExtractor(
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

  require(MediaExtractorConfig.supportedLanguages.contains(wikiCode), "MediaExtractor's supported languages: "+MediaExtractorConfig.supportedLanguages.mkString(", ")+"; not "+wikiCode)

  private val fileNamespaceIdentifier = Namespace.File.name(language)

  private val logger = Logger.getLogger(classOf[MappingExtractor].getName)

  private val encodedLinkRegex = """%[0-9a-fA-F][0-9a-fA-F]""".r

  private val imageClass = context.ontology.classes("Image")
  private val dbpediaThumbnailProperty = context.ontology.properties("thumbnail")
  private val foafDepictionProperty = context.ontology.properties("foaf:depiction")
  private val foafThumbnailProperty = context.ontology.properties("foaf:thumbnail")
  private val dcRightsProperty = context.ontology.properties("dc:rights")
  private val rdfType = context.ontology.properties("rdf:type")

  private val soundClass = context.ontology.classes("Sound")

  private val mediaItem = new OntologyProperty("dbpedia-owl:mediaItem", Map(Language.English -> "mediaItem"), Map(), null, null, false, Set(), Set())

  private val commonsLang = Language.Commons

  override val datasets = Set(DBpediaDatasets.Images)

  override def extract(node: PageNode, subjectUri: String, pageContext: PageContext): Seq[Quad] =
  {
    if(node.title.namespace != Namespace.Main) return Seq.empty

    var quads = new ArrayBuffer[Quad]()

    val api = new WikiApi(null, language)
    var firstImage = true
    for ((mediaFileName, sourceNode) <- searchMedia(node.children, 0).flatten
         if (!mediaFileName.toLowerCase.startsWith("replace_this_image"))
           && (api.fileExistsOnWiki(mediaFileName, commonsLang)))
    {
      val dbpediaUrl = ExtractorUtils.getDbpediaFileURL (mediaFileName, commonsLang)
      val url = ExtractorUtils.getFileURL (mediaFileName, commonsLang)

      if (mediaFileName.matches(MediaExtractorConfig.ImageRegex.regex)) {
          val thumbnailUrl = ExtractorUtils.getThumbnailURL (mediaFileName, commonsLang)
          val wikipediaMediaUrl = language.baseUri + "/wiki/" + fileNamespaceIdentifier + ":" + mediaFileName

          if (firstImage) {
            quads += new Quad (language, DBpediaDatasets.Images, url, foafThumbnailProperty, thumbnailUrl, sourceNode.sourceUri)
            quads += new Quad (language, DBpediaDatasets.Images, thumbnailUrl, rdfType, imageClass.uri, sourceNode.sourceUri)
            quads += new Quad (language, DBpediaDatasets.Images, thumbnailUrl, dcRightsProperty, wikipediaMediaUrl, sourceNode.sourceUri)
            quads += new Quad (language, DBpediaDatasets.Images, subjectUri, dbpediaThumbnailProperty, thumbnailUrl, sourceNode.sourceUri)

          firstImage = false
          }

          quads += new Quad (language, DBpediaDatasets.Images, url, rdfType, imageClass.uri, sourceNode.sourceUri)
          quads += new Quad (language, DBpediaDatasets.Images, url, dcRightsProperty, wikipediaMediaUrl, sourceNode.sourceUri)

          quads += new Quad (language, DBpediaDatasets.Images, subjectUri, foafDepictionProperty, url, sourceNode.sourceUri)
        }
      else if (mediaFileName.matches(MediaExtractorConfig.SoundRegex.regex)) {
        quads += new Quad (language, DBpediaDatasets.Sounds, url, rdfType, soundClass.uri, sourceNode.sourceUri)
      }
      else if (mediaFileName.matches(MediaExtractorConfig.VideoRegex.regex)) {
        // Do nothing for videos as of now
      }

      // Same quad for every media resource
      quads += new Quad(language, DBpediaDatasets.Images, subjectUri, mediaItem, dbpediaUrl, sourceNode.sourceUri)
    }

    quads
  }

  private def searchMedia(nodes: List[Node], sections: Int): List[Option[(String, Node)]] =
  {
    var media = new ListBuffer[Option[(String, Node)]]()
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
               fileName <- MediaExtractorConfig.MediaRegex.findFirstIn(text);
               encodedFileName = if (encodedLinkRegex.findFirstIn(fileName) == None)
                 WikiUtil.wikiEncode(fileName).capitalize(language.locale)
               else
                 fileName
               )
          {
            media += Some((encodedFileName, textNode))
          }
          searchMedia(children, sections).foreach(s => media :+ Some(s))
        }
        // match Files included over galleries format("File:<filename>|<futherText>")
        case (textNode @ TextNode(text, line)) if (text.contains("|")) =>
        {
          val filestring = text.split("\\|").head.replace("\n","").replace("<gallery>","").replace("File:", "")
          val filename = MediaExtractorConfig.ImageRegex.findFirstIn(filestring)
          if (filename != None){
            media += Some((filename.get, textNode))
          }
        }
        case (linkNode @ InternalLinkNode(destination, _, _, _)) if destination.namespace == Namespace.File =>
        {
          for (fileName <- MediaExtractorConfig.ImageLinkRegex.findFirstIn(destination.encoded))
          {
            media += Some((fileName, linkNode))
          }
        }
        case _ =>
        {
          searchMedia(node.children, sections).foreach(s => media += s)
        }
      }
    }
    media.toList

  }

}

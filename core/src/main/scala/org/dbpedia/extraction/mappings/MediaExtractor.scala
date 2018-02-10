package org.dbpedia.extraction.mappings

import org.apache.log4j.Logger
import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.{ExtractionLogger, ExtractionRecorder}
import org.dbpedia.extraction.config.mappings.MediaExtractorConfig
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.mappings.MappingsLoader.getClass
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
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
@SoftwareAgentAnnotation(classOf[MediaExtractor], AnnotationType.Extractor)
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

  private val logger = ExtractionLogger.getLogger(getClass, context.language)

  private val encodedLinkRegex = """%[0-9a-fA-F][0-9a-fA-F]""".r

  private val imageClass = context.ontology.classes("Image")
  private val soundClass = context.ontology.classes("Sound")
  private val dbpediaThumbnailProperty = context.ontology.properties("thumbnail")
  private val foafDepictionProperty = context.ontology.properties("foaf:depiction")
  private val foafThumbnailProperty = context.ontology.properties("foaf:thumbnail")
  private val dcRightsProperty = context.ontology.properties("dc:rights")
  private val rdfType = context.ontology.properties("rdf:type")
  private val mediaItem = context.ontology.properties("mediaItem")

  private val commonsLang = Language.Commons

  override val datasets = Set(DBpediaDatasets.Images, DBpediaDatasets.Sounds)

  private val qb = QuadBuilder.dynamicPredicate(language, DBpediaDatasets.Images)
  qb.setExtractor(this.softwareAgentAnnotation)

  override def extract(node: PageNode, subjectUri: String): Seq[Quad] =
  {
    if(node.title.namespace != Namespace.Main) return Seq.empty

    var quads = new ArrayBuffer[Quad]()

    val api = new WikiApi(null, language)
    var firstImage = true
    for ((mediaFileName, sourceNode) <- searchMedia(node.children, 0).flatten
         if (!mediaFileName.toLowerCase.startsWith("replace_this_image"))
           && (api.fileExistsOnWiki(mediaFileName, commonsLang)))
    {
      qb.setSourceUri(sourceNode.sourceIri)
      qb.setNodeRecord(node.getNodeRecord)

      val dbpediaUrl = ExtractorUtils.getDbpediaFileURL (mediaFileName, commonsLang)
      val url = ExtractorUtils.getFileURL (mediaFileName, commonsLang)

      if (mediaFileName.matches(MediaExtractorConfig.ImageRegex.regex)) {
          val thumbnailUrl = ExtractorUtils.getThumbnailURL (mediaFileName, commonsLang)
          val wikipediaMediaUrl = language.baseUri + "/wiki/" + fileNamespaceIdentifier + ":" + mediaFileName

          if (firstImage) {
            qb.setSubject(url)
            qb.setPredicate(foafThumbnailProperty)
            qb.setValue(thumbnailUrl)
            quads += qb.getQuad

            qb.setSubject(thumbnailUrl)
            qb.setPredicate(rdfType)
            qb.setValue(imageClass.uri)
            quads += qb.getQuad

            qb.setSubject(thumbnailUrl)
            qb.setPredicate(dcRightsProperty)
            qb.setValue(wikipediaMediaUrl)
            quads += qb.getQuad

            qb.setSubject(subjectUri)
            qb.setPredicate(dbpediaThumbnailProperty)
            qb.setValue(thumbnailUrl)
            quads += qb.getQuad

          firstImage = false
          }

        qb.setSubject(url)
        qb.setPredicate(rdfType)
        qb.setValue(imageClass.uri)
        quads += qb.getQuad

        qb.setSubject(url)
        qb.setPredicate(dcRightsProperty)
        qb.setValue(wikipediaMediaUrl)
        quads += qb.getQuad

        qb.setSubject(subjectUri)
        qb.setPredicate(foafDepictionProperty)
        qb.setValue(url)
        quads += qb.getQuad
      }
      else if (mediaFileName.matches(MediaExtractorConfig.SoundRegex.regex)) {
        val qbs = qb.clone
        qbs.setDataset(DBpediaDatasets.Sounds)
        qbs.setSubject(url)
        qbs.setPredicate(rdfType)
        qbs.setValue(soundClass.uri)
        quads += qbs.getQuad
      }
      else if (mediaFileName.matches(MediaExtractorConfig.VideoRegex.regex)) {
        // FIXME Do nothing for videos as of now
      }

      // Same quad for every media resource
      qb.setSubject(subjectUri)
      qb.setPredicate(mediaItem)
      qb.setValue(dbpediaUrl)
      quads += qb.getQuad
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
               textNode @ TextNode(text, _, _) <- property.children;
               fileName <- MediaExtractorConfig.MediaRegex.findFirstIn(text);
               encodedFileName = if (encodedLinkRegex.findFirstIn(fileName) == None)
                 WikiUtil.wikiEncode(fileName).capitalize(language.locale)
               else
                 fileName
               )
          {
            media += Some((encodedFileName, textNode))
          }
          searchMedia(children, sections).foreach(s => media += s)
        }
        // match Files included over galleries format("File:<filename>|<futherText>")
        case (textNode @ TextNode(text, line, _)) if (text.contains("|")) =>
        {
          val textArray = text.split("\\|")
          if (textArray.nonEmpty) {
            val filestring = textArray.head.replace("\n", "").replaceAll(".*?<gallery.*?>", "").replace("File:", "")
            val filename = MediaExtractorConfig.ImageRegex.findFirstIn(filestring)
            if (filename != None) {
              val encodedFileName = WikiUtil.wikiEncode(filename.get).capitalize(language.locale)
              media += Some((encodedFileName, textNode))
            }
          }
        }
        case (linkNode @ InternalLinkNode(destination, _, _, _)) if destination.namespace == Namespace.File =>
        {
          for (fileName <- MediaExtractorConfig.MediaLinkRegex.findFirstIn(destination.encoded))
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

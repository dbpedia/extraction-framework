package org.dbpedia.extraction.mappings

import java.util.logging.Logger
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import impl.wikipedia.Namespaces
import org.dbpedia.extraction.sources.Source
import collection.mutable.{HashSet, Set => MutableSet}
import java.math.BigInteger
import java.security.MessageDigest
import org.dbpedia.extraction.config.mappings.ImageExtractorConfig
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{Language, WikiUtil}
import java.net.URLDecoder
import org.dbpedia.extraction.util.RichString.toRichString
import scala.collection.mutable.ArrayBuffer

/**
 * Extracts the first image of a Wikipedia page. Constructs a thumbnail from it, and
 * the full size image.
 * 
 * FIXME: we're sometimes dealing with encoded links, sometimes with decoded links. It's quite a mess.
 */
class ImageExtractor( 
  context: {
    def ontology: Ontology
    def language: Language
    def articlesSource: Source
    def commonsSource: Source 
  } 
) 
extends Extractor
{
  private val wikiCode = context.language.wikiCode
  private val language = context.language

  require(ImageExtractorConfig.supportedLanguages.contains(wikiCode), "ImageExtractor's supported languages: "+ImageExtractorConfig.supportedLanguages.mkString(", ")+"; not "+wikiCode)

  private val fileNamespaceIdentifier = Namespace.File.name(language)

  private val wikipediaUrlLangPrefix = ImageExtractorConfig.wikipediaUrlPrefix + wikiCode +"/"
  private val commonsUrlPrefix = ImageExtractorConfig.wikipediaUrlPrefix + "commons/"

  private val logger = Logger.getLogger(classOf[MappingExtractor].getName)

  private val encodedLinkRegex = """%[0-9a-fA-F][0-9a-fA-F]""".r

  logger.info("Loadings images")
  private val nonFreeImages = new HashSet[String]()
  private val freeWikipediaImages = new HashSet[String]()
  ImageExtractor.loadImages(context.commonsSource, null, nonFreeImages, wikiCode)
  ImageExtractor.loadImages(context.articlesSource, freeWikipediaImages, nonFreeImages, wikiCode)
  logger.info("Images loaded from dump")

  private val dbpediaThumbnailProperty = context.ontology.properties("thumbnail")
  private val foafDepictionProperty = context.ontology.properties("foaf:depiction")
  private val foafThumbnailProperty = context.ontology.properties("foaf:thumbnail")
  private val dcRightsProperty = context.ontology.properties("dc:rights")

  override val datasets = Set(DBpediaDatasets.Images)

    override def extract(node: PageNode, subjectUri: String, pageContext: PageContext): Seq[Quad] =
    {
        if(node.title.namespace != Namespace.Main) return Seq.empty
        
        var quads = new ArrayBuffer[Quad]()

        for ((imageFileName, sourceNode) <- searchImage(node.children, 0) if !imageFileName.toLowerCase.startsWith("replace_this_image"))
        {
            val (url, thumbnailUrl) = getImageUrl(imageFileName)

            quads += new Quad(language, DBpediaDatasets.Images, subjectUri, foafDepictionProperty, url, sourceNode.sourceUri)
            quads += new Quad(language, DBpediaDatasets.Images, subjectUri, dbpediaThumbnailProperty, thumbnailUrl, sourceNode.sourceUri)
            quads += new Quad(language, DBpediaDatasets.Images, url, foafThumbnailProperty, thumbnailUrl, sourceNode.sourceUri)

            val wikipediaImageUrl = "http://" + wikiCode + ".wikipedia.org/wiki/"+ fileNamespaceIdentifier +":" + imageFileName

            quads += new Quad(language, DBpediaDatasets.Images, url, dcRightsProperty, wikipediaImageUrl, sourceNode.sourceUri)
            quads += new Quad(language, DBpediaDatasets.Images, thumbnailUrl, dcRightsProperty, wikipediaImageUrl, sourceNode.sourceUri)
        }

        quads
    }

    private def searchImage(nodes: List[Node], sections: Int): Option[(String, Node)] =
    {
        var currentSections = sections
        for (node <- nodes)
        {
            node match
            {
                case SectionNode(_, _, _, _) =>
                {
                    if (currentSections > 1) return None
                    currentSections += 1
                }
                case TemplateNode(_, children, _) =>
                {
                    for (property <- children;
                         textNode @ TextNode(text, _) <- property.children;
                         fileName <- ImageExtractorConfig.ImageRegex.findFirstIn(text);
                         encodedFileName = if (encodedLinkRegex.findFirstIn(fileName) == None)
                                               WikiUtil.wikiEncode(fileName).capitalize(language.locale)
                                           else
                                               fileName
                         if checkImageRights(encodedFileName))
                    {
                        return Some((encodedFileName, textNode))
                    }
                    searchImage(children, sections).foreach(s => return Some(s))
                }
                case (linkNode @ InternalLinkNode(destination, _, _, _)) if destination.namespace == Namespace.File =>
                {
                    for (fileName <- ImageExtractorConfig.ImageLinkRegex.findFirstIn(destination.encoded);
                         if checkImageRights(fileName))
                    {
                        return Some((fileName, linkNode))
                    }
                }
                case _ =>
                {
                    searchImage(node.children, sections).foreach(s => return Some(s))
                }
            }
        }
        None

        /*
        nodes match
        {
            case SectionNode(_, _, _) :: tail =>
            {
                if (sections > 1) return None
                return searchImage(tail, sections + 1)
            }
            case TemplateNode(_, children, _) :: tail =>
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

    private def checkImageRights(fileName: String) = (!nonFreeImages.contains(fileName))

    private def getImageUrl(fileName: String): (String, String) =
    {
      val urlPrefix = if(freeWikipediaImages.contains(URLDecoder.decode(fileName, "UTF-8"))) wikipediaUrlLangPrefix else commonsUrlPrefix

      val md = MessageDigest.getInstance("MD5")
      val messageDigest = md.digest(URLDecoder.decode(fileName, "UTF-8").getBytes)
      var md5 = (new BigInteger(1, messageDigest)).toString(16)

      // If the lenght of the MD5 hash is less than 32, then we should pad leading zeros to it, as converting it to
      // BigInteger will result in removing all leading zeros.
      while (md5.length < 32)
        md5 = "0" + md5;

      val hash1 = md5.substring(0, 1)
      val hash2 = md5.substring(0, 2);

      val urlPart = hash1 + "/" + hash2 + "/" + fileName
      val ext = if (fileName.toLowerCase.endsWith(".svg")) ".png" else ""

      val imageUrl = urlPrefix + urlPart
      val thumbnailUrl = urlPrefix + "thumb/" + urlPart + "/200px-" + fileName + ext

      (imageUrl, thumbnailUrl)
    }
}

private object ImageExtractor
{
    private def loadImages(source: Source, freeImages: MutableSet[String], nonFreeImages: MutableSet[String], wikiCode: String)
    {
        val logger = Logger.getLogger(classOf[ImageExtractor].getName)
        val startTime = System.nanoTime

        for(page <- source if page.title.namespace == Namespace.File;
            ImageExtractorConfig.ImageLinkRegex <- List(page.title.encoded) )
        {
            ImageExtractorConfig.NonFreeRegex(wikiCode).findFirstIn(page.source) match
            {
                case Some(_) => nonFreeImages += page.title.encoded
                case None => if (freeImages != null) freeImages += page.title.encoded
            }
        }

        //val count = source.count(page => !ImageLinkRegex.unapplySeq(page.title.decoded).isEmpty)

        val time = (System.nanoTime() - startTime).toDouble / 1000000000.0
        if (freeImages != null) logger.info("Found " + freeImages.size + " free images in Wikipedia")
        logger.info("Found " + nonFreeImages.size + " non free images in Wikipedia or Commons (" + time + " s)")
    }
}

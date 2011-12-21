package org.dbpedia.extraction.mappings

import java.util.logging.Logger
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad, Graph}
import org.dbpedia.extraction.wikiparser._
import impl.wikipedia.Namespaces
import collection.mutable.{HashSet, Set => MutableSet}
import java.math.BigInteger
import java.security.MessageDigest
import org.dbpedia.extraction.config.mappings.ImageExtractorConfig
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{Language, WikiUtil}
import java.net.URLDecoder
import org.dbpedia.extraction.sources.{XMLSource, Source}
import java.io.File


/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: 12/19/11
 * Time: 8:15 PM
 * This is the ImageExtarctor for Live extraction, as the other one depends mainly on the existence of dumps
 */

class LiveImageExtractor( context : {
  def ontology : Ontology
  def language : Language
  def articlesSource : Source
  def commonsSource : Source } ) extends Extractor
{
  private val language = context.language.wikiCode

  require(ImageExtractorConfig.supportedLanguages.contains(language), "ImageExtractor's supported languages: "+ImageExtractorConfig.supportedLanguages.mkString(", ")+"; not "+language)

  private val fileNamespaceIdentifier = Namespaces.getNameForNamespace(context.language, WikiTitle.Namespace.File)

  private val wikipediaUrlLangPrefix = ImageExtractorConfig.wikipediaUrlPrefix + language +"/"
  private val commonsUrlPrefix = ImageExtractorConfig.wikipediaUrlPrefix + "commons/"

  private val logger = Logger.getLogger(classOf[MappingExtractor].getName)

  private val encodedLinkRegex = """%[0-9a-fA-F][0-9a-fA-F]""".r

  logger.info("Loading nonfree images")
  private val nonFreeImages = new HashSet[String]()
  private val freeWikipediaImages = new HashSet[String]()
  LiveImageExtractor.loadImages("image_names.txt",  nonFreeImages, language)
  logger.info("Nonfree images loaded from file")

  private val dbpediaThumbnailProperty = context.ontology.getProperty("thumbnail").get
  private val foafDepictionProperty = context.ontology.getProperty("foaf:depiction").get
  private val foafThumbnailProperty = context.ontology.getProperty("foaf:thumbnail").get
  private val dcRightsProperty = context.ontology.getProperty("dc:rights").get

  override def extract(node : PageNode, subjectUri : String, pageContext : PageContext) : Graph =
  {
    if(node.title.namespace != WikiTitle.Namespace.Main) return new Graph()

    var quads = List[Quad]()

    for ((imageFileName, sourceNode) <- searchImage(node.children, 0) if !imageFileName.toLowerCase.startsWith("replace_this_image"))
    {
      val (url, thumbnailUrl) = getImageUrl(imageFileName)

      quads ::= new Quad(context.language, DBpediaDatasets.Images, subjectUri, foafDepictionProperty, url, sourceNode.sourceUri)
      quads ::= new Quad(context.language, DBpediaDatasets.Images, subjectUri, dbpediaThumbnailProperty, thumbnailUrl, sourceNode.sourceUri)
      quads ::= new Quad(context.language, DBpediaDatasets.Images, url, foafThumbnailProperty, thumbnailUrl, sourceNode.sourceUri)

      val wikipediaImageUrl = "http://" + language + ".wikipedia.org/wiki/"+ fileNamespaceIdentifier +":" + imageFileName

      quads ::= new Quad(context.language, DBpediaDatasets.Images, url, dcRightsProperty, wikipediaImageUrl, sourceNode.sourceUri)
      quads ::= new Quad(context.language, DBpediaDatasets.Images, thumbnailUrl, dcRightsProperty, wikipediaImageUrl, sourceNode.sourceUri)
    }

    new Graph(quads)
  }

  private def searchImage(nodes : List[Node], sections : Int) : Option[(String, Node)] =
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
                 WikiUtil.wikiEncode(fileName, context.language)
               else
                 fileName
               if checkImageRights(encodedFileName))
          {
            return Some((encodedFileName, textNode))
          }
          searchImage(children, sections).foreach(s => return Some(s))
        }
        case (linkNode @ InternalLinkNode(destination, _, _, _)) if destination.namespace == WikiTitle.Namespace.File =>
        {
          for (fileName <- ImageExtractorConfig.ImageLinkRegex.findFirstIn(destination.encoded))
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

  }

  private def checkImageRights(fileName : String) = (!nonFreeImages.contains(fileName))

  private def getImageUrl(fileName : String) : (String, String) =
  {
    val urlPrefix = if(nonFreeImages.contains(URLDecoder.decode(fileName, "UTF-8")))
      wikipediaUrlLangPrefix
    else
      commonsUrlPrefix

    val md = MessageDigest.getInstance("MD5")
    //val messageDigest = md.digest(fileName.getBytes)
    val messageDigest = md.digest(URLDecoder.decode(fileName, "UTF-8").getBytes);
    var md5 = (new BigInteger(1, messageDigest)).toString(16)
    //val testmd5 =Integer.toHexString(messageDigest);
    //If the lenght of the MD5 hash is less than 32, then we should pad leading zeros to it, as converting it to
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

private object LiveImageExtractor
{
  private def loadImages(imageListFileName: String, nonFreeImages: MutableSet[String], lang : String)
  {
    val logger = Logger.getLogger(classOf[ImageExtractor].getName)
    val startTime = System.nanoTime

    val source = scala.io.Source.fromFile(imageListFileName,"latin1");
//    val lines = source .mkString

    source.getLines.foreach(line => {
      try{

        val imageName = line.trim;
        //println("IMAGE NAME = " + imageName);
        nonFreeImages += imageName;
      }
      catch
      {
        case ex  : Exception => println("Error")
      }

    })

    source.close ();

    //println(lines);


//    val time = (System.nanoTime() - startTime).toDouble / 1000000000.0
//    if (freeImages != null) logger.info("Found " + freeImages.size + " free images in Wikipedia")
//    logger.info("Found " + nonFreeImages.size + " non free images in Wikipedia or Commons (" + time + " s)")
  }
}

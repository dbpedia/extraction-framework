package org.dbpedia.extraction.mappings

import java.net.URLDecoder

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.mappings.ImageExtractorConfig
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util._
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.iri.UriUtils

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.language.reflectiveCalls

/**
  * Reworked Image Extractor
  */
@SoftwareAgentAnnotation(classOf[ImageExtractorNew], AnnotationType.Extractor)
class ImageExtractorNew(
                         context: {
                           def ontology: Ontology
                           def language: Language
                           def nonFreeImages: Seq[String]
                           def freeImages: Seq[String]
                         }
                       )
  extends PageNodeExtractor {
  private val recursionDepth = 5

  private val wikiCode = context.language.wikiCode

  private val language = context.language
  require(ImageExtractorConfig.supportedLanguages.contains(wikiCode), "ImageExtractor's supported languages: " + ImageExtractorConfig.supportedLanguages.mkString(", ") + "; not " + wikiCode)

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

  override val datasets = Set(DBpediaDatasets.Images)

  private var mainImageFound = false

  private var flagImage: ArrayBuffer[Option[(String, Node)]] = ArrayBuffer()
  private var coatOfArmsImage: ArrayBuffer[Option[(String, Node)]] = ArrayBuffer()
  private var mapImage: ArrayBuffer[Option[(String, Node)]] = ArrayBuffer()
  private var signatureImage: ArrayBuffer[Option[(String, Node)]] = ArrayBuffer()
  private var mainImage: Option[(String, Node)] = None

  private var imageCount = 0

  override def extract(node: PageNode, subjectUri: String): Seq[Quad] = {

    imageCount = 0
    flagImage = ArrayBuffer[Option[(String, Node)]]()
    coatOfArmsImage = ArrayBuffer[Option[(String, Node)]]()
    signatureImage = ArrayBuffer[Option[(String, Node)]]()
    mapImage = ArrayBuffer[Option[(String, Node)]]()

    if (node.title.namespace != Namespace.Main) return Seq.empty

    var quads = new ArrayBuffer[Quad]()
    val duplicateMap = mutable.HashMap[String, Boolean]()
    // Each Page needs a new main image
    mainImageFound = false

    // --------------- Quad Gen: Normal Images ---------------
    imageSearch(node.children, 0).foreach {
      case Some((imageFileName, sourceNode)) =>
        // quick duplicate check
        if (duplicateMap.get(imageFileName).isEmpty) {
          duplicateMap.put(imageFileName, true)

          val lang = if (context.freeImages.contains(URLDecoder.decode(imageFileName, "UTF-8")))
            language else Language.Commons
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
    }
    // --------------- Quad Gen: Special Images ---------------
    mainImage.foreach(img => {
      val lang = if (context.freeImages.contains(URLDecoder.decode(img._1, "UTF-8")))
        language else Language.Commons
      val url = ExtractorUtils.getFileURL(img._1, lang)
      quads += new Quad(language, DBpediaDatasets.Images, subjectUri, mainImageProperty, url, img._2.sourceIri)
    })
    flagImage.foreach(_.foreach(img => {
      val lang = if (context.freeImages.contains(URLDecoder.decode(img._1, "UTF-8")))
        language else Language.Commons
      val url = ExtractorUtils.getFileURL(img._1, lang)
      quads += new Quad(language, DBpediaDatasets.Images, subjectUri, flagProperty, url, img._2.sourceIri)
    }))
    coatOfArmsImage.foreach(_.foreach(img => {
      val lang = if (context.freeImages.contains(URLDecoder.decode(img._1, "UTF-8")))
        language else Language.Commons
      val url = ExtractorUtils.getFileURL(img._1, lang)
      quads += new Quad(language, DBpediaDatasets.Images, subjectUri, coatOfArmsProperty, url, img._2.sourceIri)
    }))
    signatureImage.foreach(_.foreach(img => {
      val lang = if (context.freeImages.contains(URLDecoder.decode(img._1, "UTF-8")))
        language else Language.Commons
      val url = ExtractorUtils.getFileURL(img._1, lang)
      quads += new Quad(language, DBpediaDatasets.Images, subjectUri, signatureProperty, url, img._2.sourceIri)
    }))
    mapImage.foreach(_.foreach(img => {
      val lang = if (context.freeImages.contains(URLDecoder.decode(img._1, "UTF-8")))
        language else Language.Commons
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
  private def imageSearch(nodes: List[Node], depth: Int): Seq[Option[(String, Node)]] = {

    var images = ArrayBuffer[Option[(String, Node)]]()

    // Match every node for TextNode, InternalLinkNode & InterWikiLinkNode
    // for other types of node => recursive search for these types in their children

    nodes.foreach {
      case node@TextNode(_, _, _) =>
        // toWikiText is used instead of toPlainText because some images would get lost.
        totallyNotImageLinkRegex(node.toWikiText).foreach(file => {
          images += processImageLink(file, node)
          imageCount += 1
        })
      //times += "ImageLinkRegex Finish: " -> System.currentTimeMillis()
      case node@InternalLinkNode(_, _, _, _) =>
        totallyNotImageLinkRegex(node.toWikiText).foreach(file => {
          images += processImageLink(file, node)
          imageCount += 1
        })
      case node@InterWikiLinkNode(_, _, _, _) =>
        totallyNotImageLinkRegex(node.toWikiText).foreach(file => {
          images += processImageLink(file, node)
          imageCount += 1
        })
      case node =>
        if (depth < recursionDepth) images ++= imageSearch(node.children, depth + 1)
    }
    images
  }

  /**
    * Ensures Encoding and Copyright
    *
    * @param fileName name of an imageFile
    * @param node     Source-Node of the fileName
    * @return Encoded fileName of an Non-Non-Free Image
    */
  private def processImageLink(fileName: String, node: Node): Option[(String, Node)] = {
    // Encoding
    var encodedFileName = fileName
    if (encodedLinkRegex.findFirstIn(fileName).isEmpty)
      encodedFileName = UriUtils.iriDecode(WikiUtil.wikiEncode(fileName))

    val result = if (!context.nonFreeImages.contains(fileName))
      Some((encodedFileName, node))
    else
      None

    // --------------- Special Images ---------------
    val special = totallyNotSpecialImageRegex(encodedFileName)
    if (special == "flag") flagImage += result
    else if (special == "map") mapImage += result
    else if (special == "coa") coatOfArmsImage += result
    else if (special == "signature") signatureImage += result
    if (!mainImageFound) {
      // First Image will be defined as main Image
      mainImage = result
      mainImageFound = true
    }
    result
  }

  /**
    * We needed a faster way to find images in text, than regex
    * @param section Text to search in
    * @return
    */
  def totallyNotImageLinkRegex(section: String): ListBuffer[String] = {
    val charArray = section.reverse.toCharArray
    val iterator = charArray.iterator
    val sb = new mutable.StringBuilder()
    val images = ListBuffer[String]()
    var imageFound = false
    while (iterator.hasNext) {
      var c = iterator.next()
      if (!imageFound) {
        //.svg,.jpe?g,.png
        if (c.toLower == 'g' && iterator.hasNext) {
          c = iterator.next()
          //.svg
          if (c.toLower == 'v' && iterator.hasNext) {
            c = iterator.next()
            if (c.toLower == 's' && iterator.hasNext) {
              c = iterator.next()
              if (c.toLower == '.') {
                sb.append("gvs.")
                imageFound = true
              }
            }
          }
          //.png
          else if (c.toLower == 'n' && iterator.hasNext) {
            c = iterator.next()
            if (c.toLower == 'p' && iterator.hasNext) {
              c = iterator.next()
              if (c.toLower == '.') {
                sb.append("gnp.")
                imageFound = true
              }
            }
          }
          //.jpeg
          else if (c.toLower == 'e' && iterator.hasNext) {
            c = iterator.next()
            if (c.toLower == 'p' && iterator.hasNext) {
              c = iterator.next()
              if (c.toLower == 'j' && iterator.hasNext) {
                c = iterator.next()
                if (c.toLower == '.') {
                  sb.append("gepj.")
                  imageFound = true
                }
              }
            }
          }
          //.jpg
          else if (c.toLower == 'p' && iterator.hasNext) {
            c = iterator.next()
            if (c.toLower == 'j' && iterator.hasNext) {
              c = iterator.next()
              if (c.toLower == '.') {
                sb.append("gpj.")
                imageFound = true
              }
            }
          }
          //.gif
        } else if (c.toLower == 'f' && iterator.hasNext) {
          c = iterator.next()
          if (c.toLower == 'i' && iterator.hasNext) {
            c = iterator.next()
            if (c.toLower == 'g' && iterator.hasNext) {
              c = iterator.next()
              if (c.toLower == '.') {
                sb.append("fig.")
                imageFound = true
              }
            }
          }
        }
        //ImageLink-Content
      } else if (iterator.hasNext) {
        if (c != ':' && c != '=' && c != '|' && c != '\n') {
          sb.append(c)
        }
        // link ended, save string and start new search
        else {
          imageFound = false
          images += sb.reverse.toString()
          sb.clear()
        }
        // String ends => file name does too
      } else {
        sb.append(c)
        imageFound = false
        images += sb.reverse.toString()
        sb.clear()
      }
    }
    images
  }

  /**
    * We needed a faster way of checking a link for key words, than regex.
    *
    * @param link link to search in
    * @return
    */
  def totallyNotSpecialImageRegex(link: String): String = {
    var returnString = ""
    val iterator = link.toCharArray.iterator
    var firstChar = true
    while (iterator.hasNext) {
      var c = iterator.next()
      // map
      if (c.toLower == 'm' && iterator.hasNext && firstChar) {
        c = iterator.next()
        if (c.toLower == 'a' && iterator.hasNext) {
          c = iterator.next()
          if (c.toLower == 'p' && (!iterator.hasNext || (iterator.hasNext && Array(' ', '_', '.').contains(iterator.next())))) {
            returnString = "map"
          }
        }
      }
      // karte
      else if (c.toLower == 'k' && iterator.hasNext && firstChar) {
        c = iterator.next()
        if (c.toLower == 'a' && iterator.hasNext) {
          c = iterator.next()
          if (c.toLower == 'r' && iterator.hasNext) {
            c = iterator.next()
            if (c.toLower == 't' && iterator.hasNext) {
              c = iterator.next()
              if (c.toLower == 'e' && (!iterator.hasNext || (iterator.hasNext && Array(' ', '_', '.').contains(iterator.next())))) {
                returnString = "map"
              }
            }
          }
        }
      }
      // position
      else if (c.toLower == 'p' && iterator.hasNext && firstChar) {
        c = iterator.next()
        if (c.toLower == 'o' && iterator.hasNext) {
          c = iterator.next()
          if (c.toLower == 's' && iterator.hasNext) {
            c = iterator.next()
            if (c.toLower == 'i' && iterator.hasNext) {
              c = iterator.next()
              if (c.toLower == 't' && iterator.hasNext) {
                c = iterator.next()
                if (c.toLower == 'i' && iterator.hasNext) {
                  c = iterator.next()
                  if (c.toLower == 'o' && iterator.hasNext) {
                    c = iterator.next()
                    if (c.toLower == 'n' && (!iterator.hasNext || (iterator.hasNext && Array(' ', '_', '.').contains(iterator.next())))) {
                      returnString = "map"
                    }
                  }
                }
              }
            }
          }
        }
      }
      // carte
      else if (c.toLower == 'c' && iterator.hasNext && firstChar) {
        c = iterator.next()
        if (c.toLower == 'a' && iterator.hasNext) {
          c = iterator.next()
          if (c.toLower == 'r' && iterator.hasNext) {
            c = iterator.next()
            if (c.toLower == 't' && iterator.hasNext) {
              c = iterator.next()
              if (c.toLower == 'e' && (!iterator.hasNext || (iterator.hasNext && Array(' ', '_', '.').contains(iterator.next())))) {
                returnString = "map"
              }
            }
          }
        }
        // coat of arms
        else if (c.toLower == 'o' && iterator.hasNext) {
          c = iterator.next()
          if (c.toLower == 'a' && iterator.hasNext) {
            c = iterator.next()
            if (c.toLower == 't' && iterator.hasNext) {
              c = iterator.next()
              if (c.toLower == '_' && iterator.hasNext) {
                c = iterator.next()
                if (c.toLower == 'o' && iterator.hasNext) {
                  c = iterator.next()
                  if (c.toLower == 'f' && iterator.hasNext) {
                    c = iterator.next()
                    if (c.toLower == '_' && iterator.hasNext) {
                      c = iterator.next()
                      if (c.toLower == 'a' && iterator.hasNext) {
                        c = iterator.next()
                        if (c.toLower == 'r' && iterator.hasNext) {
                          c = iterator.next()
                          if (c.toLower == 'm' && iterator.hasNext) {
                            c = iterator.next()
                            if (c.toLower == 's' && (!iterator.hasNext || (iterator.hasNext && Array(' ', '_', '.').contains(iterator.next())))) {
                              returnString = "coa"
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      // flag
      else if (c.toLower == 'f' && iterator.hasNext && firstChar) {
        c = iterator.next()
        if (c.toLower == 'l' && iterator.hasNext) {
          c = iterator.next()
          if (c.toLower == 'a' && iterator.hasNext) {
            c = iterator.next()
            if (c.toLower == 'g' && (!iterator.hasNext || (iterator.hasNext && Array(' ', '_', '.').contains(iterator.next())))) {
              returnString = "flag"
            }
          }
        }
      }
      // banner
      else if (c.toLower == 'b' && iterator.hasNext && firstChar) {
        c = iterator.next()
        if (c.toLower == 'a' && iterator.hasNext) {
          c = iterator.next()
          if (c.toLower == 'n' && iterator.hasNext) {
            c = iterator.next()
            if (c.toLower == 'n' && iterator.hasNext) {
              c = iterator.next()
              if (c.toLower == 'e' && iterator.hasNext) {
                c = iterator.next()
                if (c.toLower == 'r' && (!iterator.hasNext || (iterator.hasNext && Array(' ', '_', '.').contains(iterator.next())))) {
                  returnString = "flag"
                }
              }
            }
          }
        }
      }
      // signature
      else if (c.toLower == 's' && iterator.hasNext) {
        c = iterator.next()
        if (c.toLower == 'i' && iterator.hasNext) {
          c = iterator.next()
          if (c.toLower == 'g' && iterator.hasNext) {
            c = iterator.next()
            if (c.toLower == 'n' && iterator.hasNext) {
              c = iterator.next()
              if (c.toLower == 'a' && iterator.hasNext) {
                c = iterator.next()
                if (c.toLower == 't' && iterator.hasNext) {
                  c = iterator.next()
                  if (c.toLower == 'u' && iterator.hasNext) {
                    c = iterator.next()
                    if (c.toLower == 'r' && iterator.hasNext) {
                      c = iterator.next()
                      if (c.toLower == 'e' && (!iterator.hasNext || (iterator.hasNext && Array(' ', '_', '.').contains(iterator.next())))) {
                        returnString = "signature"
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
      // toggles a new word, so we don't get occurrences where the keyword is contained in another word.
      firstChar = if (c == ' ' || c == '_') true else false
    }
    returnString
  }

}
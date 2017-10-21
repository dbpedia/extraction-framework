package org.dbpedia.extraction.mappings

import java.net.URLDecoder

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
    val start = System.currentTimeMillis()
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
    imageSearch(node.children, 0).foreach(_ match {
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
    })
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
    println("Time: " + (System.currentTimeMillis() - start) + "ms")
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

    nodes.foreach(node => node match {
      case TextNode(_, _) =>
        // toWikiText is used instead of toPlainText because some images would get lost.
        reverseLinkSearch(node.toWikiText, Seq(".jpg", ".jpeg", ".svg", ".png", ".gif"), Seq(':', '=', '|', '\n')).foreach(file => {
          images += processImageLink(file, node)
          imageCount += 1
        })
      //times += "ImageLinkRegex Finish: " -> System.currentTimeMillis()
      case InternalLinkNode(_, _, _, _) =>
        reverseLinkSearch(node.toWikiText, Seq(".jpg", ".jpeg", ".svg", ".png", ".gif"), Seq(':', '=', '|', '\n')).foreach(file => {
          images += processImageLink(file, node)
          imageCount += 1
        })
      case InterWikiLinkNode(_, _, _, _) =>
        reverseLinkSearch(node.toWikiText, Seq(".jpg", ".jpeg", ".svg", ".png", ".gif"), Seq(':', '=', '|', '\n')).foreach(file => {
          images += processImageLink(file, node)
          imageCount += 1
        })
      case _ =>
        if (depth < recursionDepth) images ++= imageSearch(node.children, depth + 1)
    })
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
    // TODO: Since this part is slow & not even in use yet, we might as well put this in a post processing script
//    ImageExtractorConfig.flagRegex.findFirstIn(encodedFileName).foreach(_ => {
//      flagImage += result
//    })
//    ImageExtractorConfig.mapRegex.findFirstIn(encodedFileName).foreach(_ => {
//      mapImage += result
//    })
//    ImageExtractorConfig.cOARegex.findFirstIn(encodedFileName).foreach(_ => {
//      coatOfArmsImage += result
//    })
//    ImageExtractorConfig.signatureRegex.findFirstIn(encodedFileName).foreach(_ => {
//      signatureImage += result
//    })
//        keywordSearch(encodedFileName, Seq("map", "flag", "coat_of_arms", "signature"), Seq('_', ' ', '\n')) match {
//      case Some(keyWord) =>
//        if(keyWord == "flag")
//          flagImage += result
//        else if(keyWord == "map")
//          mapImage += result
//        else if(keyWord == "coat_of_arms")
//          coatOfArmsImage += result
//        else if(keyWord == "signature")
//          signatureImage += result
//      case None =>
//    }
    if (!mainImageFound) {
      // First Image will be defined as main Image
      mainImage = result
      mainImageFound = true
    }
    result
  }

  /**
    * Find Keywords in text and return the first one found
    * Char by char comparison
    * not really faster than regex
    * => TODO: Still too slow
    * @param text
    * @param keyWords
    * @param delimiter
    * @return
    */
  def keywordSearch(text: String, keyWords : Seq[String], delimiter : Seq[Char]) : Option[String] = {
    // inWord : toggles to true on every new word
    var inWord = true
    // traverse text char by char
    for(i <- 0 to text.length-1){
      if(inWord){
        // check for each keyword
        keyWords.foreach(word => {
          if(word.length <= text.length-i){
            // if one char differs then 'possible' changes to false
            var possible = true
            var k = i
            // traverse the keyword char by char
            for(j <- 0 to word.length-1){
              // only check chars if 'possible' is still true
              if(possible){
                if(text(k) != word(j)){
                  possible = false
                }
                k += 1
              }
            }
            // every char matched => if the word ends here: return word
            if(possible){
              // word ends by reaching end of text
              if(k == text.length){
                return Some(word)
              } else {
                // word ends by delimiter
                if(delimiter.contains(text(k))){
                  return Some(word)
                }
              }
            }
          }
        })
        inWord = false
      }
      // if char is delimiter => new word begins
      else if(delimiter.contains(text(i))){
        inWord = true
      }
    }
    None
  }

  /**
    * A generalized & pretty efficient way of finding links in a text,
    * based on finding extensions through char by char comparison
    * @param text String: input text
    * @param extensions Seq[String]: possible extensions to look out for
    * @param delimiter Seq[Char]: possible delimiters that end the links
    * @return Seq[String]: Links
    */
  def reverseLinkSearch(text: String, extensions : Seq[String], delimiter : Seq[Char]) : Seq[String] = {
    //toggle String Building if we found a link
    var inLink = false
    val sb = new mutable.StringBuilder()
    val links = ListBuffer[String]()

    // reverse traverse the text
    for(i <- (text.length-1) to 0 by -1) {
      // Check every extension
      extensions.foreach(word => {
        if(!inLink) {
          var possible = true
          if (word.length <= i) {
            // k : copy of i for checking the next chars
            var k = i
            // j : index for reverse traversing the extension
            for (j <- word.length - 1 to 0 by -1) {
              // one char is different => don't check the next ones
              if(possible){
                if (text(k) != word(j)) {
                  possible = false
                }
                k -= 1
              }
            }
          } else {
            possible = false
          }
          // every char of the extension was found => we found a link
          if (possible) {
            inLink = true
          }
        }
      })
      // we found a link
      if(inLink){
        // current char is a delimiter => store the link & reset
        if(delimiter.contains(text(i))){
          links += sb.mkString
          sb.clear()
          inLink = false
        }
        // reached end of the text => prepend last char, store the link & reset
        else if(i == 0){
          sb.insert(0, text(i))
          links += sb.mkString
          sb.clear()
          inLink = false
        }
        // inside the link => prepend char
        else {
          sb.insert(0, text(i))
        }
      }
    }
    links
  }

  /*
  (3rd) Generalized link-finder method: Still too slow
  Still here for reference.

  def searchForLinks(text: String, extensions : Seq[String], delimiter : Seq[Char]) : ListBuffer[String] = {
    // arrays to manipulate
    var array = text.reverse
    var kw = extensions.map(_.reverse)

    val links = ListBuffer[String]()
    val sb = new StringBuilder()

    var inLink = false
    while(array.nonEmpty && (kw.nonEmpty || inLink)){
      kw = kw.filter(_.length <= array.length)
      if(!inLink) {
        // test for keywords
        if(kw.filter(_.head == array.head)
          .filter(w => w.last == array(w.length-1))
          .exists(array.startsWith(_))){
          inLink = true
        }
      }
      if(inLink){
        if(delimiter.contains(array.head)){
          // delimiter reached
          inLink = false
          val link = sb.toString()
          links += link
          sb.clear()
        }
        else if(array.tail.isEmpty) {
          // End of String reached
          sb.append(array.head)
          val link = sb.toString()
          links += link
          inLink = false
          sb.clear()
        }
        else {
          // part of the link
          sb.append(array.head)
        }
      }
      // next char
      array = array.tail
    }
    links.map(_.reverse)
  }
  * */

  /*
    [PROOF OF CONCEPT]
    Find words in text, char by char comparison, faster than regex
    not generalized, way too big, not clean, but works

    still here for reference, while this is worked on
    => TODO: Find a better way

    def searchForSpecialImage(link: String): String = {
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
            if (c.toLower == 'p' && (!iterator.hasNext || (iterator.hasNext && (Array(' ', '_', '.').contains(iterator.next()))))) {
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
                if (c.toLower == 'e' && (!iterator.hasNext || (iterator.hasNext && (Array(' ', '_', '.').contains(iterator.next()))))) {
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
                      if (c.toLower == 'n' && (!iterator.hasNext || (iterator.hasNext && (Array(' ', '_', '.').contains(iterator.next()))))) {
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
                if (c.toLower == 'e' && (!iterator.hasNext || (iterator.hasNext && (Array(' ', '_', '.').contains(iterator.next()))))) {
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
                              if (c.toLower == 's' && (!iterator.hasNext || (iterator.hasNext && (Array(' ', '_', '.').contains(iterator.next()))))) {
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
              if (c.toLower == 'g' && (!iterator.hasNext || (iterator.hasNext && (Array(' ', '_', '.').contains(iterator.next()))))) {
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
                  if (c.toLower == 'r' && (!iterator.hasNext || (iterator.hasNext && (Array(' ', '_', '.').contains(iterator.next()))))) {
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
                        if (c.toLower == 'e' && (!iterator.hasNext || (iterator.hasNext && (Array(' ', '_', '.').contains(iterator.next()))))) {
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
  */

}
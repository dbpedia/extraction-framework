package org.dbpedia.extraction.mappings

import java.util.logging.Logger
import org.dbpedia.extraction.destinations.{QuadBuilder, DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{Language, ExtractorUtils}
import scala.language.reflectiveCalls
import org.dbpedia.extraction.util.StringUtils._

/**
 * Extracts image annotations created using the Image Annotator gadget
 * (https://commons.wikimedia.org/wiki/Help:Gadget-ImageAnnotator)
 *
 * This uses the W3C Media Fragments 1.0: http://www.w3.org/TR/2012/REC-media-frags-20120925/ 
 */
class ImageAnnotationExtractor (
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends PageNodeExtractor
{
  private val logger = Logger.getLogger(classOf[ImageAnnotationExtractor].getName)

  /* Properties */
  private val hasAnnotationProperty = context.ontology.properties("hasAnnotation")
  private val descriptionProperty = context.ontology.properties("description")
  private val asWikiTextProperty = context.ontology.properties("asWikiText")

  override val datasets = Set(DBpediaDatasets.ImageAnnotations)

  override def extract(pageNode : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    // Only Commons files please.
    if(context.language != Language.Commons || pageNode.title.namespace != Namespace.File)
        return Seq.empty

    // Get a list of every ImageNote on this page.
    val imageNoteStarts = pageNode.children.filter({
        case tn: TemplateNode => tn.title.decoded.equals("ImageNote") 
        case _ => false
    })

    // Get a list of every ImageNoteEnd on this page.
    val imageNoteEnds = pageNode.children.filter({    
        case tn: TemplateNode => tn.title.decoded.equals("ImageNoteEnd")
        case _ => false
    })

    // Zip the two lists so that we end up with a Seq of
    // corresponding (ImageNote, ImageNoteEnd) pairs.
    val imageNoteQuads = imageNoteStarts.zip(imageNoteEnds).flatMap(pair => { 
        pair match {
            case (noteStart: TemplateNode, noteEnd: TemplateNode) => {
                // Make sure the sequence is ("ImageNote", "ImageNoteEnd"). Otherwise,
                // we've got an off-by-one error somewhere (such as a missing {{ImageNoteEnd}}).
                if(!noteStart.title.decoded.equals("ImageNote"))
                    throw new RuntimeException("Template '" + noteStart.title.decodedWithNamespace + "' found where 'Template:ImageNode' expected; check for missing Template:ImageNode.")

                if(!noteEnd.title.decoded.equals("ImageNoteEnd"))
                    throw new RuntimeException("Template '" + noteEnd.title.decodedWithNamespace + "' found where 'Template:ImageNodeEnd' expected; check for missing Template:ImageNode.")

                // Get the indexes of these TemplateNodes within the pageNode.
                val indexStart = pageNode.children.indexOf(noteStart)
                val indexEnd = pageNode.children.indexOf(noteEnd)

                // Build a URI for a portion of this image.
                // Since all these are required properties, we'll just throw
                // RuntimeExceptions if any of them are missing.
                def getImageNodePropertyIntValue(key: String):Int = {
                    var str = ""
                    try {
                        str = noteStart.property(key).get.children.map(_.toPlainText).mkString("")
                        str.toInt
                    } catch {
                        case ex: NoSuchElementException =>
                            throw new RuntimeException("Template ImageNote missing property '" + key + "' in " + subjectUri)
                        case ex: NumberFormatException =>
                            throw new RuntimeException("Property '" + key + "' has a non-numerical value '" + str + "', only numerical values are accepted.")
                    }
                }

                // Get the x, y, w, h and build a URL for that section of the
                // image using the W3 Media Fragments recommendation at
                // http://www.w3.org/TR/2012/REC-media-frags-20120925/
                val annotation_x = getImageNodePropertyIntValue("x")
                val annotation_y = getImageNodePropertyIntValue("y")
                val annotation_w = getImageNodePropertyIntValue("w")
                val annotation_h = getImageNodePropertyIntValue("h")
                val annotation_url = ExtractorUtils.getFileURL(pageNode.title.encoded, context.language) +
                    s"#xywh=pixel:$annotation_x,$annotation_y,$annotation_w,$annotation_h"

                // Get every node between the start and end template nodes,
                // and convert it into WikiText and plaintext.
                val contents = if(indexEnd - indexStart < 1) Seq.empty
                    else pageNode.children.slice(indexStart + 1, indexEnd)
                val contents_plaintext = contents.map(_.toPlainText).mkString("").trim
                val contents_wikitext = (noteStart +: contents :+ noteEnd).map(_.toWikiText).mkString("").trim

                // Generate a quad to indicate than an annotation exists.
                val quad_annotation = new Quad(
                    context.language,
                    DBpediaDatasets.ImageAnnotations,
                    subjectUri,
                    hasAnnotationProperty,
                    annotation_url,
                    pageNode.sourceUri
                )

                // Generate one quad each for plaintext and wikitext content.
                val quads_contents = Seq(
                    new Quad(
                        context.language, 
                        DBpediaDatasets.ImageAnnotations, 
                        annotation_url, 
                        descriptionProperty, 
                        contents_plaintext, 
                        pageNode.sourceUri
                    ),
                    new Quad(
                        context.language, 
                        DBpediaDatasets.ImageAnnotations, 
                        annotation_url, 
                        asWikiTextProperty, 
                        contents_wikitext, 
                        pageNode.sourceUri
                    )
                )

                quad_annotation +: quads_contents
            }

            case _ => throw new RuntimeException("Pair ('" + pair._1 + "', '" + pair._2 + "') detected unexpectedly.")
        }
    })

    // Return quads
    imageNoteQuads.toSeq
  }
}

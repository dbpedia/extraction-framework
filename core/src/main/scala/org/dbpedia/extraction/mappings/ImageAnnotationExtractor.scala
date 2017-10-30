package org.dbpedia.extraction.mappings

import java.util.logging.Logger

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{ExtractorUtils, Language}
import org.dbpedia.extraction.wikiparser._

import scala.language.reflectiveCalls

/**
 * Extracts image annotations created using the Image Annotator gadget
 * (https://commons.wikimedia.org/wiki/Help:Gadget-ImageAnnotator)
 *
 * The RDF produced uses the W3C Media Fragments 1.0 to identify parts of
 * an image: http://www.w3.org/TR/2012/REC-media-frags-20120925/ 
 */
@SoftwareAgentAnnotation(classOf[ImageAnnotationExtractor], AnnotationType.Extractor)
class ImageAnnotationExtractor (
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends PageNodeExtractor
{
  private val logger = Logger.getLogger(classOf[ImageAnnotationExtractor].getName)
  override val datasets = Set(DBpediaDatasets.ImageAnnotations)

  /* Properties */
  private val hasAnnotationProperty = context.ontology.properties("hasAnnotation")
  private val descriptionProperty = context.ontology.properties("description")
  private val asWikiTextProperty = context.ontology.properties("asWikiText")

  /** Extract image annotations from a particular page */
  override def extract(pageNode : PageNode, subjectUri : String) : Seq[Quad] =
  {
    // Image annotations will only be extracted from File:s on the Commons.
    if (context.language != Language.Commons || pageNode.title.namespace != Namespace.File) {
        return Seq.empty
    }

    // Get a list of every {{ImageNote}} and {{ImageNoteEnd}} TemplateNode on this page.
    // Use of 'collect' suggested at http://ochafik.com/blog/?p=393
    val imageNoteStarts = pageNode.children.collect {
        case tn: TemplateNode if tn.title.decoded.equals("ImageNote") => tn
    }
    val imageNoteEnds = pageNode.children.collect {
        case tn: TemplateNode if tn.title.decoded.equals("ImageNoteEnd") => tn
    }

    // Make sure that every {{ImageNote}} has an {{ImageNoteEnd}}
    if (imageNoteStarts.size != imageNoteEnds.size) {
        throw new RuntimeException("This page has an unequal number of {{ImageNote}} and {{ImageNoteEnd}} templates, skipping.")
    }

    // Zip the two lists so that we end up with a Seq of
    // corresponding (ImageNote, ImageNoteEnd) pairs.
    val imageNoteQuads = imageNoteStarts.zip(imageNoteEnds).flatMap(pair => { 
        pair match {
            case (noteStart: TemplateNode, noteEnd: TemplateNode) => {
                // Make sure the sequence is ("ImageNote", "ImageNoteEnd"). Otherwise,
                // we've got an off-by-one error somewhere (such as a missing {{ImageNoteEnd}}).
                if (!noteStart.title.decoded.equals("ImageNote")) {
                    throw new RuntimeException("Template '" + noteStart.title.decodedWithNamespace + "' found where 'Template:ImageNode' expected; check for missing Template:ImageNode, skipping page.")
                }

                if (!noteEnd.title.decoded.equals("ImageNoteEnd")) {
                    throw new RuntimeException("Template '" + noteEnd.title.decodedWithNamespace + "' found where 'Template:ImageNodeEnd' expected; check for missing Template:ImageNode, skipping page.")
                }

                // Get the indexes of these TemplateNodes within the pageNode.
                val indexStart = pageNode.children.indexOf(noteStart)
                val indexEnd = pageNode.children.indexOf(noteEnd)

                // Build a URI for a portion of this image.
                // Since all these are required properties, we'll just throw
                // RuntimeExceptions if any of them are missing.
                def getImageNodePropertyIntValue(key: String): Int = {
                    var str = ""
                    try {
                        str = noteStart.property(key).get.children.map(_.toPlainText).mkString("")
                        str.toInt
                    } catch {
                        case ex: NoSuchElementException =>
                            throw new RuntimeException("Template ImageNote missing property '" + key + "', skipping page.")
                        case ex: NumberFormatException =>
                            throw new RuntimeException("Property '" + key + "' has a non-numerical value '" + str + "', only numerical values are accepted, skipping page.")
                    }
                }

                // Get the x, y, w, h and build a URL for that section of the
                // image using the W3 Media Fragments recommendation at
                // http://www.w3.org/TR/2012/REC-media-frags-20120925/
                val annotation_x = getImageNodePropertyIntValue("x")
                val annotation_y = getImageNodePropertyIntValue("y")
                val annotation_w = getImageNodePropertyIntValue("w")
                val annotation_h = getImageNodePropertyIntValue("h")
                val annotation_url = try {
                    val annotation_dimx = getImageNodePropertyIntValue("dimx")
                    val annotation_dimy = getImageNodePropertyIntValue("dimy")
                    
                    ExtractorUtils.getFileURL(pageNode.title.encoded, context.language) +
                        s"?width=$annotation_dimx&height=$annotation_dimy#xywh=pixel:$annotation_x,$annotation_y,$annotation_w,$annotation_h"
                } catch {
                    case e: RuntimeException => ExtractorUtils.getFileURL(pageNode.title.encoded, context.language) +
                        s"#xywh=pixel:$annotation_x,$annotation_y,$annotation_w,$annotation_h"
                }

                // Get every node between the start and end template nodes,
                // and convert it into WikiText and plaintext.
                //
                // Since we use dbo:asWikiText, we add the {{ImageNote}} and
                // {{ImageNoteEnd}} template so that it contains the entire
                // WikiText representation of this annotation.
                val contents = if (indexEnd - indexStart < 1) Seq.empty
                    else pageNode.children.slice(indexStart + 1, indexEnd)
                val contents_plaintext = contents.map(_.toPlainText).mkString("").trim
                val contents_wikitext = contents.map(_.toWikiText).mkString("").trim

                // Generate a quad to link the annotation to the original file.
                val quads_annotation = Seq(new Quad(
                    context.language,
                    DBpediaDatasets.ImageAnnotations,
                    subjectUri,
                    hasAnnotationProperty,
                    annotation_url,
                    pageNode.sourceIri
                ))

                // Generate one quad each for plaintext and WikiText content.
                val quads_plaintext = if(contents_plaintext == "") Seq.empty else Seq(
                    new Quad(
                        context.language, 
                        DBpediaDatasets.ImageAnnotations, 
                        annotation_url, 
                        descriptionProperty, 
                        contents_plaintext, 
                        pageNode.sourceIri
                    )
                )

                val quads_wikitext = if(contents_wikitext == "") Seq.empty else Seq(
                    new Quad(
                        context.language, 
                        DBpediaDatasets.ImageAnnotations, 
                        annotation_url, 
                        asWikiTextProperty, 
                        contents_wikitext, 
                        pageNode.sourceIri
                    )
                )

                // Combine quads and return them
                quads_annotation ++ quads_plaintext ++ quads_wikitext
            }

            // If we don't have (TemplateNode, TemplateNode) ... eh, what?
            case _ => throw new RuntimeException("Pair ('" + pair._1 + "', '" + pair._2 + "') detected unexpectedly.")
        }
    })

    // Return all quads
    imageNoteQuads.toSeq
  }
}

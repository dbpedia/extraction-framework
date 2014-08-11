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
 */
class ImageAnnotationExtractor (
  context : {
    def ontology : Ontology
    def language : Language
  }
)
extends PageNodeExtractor
{
  private val descriptionProperty = context.ontology.properties("description")
  private val logger = Logger.getLogger(classOf[ImageAnnotationExtractor].getName)

  override val datasets = Set(DBpediaDatasets.ImageAnnotations)

  override def extract(pageNode : PageNode, subjectUri : String, pageContext : PageContext) : Seq[Quad] =
  {
    // Only Commons files please.
    if(context.language != Language.Commons || pageNode.title.namespace != Namespace.File)
        return Seq.empty

    // TODO: extend this to include templates inside templates?
    // Not if none of the other extractors does this.

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

    // Zip the two lists so that we end up with a datastructure of
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

                // Get every node between the start and end template nodes,
                // and convert it into WikiText and plaintext.
                val contents = if(indexEnd - indexStart < 1) Seq.empty
                    else pageNode.children.slice(indexStart + 1, indexEnd)
                val contents_plaintext = contents.map(_.toPlainText).mkString(" ")
                val contents_wikitext = contents.map(_.toWikiText).mkString(" ")

                // TODO: generate a quad to indicate than an annotation exists.

                // Generate one quad each for plaintext and wikitext content.
                val quads_contents = Seq(
                    new Quad(
                        context.language, 
                        DBpediaDatasets.ImageAnnotations, 
                        subjectUri, 
                        descriptionProperty, 
                        contents_plaintext, 
                        pageNode.sourceUri
                    ),
                    new Quad(
                        context.language, 
                        DBpediaDatasets.ImageAnnotations, 
                        subjectUri, 
                        descriptionProperty, 
                        contents_wikitext, 
                        pageNode.sourceUri
                    )
                )

                quads_contents
            }

            case _ => throw new RuntimeException("Pair ('" + pair._1 + "', '" + pair._2 + "') detected unexpectedly.")
        }
    })

    // Return quads
    imageNoteQuads.toSeq
  }
}

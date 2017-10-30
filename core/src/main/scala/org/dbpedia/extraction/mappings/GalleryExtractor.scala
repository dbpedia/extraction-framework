package org.dbpedia.extraction.mappings

import java.util.logging.Logger

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language

import scala.language.reflectiveCalls

/**
 * Extract images from galleries. I'm not sure what the best RDF representation
 * of this will be, but for now we'll start with:
 *
 *  - <Main:Gallery page> <dbo:galleryItem> <File:Image>
 *
 * The gallery tag is documented at https://en.wikipedia.org/wiki/Help:Gallery_tag
 */
@SoftwareAgentAnnotation(classOf[GalleryExtractor], AnnotationType.Extractor)
class GalleryExtractor (
  context: {
    def ontology: Ontology
    def language: Language
  }
)
extends WikiPageExtractor
{
    // Logger.
    private val logger = Logger.getLogger(classOf[GalleryExtractor].getName)

    /** Property that links a gallery page with each image on it */
    private val galleryItemProperty = context.ontology.properties("galleryItem")

    override val datasets = Set(DBpediaDatasets.ImageGalleries)

    /*
     * Regular expressions
     */
    /** Check for gallery tags in the page source. */
    val galleryRegex = new scala.util.matching.Regex("""<gallery((?s).*?)>((?s).+?)</gallery>""",
        "tags",
        "files"
    )

    /**
     * I developed the "does this look like a title" logic 
     * from https://www.mediawiki.org/wiki/Manual:Page_title
     */
    val fileLineRegex = new scala.util.matching.Regex("""^([^#<>\[\]\|\{\}]+)(|.*)?$""",
        "filename",
        "tags"
    )

    /**
     * Extract gallery tags from a WikiPage.
     */
    override def extract(page: WikiPage, subjectUri: String): Seq[Quad] = {
        // Iterate over each <gallery> set.
        val galleryQuads = galleryRegex.findAllMatchIn(page.source).flatMap(matchData => {
            // Figure out the line number by counting the newlines until the
            // start of the gallery tag.
            val lineNumber = page.source.substring(0, matchData.start).count(_ == '\n')

            val tags = matchData.group("tags")
            val fileLines = matchData.group("files")

            // Quads generated from the file lines.
            val fileLineQuads = fileLines.split('\n').flatMap({

                // Some Gallery lines might be empty.
                case "" => Seq.empty

                // Other lines have names of files.
                case fileLine =>
                    val fileLineOption = fileLineRegex.findFirstMatchIn(fileLine)

                    // If the regular expression doesn't match, ignore it:
                    // it probably won't be read correctly by MediaWiki either.
                    if (fileLineOption.isEmpty)
                        Seq.empty
                    else {
                        val fileLineMatch = fileLineOption.get

                        val sourceWithLineNumber = page.sourceIri + "#absolute-line=" +
                          lineNumber

                        try {
                            // Parse the filename in the file line match.
                            val fileWikiTitle = WikiTitle.parse(
                                fileLineMatch.group("filename"),
                                context.language
                            )

                            // Generate <subjectUri> <dbo:galleryItem> <imageUri>
                            Seq(new Quad(Language.English, DBpediaDatasets.ImageGalleries,
                                subjectUri,
                                galleryItemProperty,
                                context.language.resourceUri.append(fileWikiTitle.decodedWithNamespace),
                                //                                fileWikiTitle.pageIri,
                                sourceWithLineNumber,
                                null
                            ))
                        } catch {
                            case e: WikiParserException =>
                                // If there's a WikiParserException, report the
                                // error and keep going.
                                logger.warning("Could not parse file line '" +
                                  fileLineMatch.group("filename") +
                                  "' in gallery on page '" + subjectUri + "': " + e.getMessage
                                )

                                // Just skip this line and keep going.
                                Seq.empty
                        }
                    }
            })

            fileLineQuads.toSeq
        })

        galleryQuads.toSeq
    }
}

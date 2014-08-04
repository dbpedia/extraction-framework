package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets,Quad,QuadBuilder}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import scala.language.reflectiveCalls
import org.dbpedia.extraction.sources.WikiPage

/**
 * Extract images from galleries. I'm not sure what the best RDF representation
 * of this will be, but for now we'll start with:
 *
 *  - <Main:Gallery page> <foaf:depiction> <File:Image>
 *
 * The gallery tag is documented at https://en.wikipedia.org/wiki/Help:Gallery_tag
 */
class GalleryExtractor (
  context: {
    def ontology: Ontology
    def language: Language
  }
)
extends WikiPageExtractor
{
    private val foafDepictionProperty = context.ontology.properties("foaf:depiction")

    override val datasets = Set(DBpediaDatasets.Images)

    /*
     * Regular expressions
     */
    // Check for gallery tags in the page source.
    val galleryRegex = new scala.util.matching.Regex("""<gallery((?s).*?)>((?s).+?)</gallery>""",
        "tags",
        "files"
    )

    // The structure of a [[File:...]] link.
    // I developed the "does this look like a title" logic from https://www.mediawiki.org/wiki/Manual:Page_title
    val fileLineRegex = new scala.util.matching.Regex("""^([^#<>\[\]\|\{\}]+)(|.*)?$""",
        "filename",
        "tags"
    )

    /**
     * Extract gallery tags from a WikiPage.
     */
    override def extract(page: WikiPage, subjectUri: String, pageContext: PageContext): Seq[Quad] = {
        // Iterate over each <gallery> set.
        val galleryQuads = galleryRegex.findAllMatchIn(page.source).flatMap(matchData => {
            val lineNumber = page.source.substring(0, matchData.start).count(_ == '\n')
            val tags = matchData.group("tags")
            val fileLines = matchData.group("files")

            val fileLineQuads = fileLines.split('\n').flatMap({
                // Some Gallery lines might just be empty.
                case "" => Seq.empty

                // Other lines have names of files.
                case fileLine => {
                    val fileLineOption = fileLineRegex.findFirstMatchIn(fileLine) 

                    if (fileLineOption.isEmpty) 
                        Seq.empty
                    else {
                        val fileLineMatch = fileLineOption.get
                        
                        Seq(new Quad(Language.English, DBpediaDatasets.Images,
                            subjectUri,
                            foafDepictionProperty,
                            WikiTitle.parse(fileLineMatch.group("filename"), context.language).pageIri,
                            page.sourceUri + "#absolute-line=" + lineNumber,
                            null
                        ))
                    }
                }
            })

            fileLineQuads.toSeq
        })

        galleryQuads.toSeq
    }
}

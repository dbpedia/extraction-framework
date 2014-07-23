package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.destinations.{DBpediaDatasets,Quad,QuadBuilder}
import org.dbpedia.extraction.wikiparser.LinkNode
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

  override def extract(page: WikiPage, subjectUri: String, pageContext: PageContext): Seq[Quad] = {
    // Check for gallery tags in the page source.
    val galleryRegex = new scala.util.matching.Regex("""<gallery(.*?)>(.+?)</gallery>""",
        "tags",
        "files"
    )
    val galleryMatch = galleryRegex.findAllIn(page.source)

    val galleryQuads = if(!galleryMatch.isEmpty) {
        val files = galleryMatch.group("files").split('\n')

        // TODO: parse the line, figure out the link and the label
        // and build InternalLinkNodes, then turn those into Quads.
    }

    Seq.empty
  }
}

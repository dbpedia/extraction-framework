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

  override def extract(page: WikiPage, subjectUri: String, pageContext: PageContext): Seq[Quad] = {
    // Check for gallery tags in the page source.
    val galleryRegex = new scala.util.matching.Regex("""<gallery((?s).*?)>((?s).+?)</gallery>""",
        "tags",
        "files"
    )
    val galleryMatch = galleryRegex.findAllIn(page.source)

    if(galleryMatch.isEmpty) return Seq.empty

    val files = galleryMatch.group("files").split('\n')

    // TODO: figure out page numbers.
    // TODO: parse each line into a WikiTitle and a label.
    // See parseLink(...) in SimpleWikiParser to see how this is done.
    files.flatMap(filename => filename match {
        case "" => Seq.empty
        case filename:String => Seq(new Quad(Language.English,
            DBpediaDatasets.Images,
            subjectUri,
            foafDepictionProperty,
            WikiTitle.parse(filename, context.language).pageIri,
            page.sourceUri,
            null
        ))
    })
  }
}

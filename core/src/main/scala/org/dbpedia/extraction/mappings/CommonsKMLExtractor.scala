package org.dbpedia.extraction.mappings

import java.util.logging.Logger
import org.dbpedia.extraction.destinations.{DBpediaDatasets,Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.sources.WikiPage
import scala.language.reflectiveCalls

/**
 * Extract KML files from the Commons and links them to the original document.
 * There are only 160 KML files on the Commons right now, but some day there
 * might be more.
 *
 * These are currently used as overlays, documented at
 * https://commons.wikimedia.org/wiki/Commons:Geocoding/Overlay 
 */
class CommonsKMLExtractor (
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
    private val hasKMLDataProperty = context.ontology.properties("hasKMLData")

    override val datasets = Set(DBpediaDatasets.KMLFiles)

    /** Check for gallery tags in the page source. 
     * Note that this won't match to the end of the string, only the end of the
     * first <source></source> 
     */
    val sourceRegex = new scala.util.matching.Regex("""(?s)<source\s+lang=["']xml["']>\s*(.*?)\s*</source>""",
        "kml_content"
    )

    /**
     * Extract a WikiPage that consists of KML.
     */
    override def extract(page: WikiPage, subjectUri: String, pageContext: PageContext): Seq[Quad] = {
        // This only applies to Commons files with a .kml extension.
        if(context.language != Language.Commons || !page.title.decoded.toLowerCase.endsWith(".kml"))
            return Seq.empty

        // Take out the initial '<source lang="xml">' and final '</source>' if present.
        val kmlContentQuads = sourceRegex.findAllMatchIn(page.source).map(result => new Quad(
            Language.English, DBpediaDatasets.KMLFiles,
            subjectUri,
            hasKMLDataProperty,
            result.group("kml_content"),
            page.sourceUri,
            new Datatype("rdf:XMLLiteral")
        ))

        kmlContentQuads.toSeq
    }
}

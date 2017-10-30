package org.dbpedia.extraction.mappings

import java.util.logging.Logger

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.WikiPage

import scala.language.reflectiveCalls

/**
 * Extract KML files from the Commons and links them to the original document.
 * There are only 160 KML files on the Commons right now, but some day there
 * might be more.
 *
 * These are currently used as overlays, documented at
 * https://commons.wikimedia.org/wiki/Commons:Geocoding/Overlay 
 */
@SoftwareAgentAnnotation(classOf[CommonsKMLExtractor], AnnotationType.Extractor)
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
    override def extract(page: WikiPage, subjectUri: String): Seq[Quad] = {
        // This extractor only applies to Commons file named '.*/overlay.kml'.
        if (context.language != Language.Commons || !page.title.decoded.toLowerCase.endsWith("/overlay.kml")) {
            return Seq.empty
        }

        // The overlay.kml page is an overlay on the actual image, so the
        // subjectUri should be modified to point there. Since we already know it
        // ends with '/overlay.kml', we can just take out the last 12 characters.
        val subjectUriWithoutOverlay = subjectUri.substring(0, subjectUri.length - 12)

        // Take out the initial '<source lang="xml">' and final '</source>' if present
        // and add the XML to the output as rdf:XMLLiterals.
        val kmlContentQuads = sourceRegex.findAllMatchIn(page.source).map(result => new Quad(
            Language.English, DBpediaDatasets.KMLFiles,
            subjectUriWithoutOverlay,
            hasKMLDataProperty,
            result.group("kml_content"),
            page.sourceIri,
            new Datatype("rdf:XMLLiteral")
        ))

        kmlContentQuads.toSeq
    }
}

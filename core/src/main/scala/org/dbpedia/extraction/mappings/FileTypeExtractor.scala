package org.dbpedia.extraction.mappings

import java.util.logging.Logger
import org.dbpedia.extraction.config.mappings.FileTypeExtractorConfig
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.util.{Language, ExtractorUtils}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces
import scala.language.reflectiveCalls

/**
 * Identifies the type of a File page.
 */
class FileTypeExtractor(context: { 
    def ontology: Ontology
    def language : Language
}) extends WikiPageExtractor
{
    // For writing warnings about unexpected files.
    private val logger = Logger.getLogger(classOf[FileTypeExtractor].getName)

    // Properties we use.
    private val fileExtensionProperty = context.ontology.properties("fileExtension")
    private val rdfTypeProperty = context.ontology.properties("rdf:type")
    private val dcTypeProperty = context.ontology.properties("dct:type")
    private val dcFormatProperty = context.ontology.properties("dct:format")
    private val foafDepictionProperty = context.ontology.properties("foaf:depiction")
    private val foafThumbnailProperty = context.ontology.properties("foaf:thumbnail")

    // Classes we use.
    private val dboFileClass = context.ontology.classes("File")
    
    // All data will be written out to DBpediaDatasets.FileInformation.
    override val datasets = Set(DBpediaDatasets.FileInformation)
    
    /**
     * Extract a single WikiPage. We guess the file type from the file extension
     * used by the page.
     */
    override def extract(page: WikiPage, subjectUri: String, pageContext: PageContext) : Seq[Quad] =
    {
        // This extraction only works on File:s.
        if(page.title.namespace != Namespace.File || page.redirect != null) 
            return Seq.empty

        // Store the depiction and thumbnail for this image.
        val (imageURL, thumbnailURL) = ExtractorUtils.getImageURL(
            Language.Commons,
            page.title.encoded
        )
        val image_url_quads = Seq(
             new Quad(Language.English, 
                DBpediaDatasets.FileInformation,
                subjectUri,
                foafDepictionProperty,
                imageURL,
                page.sourceUri,
                null
            ), new Quad(Language.English, 
                DBpediaDatasets.FileInformation,
                imageURL,
                foafThumbnailProperty,
                thumbnailURL,
                page.sourceUri,
                null
            )
        )

        // Extract an extension.
        val extensionRegex = new scala.util.matching.Regex("""\.(\w+)$""", "extension")
        val extensionMatch = extensionRegex.findAllIn(page.title.decoded)

        // If there is a match, create the appropriate file_type_quads.
        val file_type_quads = if(extensionMatch.isEmpty) Seq.empty else { 
            val extension = extensionMatch.group("extension").toLowerCase

            // Warn the user on long extensions.
            if(extension.length > 4)
                logger.warning("Page '" + page.title.decodedWithNamespace + "' has an unusually long extension '" + extension + "'")
 
            // This will return a type class (as an OntologyClass) and a mimeType.
            val (fileTypeClass, mimeType) = FileTypeExtractorConfig.typeAndMimeType(context.ontology, extension)

            // Generate the quads. 
            //
            // This file has <rdf:type> <dbo:File> (and all related classes) and
            // <rdf:type> fileTypeClass and all related class. First, let's build
            // a Set of all these classes.
            val rdfTypeClasses = 
                (dboFileClass.relatedClasses ++ fileTypeClass.relatedClasses).toSet

            // Then, map each OntologyClass to a Quad with an <rdf:type>.
            val rdfTypeQuads = rdfTypeClasses.map(rdfClass => 
                new Quad(Language.English, 
                    DBpediaDatasets.FileInformation,
                    subjectUri,
                    rdfTypeProperty,
                    rdfClass.uri,
                    page.sourceUri,
                    null
                )).toSeq

            // To this list, we need to add three other file type quads here:
            rdfTypeQuads ++ Seq(
                // 1. file <dbo:fileExtension> "ext"^^xsd:string
                new Quad(Language.English, DBpediaDatasets.FileInformation,
                    subjectUri,
                    fileExtensionProperty,
                    extension,
                    page.sourceUri,
                    context.ontology.datatypes("xsd:string")
                ), 
                // 2. file <dc:type> fileTypeClass
                new Quad(Language.English, DBpediaDatasets.FileInformation,
                    subjectUri,
                    dcTypeProperty,
                    fileTypeClass.uri,
                    page.sourceUri,
                    null
                ), 
                // 3. file <dc:format> "mimeType"^^xsd:string
                new Quad(Language.English, DBpediaDatasets.FileInformation,
                    subjectUri,
                    dcFormatProperty,
                    mimeType,
                    page.sourceUri,
                    context.ontology.datatypes("xsd:string")
                )
            )
        }

        // Combine and return all the generated quads.
        image_url_quads ++ file_type_quads
    }
}

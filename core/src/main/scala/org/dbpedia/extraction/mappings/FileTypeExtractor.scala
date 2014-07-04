package org.dbpedia.extraction.mappings

import java.util.logging.Logger
import org.dbpedia.extraction.config.mappings.FileTypeExtractorConfig
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.util.{Language, ExtractorUtils}
import org.dbpedia.extraction.wikiparser._
import scala.language.reflectiveCalls

/**
 * Identifies the type of a File page.
 */
class FileTypeExtractor(context: {
    def ontology: Ontology
    def language : Language
}) extends WikiPageExtractor
{
    // For writing warnings.
    private val logger = Logger.getLogger(classOf[FileTypeExtractor].getName)

    // To store the Commons language.
    private val commonsLang = Language.Commons

    // Ontology.
    private val ontology = context.ontology

    // RDF datatypes we use.
    private val xsdString = context.ontology.datatypes("xsd:string")

    // RDF properties we use.
    private val fileExtensionProperty = context.ontology.properties("fileExtension")
    private val rdfTypeProperty = context.ontology.properties("rdf:type")
    private val dcTypeProperty = context.ontology.properties("dct:type")
    private val dcFormatProperty = context.ontology.properties("dct:format")
    private val dboSourceProperty = context.ontology.properties("source")
    private val dboThumbnailProperty = context.ontology.properties("thumbnail")
    private val foafDepictionProperty = context.ontology.properties("foaf:depiction")
    private val foafThumbnailProperty = context.ontology.properties("foaf:thumbnail")

    // RDF classes we use.
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

        // Generate the depiction and thumbnail for this image.
        val image_url_quads = generateImageURLQuads(page, subjectUri)

        // Attempt to identify the extension, then use that to generate
        // file-type quads.
        val file_type_quads = extractExtension(page) match {
            case None => Seq.empty
            case Some(extension) => generateFileTypeQuads(extension, page, subjectUri)
        }

        // Combine and return all the generated quads.
        image_url_quads ++ file_type_quads
    }

    /**
     * Generate Quads for the image files pointed to by this image, including:
     *  <resource> foaf:depiction <image>
     *  <image> foaf:thumbnail <image-thumbnail>
     */
    def generateImageURLQuads(page: WikiPage, subjectUri: String): Seq[Quad] =
    {
        // Get the image and thumbnail URLs.
        val (imageURL, thumbnailURL) = ExtractorUtils.getFileURLWithThumbnail(
            commonsLang,
            page.title.encoded
        )

        Seq(
            // 1. <resource> source <image>
            new Quad(Language.English,
                DBpediaDatasets.FileInformation,
                subjectUri,
                dboSourceProperty,
                imageURL,
                page.sourceUri,
                null
            ),
            // 2. <resource> foaf:depiction <image>
            new Quad(Language.English,
                DBpediaDatasets.FileInformation,
                subjectUri,
                foafDepictionProperty,
                imageURL,
                page.sourceUri,
                null
            ), 
            // 3. <resource> thumbnail <image>
            new Quad(Language.English,
                DBpediaDatasets.FileInformation,
                subjectUri,
                dboThumbnailProperty,
                thumbnailURL,
                page.sourceUri,
                null
            ),
            // 4. <image> foaf:thumbnail <image>
            new Quad(Language.English,
                DBpediaDatasets.FileInformation,
                imageURL,
                foafThumbnailProperty,
                thumbnailURL,
                page.sourceUri,
                null
            )
        )
    }

    /**
     * Determine the extension of a WikiPage.
     * @returns None if no extension exists, Some[String] if an extension was found.
     */
    def extractExtension(page: WikiPage): Option[String] =
    {
        // Extract an extension.
        val extensionRegex = new scala.util.matching.Regex("""\.(\w+)$""", "extension")
        val extensionMatch = extensionRegex.findAllIn(page.title.decoded)

        // If there is no match, bail out.
        if(extensionMatch.isEmpty) return None
        val extension = extensionMatch.group("extension").toLowerCase

        // Warn the user on long extensions.
        val page_title = page.title.decodedWithNamespace
        if(extension.length > 4)
            logger.warning(f"Page '$page_title%s' has an unusually long extension '$extension%s'")

        Some(extension)
    }

    /**
     * Generate quads that describe the file types for an extension.
     *  <resource> dbo:fileExtension "extension"^^xsd:string
     *  <resource> 
     *  <resource> dc:type dct:StillImage
     *  <resource> rdf:type dbo:File
     *  <resource> rdf:type dbo:Document
     *  <resource> rdf:type dbo:Image
     *  <resource> rdf:type dbo:StillImage
     */
    def generateFileTypeQuads(extension: String, page: WikiPage, subjectUri: String):Seq[Quad] = {
        // 1. <resource> dbo:fileExtension "extension"^^xsd:string
        val file_extension_quad = new Quad(
            Language.English, DBpediaDatasets.FileInformation,
            subjectUri,
            fileExtensionProperty,
            extension,
            page.sourceUri,
            xsdString
        )

        // 2. Figure out the file type and MIME type.
        val (fileTypeClass, mimeType) = FileTypeExtractorConfig.typeAndMimeType(ontology, extension)

        // 3. <resource> dc:type fileTypeClass
        val file_type_quad = new Quad(
            Language.English, DBpediaDatasets.FileInformation,
            subjectUri,
            dcTypeProperty,
            fileTypeClass.uri,
            page.sourceUri,
            null
        )
            
        // 4. <resource> dc:format "mimeType"^^xsd:string
        val mime_type_quad = new Quad(
            Language.English, DBpediaDatasets.FileInformation,
            subjectUri,
            dcFormatProperty,
            mimeType,
            page.sourceUri,
            xsdString
        )

        // 5. For fileTypeClass and dbo:File, add all related classes.
        val relatedRDFClasses = (dboFileClass.relatedClasses ++ fileTypeClass.relatedClasses).toSet
        val rdf_type_from_related_quads = relatedRDFClasses.map(rdfClass =>
            new Quad(Language.English,
                DBpediaDatasets.FileInformation,
                subjectUri,
                rdfTypeProperty,
                rdfClass.uri,
                page.sourceUri,
                null
            )
        )

        // Return all quads.
        Seq(file_extension_quad, file_type_quad, mime_type_quad) ++ 
            rdf_type_from_related_quads.toSeq
    }
}

package org.dbpedia.extraction.mappings

import java.util.logging.Logger

import org.dbpedia.extraction.annotations.{AnnotationType, SoftwareAgentAnnotation}
import org.dbpedia.extraction.config.mappings.FileTypeExtractorConfig
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.{ExtractorUtils, Language}
import org.dbpedia.extraction.wikiparser._

import scala.language.reflectiveCalls

/**
 * Identifies the type of a File page.
 */
@SoftwareAgentAnnotation(classOf[FileTypeExtractor], AnnotationType.Extractor)
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
    private val dctTypeProperty = context.ontology.properties("dct:type")
    private val dctFormatProperty = context.ontology.properties("dct:format")
    private val dboFileURLProperty = context.ontology.properties("fileURL")
    private val dboThumbnailProperty = context.ontology.properties("thumbnail")
    private val foafDepictionProperty = context.ontology.properties("foaf:depiction")
    private val foafThumbnailProperty = context.ontology.properties("foaf:thumbnail")

    // RDF classes we use.
    private val dboFile = context.ontology.classes("File")
    private val dboStillImage = context.ontology.classes("StillImage")

    // All data will be written out to DBpediaDatasets.FileInformation.
    override val datasets = Set(DBpediaDatasets.FileInformation, DBpediaDatasets.OntologyTypes, DBpediaDatasets.OntologyTypesTransitive)

    /**
     * Extract a single WikiPage. We guess the file type from the file extension
     * used by the page.
     */
    override def extract(page: WikiPage, subjectUri: String) : Seq[Quad] =
    {
        // This extraction only works on File:s.
        if(page.title.namespace != Namespace.File || page.isRedirect)
            return Seq.empty

        // Generate the fileURL.
        val fileURL = ExtractorUtils.getFileURL(page.title.encoded, commonsLang)
        val file_url_quads = Seq(
            // <resource> dbo:fileURL <url>
            new Quad(Language.English,
                DBpediaDatasets.FileInformation,
                subjectUri,
                dboFileURLProperty,
                fileURL,
                page.sourceIri,
                null
            )
        )

        // Attempt to identify the extension, then use that to generate
        // file-type quads.
        val file_type_quads = extractExtension(page) match {
            case None => Seq.empty
            case Some(extension) => generateFileTypeQuads(extension, page, subjectUri)
        }

        // Combine and return all the generated quads.
        file_url_quads ++ file_type_quads
    }

    /**
     * Generate Quads for the StillImage pointed to by this file, including:
     *  <resource> foaf:depiction <file>
     *  <file> foaf:thumbnail <image-thumbnail>
     */
    def generateImageURLQuads(page: WikiPage, subjectUri: String): Seq[Quad] =
    {
        // Get the file and thumbnail URLs.
        val fileURL = ExtractorUtils.getFileURL(page.title.encoded, commonsLang)
        val thumbnailURL = ExtractorUtils.getThumbnailURL(page.title.encoded, commonsLang)

        Seq(
            // 1. <resource> foaf:depiction <image>
            new Quad(Language.English,
                DBpediaDatasets.FileInformation,
                subjectUri,
                foafDepictionProperty,
                fileURL,
                page.sourceIri,
                null
            ), 
            // 2. <resource> thumbnail <image>
            new Quad(Language.English,
                DBpediaDatasets.FileInformation,
                subjectUri,
                dboThumbnailProperty,
                thumbnailURL,
                page.sourceIri,
                null
            ),
            // 3. <image> foaf:thumbnail <image>
            new Quad(Language.English,
                DBpediaDatasets.FileInformation,
                fileURL,
                foafThumbnailProperty,
                thumbnailURL,
                page.sourceIri,
                null
            )
        )
    }

    /**
     * Determine the extension of a WikiPage.
 *
     * @return None if no extension exists, Some[String] if an extension was found.
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
     *  <resource> dct:type dct:StillImage
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
            page.sourceIri,
            xsdString
        )

        // 2. Figure out the file type and MIME type.
        val (fileTypeClass, mimeType) = FileTypeExtractorConfig.typeAndMimeType(ontology, extension)

        // 3. StillImages have a depiction.
        val depiction_and_thumbnail_quads = if(fileTypeClass != dboStillImage) Seq.empty
            else generateImageURLQuads(page, subjectUri)

        // 4. <resource> dct:type fileTypeClass
        val file_type_quad = new Quad(
            Language.English, DBpediaDatasets.FileInformation,
            subjectUri,
            dctTypeProperty,
            fileTypeClass.uri,
            page.sourceIri,
            null
        )
            
        // 5. <resource> dct:format "mimeType"^^xsd:string
        val mime_type_quad = new Quad(
            Language.English, DBpediaDatasets.FileInformation,
            subjectUri,
            dctFormatProperty,
            mimeType,
            page.sourceIri,
            xsdString
        )

        // 6. Add dboFile as direct type
        val rdf_type_direct = new Quad(
          Language.English,
          DBpediaDatasets.OntologyTypes,
          subjectUri,
          rdfTypeProperty,
          dboFile.uri,
          page.sourceIri,
          null
        )

        // 7. For fileTypeClass and dbo:File, add all related classes.
        val relatedRDFClasses = (dboFile.relatedClasses ++ fileTypeClass.relatedClasses).toSet.filter( _ != dboFile) // remove direct type
        val rdf_type_from_related_quads = relatedRDFClasses.map(rdfClass =>
            new Quad(Language.English,
                DBpediaDatasets.OntologyTypesTransitive,
                subjectUri,
                rdfTypeProperty,
                rdfClass.uri,
                page.sourceIri,
                null
            )
        )

        // Return all quads.
        Seq(file_extension_quad, file_type_quad, mime_type_quad, rdf_type_direct) ++
            depiction_and_thumbnail_quads ++
            rdf_type_from_related_quads.toSeq
    }
}

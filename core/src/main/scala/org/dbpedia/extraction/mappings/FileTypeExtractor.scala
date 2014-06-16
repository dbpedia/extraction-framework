package org.dbpedia.extraction.mappings

import java.util.logging.Logger
import org.dbpedia.extraction.config.mappings.FileTypeExtractorConfig
import org.dbpedia.extraction.destinations.{DBpediaDatasets, Quad}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.sources.WikiPage
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces
import scala.language.reflectiveCalls

/**
 * Identifies the type used by a File.
 */
class FileTypeExtractor(context: { 
    def ontology: Ontology
    def language : Language
}) extends WikiPageExtractor
{
    private val logger = Logger.getLogger(classOf[FileTypeExtractor].getName)
    private val fileExtensionProperty = context.ontology.properties("fileExtension")
    private val dcTypeProperty = context.ontology.properties("dct:type")
    private val dcFormatProperty = context.ontology.properties("dct:format")
    
    override val datasets = Set(DBpediaDatasets.FileInformation)
    
    override def extract(page: WikiPage, subjectUri: String, pageContext: PageContext) : Seq[Quad] =
    {
        // This interface only applies to File:s.
        if(page.title.namespace != Namespace.File || page.redirect != null) 
            return Seq.empty

        // Add a quad for the file type as guessed from the extension.
        val extensionRegex = new scala.util.matching.Regex("""\.(\w+)$""", "extension")
        val extensionMatch = extensionRegex.findAllIn(page.title.decoded)

        val file_type_quads = if(extensionMatch.isEmpty) Seq.empty else {
            val extension = extensionMatch.group("extension").toLowerCase

            if(extension.length > 4)
                logger.warning("Page '" + page.title.decodedWithNamespace + "' has an unusually long extension '" + extension + "'")
 
            val (fileType, mimeType) = FileTypeExtractorConfig.typeAndMimeType(extension)

            Seq(new Quad(Language.English, DBpediaDatasets.FileInformation,
                subjectUri,
                fileExtensionProperty,
                extension,
                page.sourceUri,
                context.ontology.datatypes("xsd:string")
            ), new Quad(Language.English, DBpediaDatasets.FileInformation,
                subjectUri,
                dcTypeProperty,
                fileType,
                page.sourceUri,
                null // fileType should be a URL
            ), new Quad(Language.English, DBpediaDatasets.FileInformation,
                subjectUri,
                dcFormatProperty,
                mimeType,
                page.sourceUri,
                context.ontology.datatypes("xsd:string")
            ))
        }

        return Seq(file_type_quads).flatten
    }
}

package org.dbpedia.extraction.config.mappings

import org.dbpedia.extraction.ontology.{Ontology, OntologyClass}

/**
 * Configuration for the FileTypeExtractor.
 */
object FileTypeExtractorConfig
{
    /**
     * Returns the assumed OntologyClass and stringified MIME-type for a
     * given file extension.
     */
    def typeAndMimeType(ontology: Ontology, extension: String):(OntologyClass, String) = {
        // Find the entry in mimeTypeFromFileExtension that contains the
        // extension as a key in the second-level Map.
        mimeTypeFromFileExtension.find(
            pair => pair._2.contains(extension.toLowerCase)
        ) match {
            // If a match is found, return the results as an OntologyClass
            // with the MIME-type as a String.
            case Some(result) => (
                ontology.classes(result._1), 
                result._2.getOrElse(
                    extension.toLowerCase,
                    throw new RuntimeException("This should not be reachable")
                )
            )

            // If the extension could not be found, treat it as an unknown file.
            case None => (ontology.classes("owl:Thing"), "application/octet-stream")
        }
    }

    /**
     * For each File class in the ontology, stores a list of possible
     * extensions and MIME-types used by it.
     */
    private val mimeTypeFromFileExtension = Map(
        "StillImage" -> Map(
            "jpg" -> "image/jpeg",
            "jpeg" -> "image/jpeg",
            "png" -> "image/png",
            "svg" -> "image/svg+xml",
            "pdf" -> "application/pdf",
            "gif" -> "image/gif",
            "tif" -> "image/tiff",
            "tiff" -> "image/tiff",
            "djvu" -> "image/vnd.djvu",
            "xcf" -> "image/xcf",
            "kml" -> "application/vnd.google-earth.kml+xml"
        ),
        "MovingImage" -> Map(
            "ogv" -> "video/ogg",
            "webm" -> "video/webm"
        ),
        "Sound" -> Map(
            "oga" -> "audio/ogg",
            // "webm" -> already defined above
            "mid" -> "application/x-midi",
            "flac" -> "audio/x-flac",
            "wav" -> "audio/vnd.wave"
        ),
        "Software" -> Map(
            "js" -> "application/javascript"
        )
    )
}

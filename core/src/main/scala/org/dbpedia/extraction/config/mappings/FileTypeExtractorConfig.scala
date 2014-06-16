package org.dbpedia.extraction.config.mappings


object FileTypeExtractorConfig
{
    def typeAndMimeType(extension: String):(String, String) = {
        mimeTypeFromFileExtension.find(
            pair => pair._2.contains(extension.toLowerCase)
        ) match {
            case Some(result) => (result._1, 
                result._2.getOrElse(
                    extension.toLowerCase,
                    throw new RuntimeException("This should not be reachable")
                )
            )

            // If the extension could be found, treat it as an unknown file.
            case None => ("owl:Thing", "application/octet-stream")
        }
    }

    // TODO: replace outer key with a proper type class that can
    // serialize itself into quads.
    private val mimeTypeFromFileExtension = Map(
        "http://purl.org/dc/dcmitype/StillImage" -> Map(
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
        "http://purl.org/dc/dcmitype/MovingImage" -> Map(
            "ogv" -> "video/ogg",
            "webm" -> "video/webm"
        ),
        "http://purl.org/dc/dcmitype/Sound" -> Map(
            "oga" -> "audio/ogg",
            // "webm" -> already defined above
            "mid" -> "application/x-midi",
            "flac" -> "audio/x-flac",
            "wav" -> "audio/vnd.wave"
        ),
        "http://purl.org/dc/dcmitype/Software" -> Map(
            "js" -> "application/javascript"
        )
    )
}

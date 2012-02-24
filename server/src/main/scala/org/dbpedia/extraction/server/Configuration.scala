package org.dbpedia.extraction.server

import _root_.org.dbpedia.extraction.util.Language
import java.net.URL
import org.dbpedia.extraction.mappings._

class Configuration
{
    /** The extraction languages */
    val languages = Set("ar", "bn", "ca", "cs", "de", "el", "en", "es", "fr", "ga", "hi", "hr", "hu", "it", "ko", "nl", "pl", "pt", "ru", "sl", "tr").flatMap(Language.fromWikiCode(_))

    /** The extractors */
    val extractors = List(
        classOf[LabelExtractor].asInstanceOf[Class[Extractor]],
        classOf[MappingExtractor].asInstanceOf[Class[Extractor]]
    )

    /**
     * The extraction manager
     * DynamicExtractionManager is able to update the ontology/mappings.
     * StaticExtractionManager is NOT able to update the ontology/mappings.
     */
    val extractionManager = new DynamicExtractionManager(languages, extractors)  // new StaticExtractionManager(languages, extractors)

    /** The URL where the pages of the Mappings Wiki are located */
    val wikiPagesUrl = new URL("http://mappings.dbpedia.org/index.php")

    /** The URL of the MediaWiki API of the Mappings Wiki */
    val wikiApiUrl = new URL("http://mappings.dbpedia.org/api.php")
}

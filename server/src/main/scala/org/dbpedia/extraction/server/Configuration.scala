package org.dbpedia.extraction.server

import _root_.org.dbpedia.extraction.util.Language
import java.net.URL
import org.dbpedia.extraction.mappings._

object Configuration
{
    /** The extraction languages. Use List (not Set) to preserve order. English first, rest sorted. */
    val languages = List("en", "ar", "bn", "ca", "cs", "de", "el", "es", "eu", "fr", "ga", "hi", "hr", "hu", "it", "ko", "nl", "pl", "pt", "ru", "sl", "tr")
      .map(Language.forCode)

    /** The extractors */
    val extractors = List(
        classOf[LabelExtractor],
        classOf[MappingExtractor]
    )

    /** The URL where the pages of the Mappings Wiki are located */
    val wikiPagesUrl = new URL("http://mappings.dbpedia.org/index.php")

    /** The URL of the MediaWiki API of the Mappings Wiki */
    val wikiApiUrl = new URL("http://mappings.dbpedia.org/api.php")
    
}

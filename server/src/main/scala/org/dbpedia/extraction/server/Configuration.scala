package org.dbpedia.extraction.server

import _root_.org.dbpedia.extraction.util.Language
import java.net.URL

class Configuration
{
    /** The extraction languages */
    val languages = Set("en", "de", "fr", "pl", "it", "es", "pt", "nl", "ca", "hu", "sl", "hr", "el", "ga").flatMap(Language.fromWikiCode(_))

    /** The URL where the pages of the Mappings Wiki are located */
    val wikiPagesUrl = new URL("http://mappings.dbpedia.org/index.php")

    /** The URL of the MediaWiki API of the Mappings Wiki */
    val wikiApiUrl = new URL("http://mappings.dbpedia.org/api.php")
}

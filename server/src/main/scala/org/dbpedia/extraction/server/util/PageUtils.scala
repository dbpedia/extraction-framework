package org.dbpedia.extraction.server.util

import scala.xml.Elem
import org.dbpedia.extraction.wikiparser.PageNode

object PageUtils
{
    /**
     * Generates a relative link from the title of the given page. A colon in the title is escaped,
     * otherwise the browser would interpret the namespace as a protocol.
     */
    def relativeLink( page : PageNode ) : Elem =
    {
        <a href={page.title.encodedWithNamespace.replace(":", "%3A")}>{page.title.decodedWithNamespace}</a>
    }
}
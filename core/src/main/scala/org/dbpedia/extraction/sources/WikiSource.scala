package org.dbpedia.extraction.sources

import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{WikiTitle,Namespace}
import java.net.URL
import org.dbpedia.extraction.util.WikiApi

/**
 * Fetches pages from a MediaWiki.
 *
 * @param url The URL of the MediaWiki API e.g. http://en.wikipedia.org/w/api.php
 * @param language The language of the MediaWiki
 * @param namespace The namespaces to fetch articles from
 */
object WikiSource
{
    /**
     * Fetches all pages from a list of titles.
     *
     * @param title The titles of the pages
     * @param url The URL of the MediaWiki API e.g. http://en.wikipedia.org/w/api.php
     * @param language The language of the MediaWiki
     */
    def fromTitles(titles: Traversable[WikiTitle], url: URL, language: Language): Source =
    new Source
    {
        private val api = new WikiApi(url, language)

        override def foreach[U](f : WikiPage => U) : Unit =
        {
            api.retrievePagesByTitle(titles).foreach(f)
        }

        override def hasDefiniteSize = true
    }

    /**
     * Source of all pages which belong to a specific namespace.
     *
     * @param namespace The namespaces to fetch articles from
     * @param url The URL of the MediaWiki API e.g. http://en.wikipedia.org/w/api.php
     * @param language The language of the MediaWiki
     */
    def fromNamespaces(namespaces: Set[Namespace], url: URL, language: Language): Source =
    new Source
    {
        private val api = new WikiApi(url, language)

        override def foreach[U](f : WikiPage => U) : Unit =
        {
            for(namespace <- namespaces)
            {
                api.retrievePagesByNamespace(namespace, f)
            }
        }

        override def hasDefiniteSize = true
    }
}
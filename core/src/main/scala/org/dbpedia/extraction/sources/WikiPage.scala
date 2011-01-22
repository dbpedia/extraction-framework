package org.dbpedia.extraction.sources

import org.dbpedia.extraction.wikiparser.WikiTitle

/**
 * Represents a wiki page
 *
 * @param title The title of this page
 * @param id The page ID
 * @param revision The revision of this page
 * @param source The WikiText source of this page
 */
case class WikiPage(val title : WikiTitle, val id : Long, val revision : Long, val source : String)
{
    override def toString = "WikiPage(" + title + "," + id + "," + revision + "," + source + ")"

    /**
     * Serializes this page to XML using the MediaWiki export format.
     * The MediaWiki export format is specified at http://www.mediawiki.org/xml/export-0.4.
     */
    def toXML =
    {
        <mediawiki xmlns="http://www.mediawiki.org/xml/export-0.4/"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.mediawiki.org/xml/export-0.4/ http://www.mediawiki.org/xml/export-0.4.xsd"
                   version="0.4"
                   xml:lang="en">
          <page>
            <title>{title.decodedWithNamespace}</title>
            <id>{id}</id>
            <revision>
              <id>{revision}</id>
              <text xml:space="preserve">{source}</text>
            </revision>
          </page>
        </mediawiki>
    }
}

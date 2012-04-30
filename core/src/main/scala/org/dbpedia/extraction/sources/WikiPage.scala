package org.dbpedia.extraction.sources

import org.dbpedia.extraction.wikiparser.WikiTitle

/**
 * Represents a wiki page
 *
 * TODO: use redirect id to check redirect extractor. Or get rid of redirect extractor.
 * 
 * @param title The title of this page
 * @param id The page ID
 * @param revision The revision of this page
 * @param source The WikiText source of this page
 */
case class WikiPage(val title : WikiTitle, val redirect : WikiTitle, val id : Long, val revision : Long, val timestamp: String, val source : String)
{
    override def toString = "WikiPage(" + title + "," + id + "," + revision + "," + source + ")"

    /**
     * Serializes this page to XML using the MediaWiki export format.
     * The MediaWiki export format is specified at http://www.mediawiki.org/xml/export-0.4.
     */
    def toXML =
    {
        <mediawiki xmlns="http://www.mediawiki.org/xml/export-0.6/"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.mediawiki.org/xml/export-0.6/ http://www.mediawiki.org/xml/export-0.6.xsd"
                   version="0.6"
                   xml:lang={title.language.isoCode}>
          <page>
            <title>{title.decodedWithNamespace}</title>
            <id>{id}</id>
            <revision>
              <id>{revision}</id>
              <timestamp>{timestamp}</timestamp>
              <text xml:space="preserve">{source}</text>
            </revision>
          </page>
        </mediawiki>
    }
}

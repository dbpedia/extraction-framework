package org.dbpedia.extraction.wikiparser

/**
 * Represents a page.
 * 
 * @param title The title of this page
 * @param id The page ID
 * @param revision The revision of this page
 * @param isRedirect True, if this is a Redirect page
 * @param isDisambiguation True, if this is a Disambiguation page
 * @param children The contents of this page
 */
class PageNode(val title : WikiTitle, val id : Long, val revision : Long, val timestamp: String, val isRedirect: Boolean, val isDisambiguation : Boolean,
                    override val children : List[Node] = List.empty) extends Node(children, 0)
{
    def toWikiText() : String = children.map(_.toWikiText).mkString

    // FIXME: copy and paste from WikiPage.scala
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
              <text xml:space="preserve">{toWikiText()}</text>
            </revision>
          </page>
        </mediawiki>
    }
}

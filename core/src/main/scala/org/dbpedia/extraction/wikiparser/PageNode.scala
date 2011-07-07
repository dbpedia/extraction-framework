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
case class PageNode(title : WikiTitle, id : Long, revision : Long, isRedirect : Boolean, isDisambiguation : Boolean,
                    override val children : List[Node] = List.empty) extends Node(children, 0)
{
    def toWikiText() : String = children.map(_.toWikiText()).mkString("")

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
              <text xml:space="preserve">{toWikiText()}</text>
            </revision>
          </page>
        </mediawiki>
    }
}

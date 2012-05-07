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
case class PageNode(title : WikiTitle, id : Long, revision : Long, timestamp: String, isRedirect: Boolean, isDisambiguation : Boolean,
                    override val children: List[Node] = List.empty) extends Node(children, 0)
{
    def toWikiText() : String = children.map(_.toWikiText).mkString

    // FIXME: copy and paste from WikiPage.scala
    // TODO: this is the XML format used in the dump files, but the format used by api.php is different.
    // We should move this method to a utility class that also has a method generating XML in api.php format.
    // TODO: make sure that XML is valid according to the schema. If not, add dummy elements / attributes where required.
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

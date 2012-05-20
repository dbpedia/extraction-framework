package org.dbpedia.extraction.server.util

import org.dbpedia.extraction.server.Server
import scala.xml.Elem
import org.dbpedia.extraction.wikiparser.PageNode
import org.dbpedia.extraction.util.Language

object PageUtils
{
  /**
   * Generates a relative link from the title of the given page. A colon in the title is escaped,
   * otherwise the browser would interpret the namespace as a protocol.
   */
  def relativeLink(page: PageNode): Elem =
  {
      <a href={page.title.encodedWithNamespace.replace(":", "%3A")}>{page.title.decodedWithNamespace}</a>
  }
  
  def languageList(title: String, header: String, prefix: String): Elem = {
    // we need toSeq here to keep languages ordered.
    val links = Server.instance.managers.keys.toSeq.map(lang => (lang.wikiCode+"/", prefix + " " + lang.wikiCode))
    linkList(title: String, header: String, links)
  }
  
  /**
   * @param links url -> text
   */
  def linkList(title: String, header: String, links: Seq[(String, String)]): Elem = {
    <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en">
    <head>
      <title>{title}</title>
      <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    </head>
    <body>
      <h2>{header}</h2>
      {
        for((url, text) <- links) yield
        {
          <p><a href={url}>{text}</a></p>
        }
      }
    </body>
    </html>
  }
}
package org.dbpedia.extraction.sources

/**
 * Created with IntelliJ IDEA.
 * User: andread
 * Date: 08/04/13
 * Time: 12:37
 * To change this template use File | Settings | File Templates.
 */
object WikiPageFormat extends Enumeration {
  type WikiPageFormat = Value
  val WikiText = Value("WikiText")
  val Json = Value("Json")

  def mimeToWikiPageFormat(mimeType : String) : WikiPageFormat = {
    mimeType match {
      case "application/json" => Json
      case "text/x-wiki" => WikiText
      case _ => WikiText
    }
  }
}
package org.dbpedia.extraction.dump.extract

import org.dbpedia.extraction.mappings.Extractor
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Namespace, WikiPage, WikiTitle}

import scala.util.Try
import scala.xml.XML.loadString

object SerializableUtils extends Serializable {
  //@transient lazy val logger = LogManager.getRootLogger

  /**
    * Extracts Quads from a WikiPage
    * @param namespaces Set of wanted namespaces
    * @param extractor WikiPage Extractor
    * @param page WikiPage
    * @return Quad collection
    */
  def extractQuadsFromPage(namespaces: Set[Namespace], extractor: Extractor[WikiPage], page: WikiPage): Seq[Quad] = {
    var quads = Seq[Quad]()
    try {
      if (namespaces.exists(_.equals(page.title.namespace))) {
        //Extract Quads
        quads = extractor.extract(page, page.uri)
      }
    }
    catch {
      case ex: Exception =>
        page.addExtractionRecord(null, ex)
        page.getExtractionRecords()
    }
    quads
  }

  /**
    * Parses a xml string to a wikipage.
    * based on org.dbpedia.extraction.sources.XMLSource
    * @param xmlString xml wiki page
    * @return Option[WikiPage]
    */
  def parseXMLToWikiPage(xmlString: String, language: Language): Option[WikiPage] = {
    /*
     * Handle special cases occurring in the extraction
     */
    var validXML = xmlString
    val length = xmlString.length
    if(xmlString.startsWith("<page><mediawiki")){
      return None
    }
    if(xmlString.charAt(length-12) == '<'){
      validXML = xmlString.dropRight(12)
    }

    /* ------- Parse XML & Build WikiPage -------
     * based on org.dbpedia.extraction.sources.XMLSource
     */
    try {
      val page = loadString(validXML)
      val revision = page \ "revision"
      val title = WikiTitle.parseCleanTitle((page \ "title").text, language,
        Try { new java.lang.Long(java.lang.Long.parseLong((page \ "id").text))}.toOption)
      val namespaceElement = page \ "ns"
      if (namespaceElement.nonEmpty) {
        try {
          val namespaceCode = namespaceElement.text.toInt
          require(title.namespace.code == namespaceCode,
            s"XML Namespace ($namespaceCode) does not match the computed namespace (${title.namespace}) in page: ${title.decodedWithNamespace}"
          )
        }
        catch {
          case e: NumberFormatException => throw new IllegalArgumentException("Cannot parse content of element [ns] as int", e)
        }
      }
      //Skip bad titles
      if (title != null) {
        val _redirect = (page \ "redirect" \ "@title").text match {
          case "" => null
          case t => WikiTitle.parse(t, language)
        }
        val _contributorID = (revision \ "contributor" \ "id").text match {
          case null => "0"
          case id => id
        }
        Some(new WikiPage(
          title     = title,
          redirect  = _redirect,
          id        = (page \ "id").text,
          revision  = (revision \ "id").text,
          timestamp = (revision \ "timestamp").text,
          contributorID   = _contributorID,
          contributorName =
            if (_contributorID == "0") (revision \ "contributor" \ "ip").text
            else (revision \ "contributor" \ "username").text,
          source    = (revision \ "text").text,
          format    = (revision \ "format").text))
      }
      else None
    } catch {
      case ex: Throwable =>
        //logger.warn("error parsing xml:" + ex.getMessage)
        None
    }
  }
}

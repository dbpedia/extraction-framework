package org.dbpedia.extraction.dump.extract

import org.apache.log4j.LogManager
import org.dbpedia.extraction.mappings.Extractor
import org.dbpedia.extraction.transform.Quad
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{Namespace, WikiPage, WikiTitle}

import scala.collection.mutable
import scala.util.Try
import scala.xml.XML.loadString

object SerializableUtils extends Serializable {
  @transient lazy val logger = LogManager.getRootLogger

  /**
    * Extracts Quads from a WikiPage
    * checks for desired namespace
    * deduplication
    * @param namespaces Set of correct namespaces
    * @param extractor Wikipage Extractor
    * @param page WikiPage
    * @return Deduplicated Colection of Quads
    */
  def processPage(namespaces: Set[Namespace], extractor: Extractor[WikiPage], page: WikiPage): Seq[Quad] = {
    //LinkedHashSet for deduplication
    val uniqueQuads = new mutable.LinkedHashSet[Quad]()
    try {
      if (namespaces.exists(_.equals(page.title.namespace))) {
        //Extract Quads
        uniqueQuads ++= extractor.extract(page, page.uri)
      }
    }
    catch {
      case ex: Exception =>
        logger.error("error while processing page " + page.title.decoded, ex)
        page.addExtractionRecord(null, ex)
        page.getExtractionRecords()
    }
    uniqueQuads.toSeq
  }

  /**
    * Parses a xml string to a wikipage.
    * @param xmlString xml
    * @return WikiPage Option
    */
  def xmlToWikiPage(xmlString: String, language: Language): Option[WikiPage] = {
    /* ------- Special Cases -------
     * for normal pages:  add <page> in front
     * for last page:     cut </mediawiki>
     * first input:       skip
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
      val rev = page \ "revision"
      val title = WikiTitle.parseCleanTitle((page \ "title").text, language,
        Try { new java.lang.Long(java.lang.Long.parseLong((page \ "id").text))}.toOption)
      val nsElem = page \ "ns"
      if (nsElem.nonEmpty) {
        try {
          val nsCode = nsElem.text.toInt
          require(title.namespace.code == nsCode, "XML Namespace (" + nsCode + ") does not match the computed namespace (" + title.namespace + ") in page: " + title.decodedWithNamespace)
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
        val _contributorID = (rev \ "contributor" \ "id").text match {
          case null => "0"
          case id => id
        }
        Some(new WikiPage(
          title     = title,
          redirect  = _redirect,
          id        = (page \ "id").text,
          revision  = (rev \ "id").text,
          timestamp = (rev \ "timestamp").text,
          contributorID   = _contributorID,
          contributorName =
            if (_contributorID == "0") (rev \ "contributor" \ "ip").text
            else (rev \ "contributor" \ "username").text,
          source    = (rev \ "text").text,
          format    = (rev \ "format").text))
      }
      else None
    } catch {
      case ex: Throwable =>
        logger.warn("error parsing xml:" + ex.getMessage)
        None
    }
  }
}

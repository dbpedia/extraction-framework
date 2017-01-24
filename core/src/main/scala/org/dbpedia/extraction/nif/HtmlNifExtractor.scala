package org.dbpedia.extraction.nif

import java.net.URI

import org.apache.commons.lang3.StringEscapeUtils
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.nif.LinkExtractor.NifExtractorContext
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.util.{Config, UriUtils, WikiUtil}
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, TextNode}
import org.jsoup.select.NodeTraversor

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}
import scala.collection.convert.decorateAsScala._
/**
  * Created by Chile on 1/19/2017.
  */
abstract class HtmlNifExtractor(nifContextIri: String, language: String, configFile : Config) {

  assert(nifContextIri.contains("?"), "the nifContextIri needs a query part!")

  protected val writeLinkAnchors = configFile.nifParameters.writeLinkAnchor
  protected val writeStrings = configFile.nifParameters.writeAnchor
  protected val removeElementsCssQueries = configFile.nifParameters.removeElements
  protected val replaceElementsCssQueries = configFile.nifParameters.replaceElements

  protected lazy val nifContext = QuadBuilder.dynamicPredicate(language, DBpediaDatasets.NifContext.encoded) _
  protected lazy val nifStructure = QuadBuilder.dynamicPredicate(language, DBpediaDatasets.NifPageStructure.encoded) _
  protected lazy val nifLinks = QuadBuilder.dynamicPredicate(language, DBpediaDatasets.NifTextLinks.encoded) _
  protected lazy val rawTables = QuadBuilder.dynamicPredicate(language, DBpediaDatasets.RawTables.encoded) _

  protected val templateString = "Template"

  /**
    * Extract the relevant html page divided in sections and paragraphs
    * @param html - the html string downloaded from the destination resource
    * @return -  a collection of section objects
    */
  def getRelevantParagraphs (html: String): Seq[PageSection]

  /**
    *
    * @param graphIri
    * @param subjectIri
    * @param html
    * @param exceptionHandle
    * @return
    */
  def extractNif(graphIri: String, subjectIri: String, html: String)(exceptionHandle: Throwable => Unit): Seq[Quad] = {

    val sections = getRelevantParagraphs(html)

    var context = ""
    var length = 0
    val quads = for(section <- sections) yield {
      extractTextFromHtml(section, new NifExtractorContext(language, subjectIri, templateString)) match {
        case Success(extractionResults) => {
          context = context + (if (context.length > 0) " " else "") + extractionResults.text
          val quad = makeStructureElements(section, extractionResults.paragraphs, nifContextIri, graphIri, length, extractionResults.length)
          length = length + (if (context.length > 0) 1 else 0) + extractionResults.length

          //collect additional triples
          extendSectionTriples(extractionResults, graphIri, subjectIri)
          //forward exceptions
          extractionResults.errors.foreach(err => exceptionHandle(new Exception(err)))
          quad
        }
        case Failure(e) => {
          exceptionHandle(e)
          List()
        }
      }
    }

    quads.flatten ++ makeContext(context, nifContextIri, graphIri, length) ++ extendContextTriples(quads.flatten, graphIri, subjectIri)
  }

  /**
    * Each extracted section can be further enriched by this function, by providing additional quads
    * @param extractionResults - the extraction results for a particular section
    * @return
    */
  def extendSectionTriples(extractionResults: TempHtmlExtractionResults, graphIri: String, subjectIri: String): Seq[Quad]

  /**
    * For each page additional triples can be added with this function
    * @param quads - the current collection of quads
    * @return
    */
  def extendContextTriples(quads: Seq[Quad], graphIri: String, subjectIri: String): Seq[Quad]

  private def makeContext(text: String, contextUri: String, sourceUrl: String, contextEnd: Int): ListBuffer[Quad] = {
    var cont = ListBuffer[Quad]()
    val wikipediaUrl = if(sourceUrl.contains("?")) sourceUrl.substring(0, sourceUrl.indexOf('?')) else sourceUrl
    if (contextEnd == 0)
      return ListBuffer()
    cont += nifContext(contextUri, RdfNamespace.RDF.append("type"), RdfNamespace.NIF.append("Context"), sourceUrl, null)
    cont += nifContext(contextUri, RdfNamespace.NIF.append("beginIndex"), "0", sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger") )
    cont += nifContext(contextUri, RdfNamespace.NIF.append("endIndex"), contextEnd.toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger") )
    cont += nifContext(contextUri, RdfNamespace.NIF.append("sourceUrl"), wikipediaUrl, sourceUrl, null)
    cont += nifContext(contextUri, RdfNamespace.NIF.append("isString"), text, sourceUrl, RdfNamespace.XSD.append("string"))
    cont
  }

  private def makeStructureElements(section: PageSection, paragraphs: List[Paragraph], contextUri: String, sourceUrl: String, offset: Int, length: Int): ListBuffer[Quad] = {
    var triples = ListBuffer[Quad]()
    val sectionUri = section.getUri
    triples += nifStructure(sectionUri, RdfNamespace.RDF.append("type"), RdfNamespace.NIF.append("Section"), sourceUrl, null)
    triples += nifStructure(sectionUri, RdfNamespace.SKOS.append("notation"), section.ref, sourceUrl, RdfNamespace.RDFS.append("Literal"))
    triples += nifStructure(sectionUri, RdfNamespace.NIF.append("beginIndex"), offset.toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
    triples += nifStructure(sectionUri, RdfNamespace.NIF.append("endIndex"), (offset + length).toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
    triples += nifStructure(sectionUri, RdfNamespace.NIF.append("referenceContext"), contextUri, sourceUrl, null)
    triples += nifStructure(contextUri, RdfNamespace.NIF.append("hasSection"), sectionUri, sourceUrl, null)

    //adding navigational properties
    section.prev match{
      case Some(p) => triples += nifStructure(sectionUri, RdfNamespace.NIF.append("previousSection"), p.getUri, sourceUrl, null)
      case None =>
    }
    section.sub match{
      case Some(p) => triples += nifStructure(sectionUri, RdfNamespace.NIF.append("subSection"), p.getUri, sourceUrl, null)
      case None =>
    }
    section.top match{
      case Some(p) => {
        triples += nifStructure(sectionUri, RdfNamespace.NIF.append("topSection"), p.getUri, sourceUrl, null)
        triples += nifStructure(sectionUri, RdfNamespace.NIF.append("superString"), p.getUri, sourceUrl, null)
      }
      case None =>
    }
    section.next match{
      case Some(p) => triples += nifStructure(sectionUri, RdfNamespace.NIF.append("nextSection"), p.getUri, sourceUrl, null)
      case None =>
    }

    if(section.top.isEmpty && section.prev.isEmpty) {
      triples += nifStructure(contextUri, RdfNamespace.NIF.append("firstSection"), sectionUri, sourceUrl, null)
      triples += nifStructure(sectionUri, RdfNamespace.NIF.append("superString"), contextUri, sourceUrl, null)
    }
    if(section.sub.isEmpty && section.next.isEmpty)
      triples += nifStructure(contextUri, RdfNamespace.NIF.append("lastSection"), sectionUri, sourceUrl, null)

    //add titles
    triples ++= createSectionTitle(section, contextUri, sourceUrl, offset)

    //further specifying paragraphs of every section
    var lastParagraph: String = null

    for(i <- paragraphs.indices) {
      val pt = paragraphs(i).getText.trim
      if(pt.length > 0 && pt != section.title.trim)                            //if titles were extracted as paragraphs
        writeParagraph(i)
    }

    def writeParagraph(i: Int): Unit = {
      if(paragraphs(i).getLength == 0)
        return
      val paragraph = getNifIri("paragraph", paragraphs(i).getBegin(offset).toString, paragraphs(i).getEnd(offset).toString)

      if (lastParagraph != null) //provide the nextParagraph triple
        triples += nifStructure(lastParagraph, RdfNamespace.NIF.append("nextParagraph"), paragraph, sourceUrl, null)

      triples += nifStructure(paragraph, RdfNamespace.RDF.append("type"), RdfNamespace.NIF.append("Paragraph"), sourceUrl, null)
      triples += nifStructure(paragraph, RdfNamespace.NIF.append("beginIndex"), paragraphs(i).getBegin(offset).toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
      triples += nifStructure(paragraph, RdfNamespace.NIF.append("endIndex"), paragraphs(i).getEnd(offset).toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
      triples += nifStructure(paragraph, RdfNamespace.NIF.append("referenceContext"), contextUri, sourceUrl, null)
      triples += nifStructure(paragraph, RdfNamespace.NIF.append("superString"), sectionUri, sourceUrl, null)
      if (writeStrings)
        triples += nifStructure(paragraph, RdfNamespace.NIF.append("anchorOf"), paragraphs(i).getText, sourceUrl, RdfNamespace.XSD.append("string"))
      triples += nifStructure(sectionUri, RdfNamespace.NIF.append("hasParagraph"), paragraph, sourceUrl, null)
      if (i == 0)
        triples += nifStructure(sectionUri, RdfNamespace.NIF.append("firstParagraph"), paragraph, sourceUrl, null)
      if (i == paragraphs.indices.last)
        triples += nifStructure(sectionUri, RdfNamespace.NIF.append("lastParagraph"), paragraph, sourceUrl, null)

      lastParagraph = paragraph

      triples ++= makeWordsFromLinks(paragraphs(i).getLinks.asScala.toList, contextUri, paragraph, sourceUrl, offset)
      triples ++= saveRawTables(paragraphs(i).getTableHtml.asScala, section, contextUri, sourceUrl, offset)
    }

    triples
  }

  private def saveRawTables(tables: mutable.Map[Integer, String], section: PageSection, contextUri: String, sourceUrl: String, offset: Int): ListBuffer[Quad] = {
    val triples = ListBuffer[Quad]()
    for(table <- tables.toList){
      val tableUri = getNifIri("table", section.ref, section.tableCount.toString)
      section.tableCount = section.tableCount+1

      triples += rawTables(tableUri, RdfNamespace.RDF.append("type"), RdfNamespace.NIF.append("Structure"), sourceUrl, null)
      triples += rawTables(tableUri, RdfNamespace.NIF.append("referenceContext"), contextUri, sourceUrl, null)
      triples += rawTables(tableUri, RdfNamespace.NIF.append("beginIndex"), (offset + table._1).toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
      triples += rawTables(tableUri, RdfNamespace.NIF.append("endIndex"), (offset + table._1).toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
      triples += rawTables(tableUri, RdfNamespace.DC.append("source"), table._2, sourceUrl, RdfNamespace.RDF.append("XMLLiteral"))
    }
    triples
  }

  private def createSectionTitle(section: PageSection, contextUri: String, sourceUrl: String, offset: Int): ListBuffer[Quad] = {
    val triples = ListBuffer[Quad]()
    if(section.title == "abstract")
      return triples                //the abstract has no title

    val tableUri = getNifIri("title", offset.toString, (offset + section.title.length).toString)
    section.tableCount = section.tableCount+1

    triples += nifStructure(tableUri, RdfNamespace.RDF.append("type"), RdfNamespace.NIF.append("Title"), sourceUrl, null)
    triples += nifStructure(tableUri, RdfNamespace.NIF.append("referenceContext"), contextUri, sourceUrl, null)
    triples += nifStructure(tableUri, RdfNamespace.NIF.append("beginIndex"), offset.toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
    triples += nifStructure(tableUri, RdfNamespace.NIF.append("endIndex"), (offset + section.title.length).toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
    triples += nifStructure(tableUri, RdfNamespace.NIF.append("superString"), section.getUri, sourceUrl, null)
    if(writeLinkAnchors)
      triples += nifLinks(tableUri, RdfNamespace.NIF.append("anchorOf"), section.title, sourceUrl, RdfNamespace.XSD.append("string"))
    triples
  }

  private def makeWordsFromLinks(links: List[Link], contextUri: String, paragraphUri: String, sourceUrl: String, offset: Int): ListBuffer[Quad] = {
    var words = ListBuffer[Quad]()
    for (link <- links) {
      if (link.getWordEnd - link.getWordStart > 0) {
        val typ = if (link.getLinkText.split(" ").length > 1) "Phrase" else "Word"
        val word = getNifIri(typ.toString.toLowerCase, (offset+link.getWordStart).toString, (offset + link.getWordEnd).toString)
        words += nifLinks(word, RdfNamespace.RDF.append("type"), RdfNamespace.NIF.append(typ), sourceUrl, null)
        words += nifLinks(word, RdfNamespace.NIF.append("referenceContext"), contextUri, sourceUrl, null)
        words += nifLinks(word, RdfNamespace.NIF.append("beginIndex"), (offset + link.getWordStart).toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
        words += nifLinks(word, RdfNamespace.NIF.append("endIndex"), (offset + link.getWordEnd).toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
        words += nifLinks(word, RdfNamespace.NIF.append("superString"), paragraphUri, sourceUrl, null)
        words += nifLinks(word, "http://www.w3.org/2005/11/its/rdf#taIdentRef", UriUtils.createUri(link.getUri).toString, sourceUrl, null)  //TODO IRI's might throw exception in org.dbpedia.extraction.destinations.formatters please check this
        if(writeLinkAnchors)
          words += nifLinks(word, RdfNamespace.NIF.append("anchorOf"), link.getLinkText, sourceUrl, RdfNamespace.XSD.append("string"))
      }
    }
    words
  }

  protected def extractTextFromHtml(pageSection: PageSection, extractionContext: NifExtractorContext): Try[TempHtmlExtractionResults] = {
    Try {
      var paragraphs = List[Paragraph]()
      var errors = List[String]()
      var abstractText: String = ""
      var offset: Int = 0
      for (elementNode <- pageSection.content) {
        if (elementNode.isInstanceOf[TextNode]) {
          val paragraph = new Paragraph(offset, "")
          val text = elementNode.asInstanceOf[TextNode].text().trim
          if(text.length > 0) {
            if(abstractText.length > 0 && List('.',',',';',':').contains(text.charAt(0))) {
              offset += text.length
              abstractText = abstractText.substring(0, abstractText.length-1) + text + " "
            }
            else{
              offset += text.length +1
              abstractText += text + " "
            }
            paragraph.addText(text)
            paragraphs ++= List(paragraph)
          }
        }
        else {
          val extractor: LinkExtractor = new LinkExtractor(offset, extractionContext)
          val traversor: NodeTraversor = new NodeTraversor(extractor)
          traversor.traverse(elementNode)
          val cleanedLinkText = extractor.getText.trim
          if (cleanedLinkText.length > 0){
            if(abstractText.length > 0 && List('.',',',';',':').contains(cleanedLinkText.charAt(0))) {
              offset = extractor.getOffset - (extractor.getText.length - cleanedLinkText.length)
              abstractText = abstractText.substring(0, abstractText.length-1) + cleanedLinkText + " "
            }
            else{
              offset = extractor.getOffset - (extractor.getText.length - cleanedLinkText.length) +1
              abstractText += cleanedLinkText + " "
            }
            paragraphs ++= extractor.getParagraphs.asScala
            errors ++= extractor.getErrors.asScala
          }
        }
      }
      new TempHtmlExtractionResults(pageSection, abstractText, offset, paragraphs, errors)
    }
  }

  @deprecated
  private def extractTextParagraph(text: String): (String, Int) = {
    var tempText: String = StringEscapeUtils.unescapeHtml4(text)
    tempText = WikiUtil.cleanSpace(tempText).trim
    if (tempText.contains("\\")) {
      tempText = tempText.replace("\\", "")
    }
    var escapeCount: Int = 0
    if (tempText.contains("\"") && !(tempText.trim == "\"")) {
      tempText = tempText.replace("\"", "\\\"")
      escapeCount = org.apache.commons.lang3.StringUtils.countMatches(tempText, "\\")
    }
    else if (tempText.trim == "\"") {
      tempText = ""
    }
    (WikiUtil.cleanSpace(tempText).trim, tempText.length - escapeCount)
  }

  private def cleanHtml(str: String): String = {
    val text = StringEscapeUtils.unescapeHtml4(str)
    StringEscapeUtils.unescapeJava(text.replaceAll("\\n", ""))
  }

  protected def getJsoupDoc(html: String): Document = {
    val doc = Jsoup.parse(cleanHtml(html))

    //delete queries
    for(query <- removeElementsCssQueries)
      for(item <- doc.select(query).asScala)
        item.remove()

    // get all tables and save them as is (after delete, since we want the same number of tables before and after)
    val tables = doc.select("table").clone().asScala

    //hack to number ol items (cant see a way to do it with css selectors in a sufficient way)
    for(ol <- doc.select("ol").asScala){
      val li = ol.children().select("li").asScala
      for(i <- 0 until li.size)
        li(i).before("<span> " + (i+1) + ". </span>")
    }

    //replace queries
    for(query <- replaceElementsCssQueries)
      for(item <- doc.select(query._1).asScala) {
        val before = query._2.substring(0, query._2.indexOf("$c"))
        val after = query._2.substring(query._2.indexOf("$c")+2)
        item.before("<span>" + before + "</span>")
        item.after("<span>" + after + "</span>")
      }

    //revert to original tables, which might be corrupted by alterations above -> tables shall be untouched by alterations!
    val zw = doc.select("table").asScala
    if(zw.size != tables.size)
      throw new Exception("An error occurred due to differing table counts")
    for(i <- zw.indices)
      zw(i).replaceWith(tables(i))

    doc
  }

  @deprecated
  private def cleanUpWhiteSpaces(input : String): String =
  {
    //replaces multiple replace functions: tempText.replace("( ", "(").replace("  ", " ").replace(" ,", ",").replace(" .", ".");
    val sb = new StringBuilder()
    val chars = input.toCharArray

    var pos = 0
    var l = ' '

    while (pos < chars.length)
    {
      val c = chars(pos)
      if(c == ' ' || c == ',' || c == '.' || c == ')' || c == ']')        //
      {
        if(l != ' ')                //
          sb.append(l)
      }
      else
        sb.append(l)

      if(l == '(' || l == '[')        //
      {
        if(c != ' ')                //
          l = c
      }
      else
        l = c
      pos += 1
    }
    sb.append(l)

    sb.toString.substring(1)   //delete first space (see init of l)
  }

  protected def getNifIri(nifClass: String, beginIndex: String, endIndex: String): String ={
    val uri = URI.create(nifContextIri)
    var iri = uri.getScheme + "://" + uri.getHost + (if(uri.getPort > 0) (":" + uri.getPort) else "") + uri.getPath + "?"
    val m = uri.getQuery.split("&").map(_.trim).collect{ case x if !x.startsWith("nif=") => x}
    iri += m.foldRight("")(_+"&"+_) + "nif=" + nifClass + "_" + beginIndex + "_" + endIndex
    iri
  }
  protected class TempHtmlExtractionResults(
     val section: PageSection,
     val text: String,
     val length: Int,
     val paragraphs: List[Paragraph],
     val errors: List[String]
   )

  protected class PageSection(
     var prev: Option[PageSection],
     var top: Option[PageSection],
     var next: Option[PageSection],
     var sub: Option[PageSection],
     val id: String,
     val title: String,
     val ref: String,
     var tableCount: Integer,
     val content: Seq[org.jsoup.nodes.Node]
   ) {
    def getUri = getNifIri("section", ref, id)
  }
}

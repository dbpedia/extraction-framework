package org.dbpedia.extraction.nif

import org.apache.commons.lang3.StringEscapeUtils
import org.dbpedia.extraction.config.provenance.DBpediaDatasets
import org.dbpedia.extraction.nif.LinkExtractor.NifExtractorContext
import org.dbpedia.extraction.nif.Paragraph.HtmlString
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.transform.{Quad, QuadBuilder}
import org.dbpedia.extraction.config.Config.NifParameters
import org.dbpedia.extraction.config.RecordCause
import org.dbpedia.extraction.mappings.Extractor
import org.dbpedia.extraction.util.CssConfigurationMap
import org.dbpedia.iri.UriUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element, TextNode}
import org.jsoup.parser.Tag
import org.jsoup.select.NodeTraversor
import uk.ac.ed.ph.snuggletex.{SerializationMethod, SnuggleEngine, SnuggleInput, XMLStringOutputOptions}

import scala.collection.convert.decorateAsScala._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}
/**
  * Created by Chile on 1/19/2017.
  */
abstract class HtmlNifExtractor(nifContextIri: String, language: String, nifParameters : NifParameters) {

  assert(nifContextIri.contains("?"), "the nifContextIri needs a query part!")

  protected val writeLinkAnchors: Boolean = nifParameters.writeLinkAnchor
  protected val writeStrings: Boolean = nifParameters.writeAnchor
  protected val cssSelectorConfigMap: CssConfigurationMap#CssLanguageConfiguration = new CssConfigurationMap(nifParameters.cssSelectorMap).getCssSelectors(language)

  protected lazy val nifContext: (String, String, String, String, String) => Quad = QuadBuilder.dynamicPredicate(language, DBpediaDatasets.NifContext.encoded) _
  protected lazy val nifStructure: (String, String, String, String, String) => Quad = QuadBuilder.dynamicPredicate(language, DBpediaDatasets.NifPageStructure.encoded) _
  protected lazy val nifLinks: (String, String, String, String, String) => Quad = QuadBuilder.dynamicPredicate(language, DBpediaDatasets.NifTextLinks.encoded) _
  protected lazy val rawTables: (String, String, String, String, String) => Quad = QuadBuilder.dynamicPredicate(language, DBpediaDatasets.RawTables.encoded) _
  protected lazy val equations: (String, String, String, String, String) => Quad = QuadBuilder.dynamicPredicate(language, DBpediaDatasets.Equations.encoded) _

  protected val templateString = "Template"

  private val sectionMap = new mutable.HashMap[PageSection, ExtractedSection]()

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
  def extractNif(graphIri: String, subjectIri: String, html: String)(exceptionHandle: (String, RecordCause.Value, Throwable) => Unit): Seq[Quad] = {

    val sections = getRelevantParagraphs(html)

    var  context = ""
    var offset = 0
    val quads = for(section <- sections) yield {
      extractTextFromHtml(section, new NifExtractorContext(language, subjectIri, templateString)) match {
        case Success(extractionResults) => {
          sectionMap.put(section, extractionResults)
          sectionMap.put(extractionResults, extractionResults)

          if (context.length != 0) {
            context = context + "\n\n"
            offset += 2
          }
          var quads = if(nifParameters.abstractsOnly)
            Seq()
          else
            makeStructureElements(extractionResults, nifContextIri, graphIri, offset)

          offset += extractionResults.getExtractedLength
          context += extractionResults.getExtractedText

          //collect additional triples
          quads ++= extendSectionTriples(extractionResults, graphIri, subjectIri)
          //forward exceptions
          extractionResults.errors.foreach(exceptionHandle(_, RecordCause.Warning, null))
          quads
        }
        case Failure(e) => {
          exceptionHandle(e.getMessage, RecordCause.Exception, e)
          List()
        }
      }
    }

    quads.flatten ++
    (if(nifParameters.abstractsOnly) Seq()
      else {
      makeContext(context, nifContextIri, graphIri, offset) ++
      extendContextTriples(quads.flatten, graphIri, subjectIri)
    })
  }

  /**
    * Each extracted section can be further enriched by this function, by providing additional quads
    * @param extractionResults - the extraction results for a particular section
    * @return
    */
  def extendSectionTriples(extractionResults: ExtractedSection, graphIri: String, subjectIri: String): Seq[Quad]

  /**
    * For each page additional triples can be added with this function
    * @param quads - the current collection of quads
    * @return
    */
  def extendContextTriples(quads: Seq[Quad], graphIri: String, subjectIri: String): Seq[Quad]

  private def makeContext(text: String, contextUri: String, sourceUrl: String, contextEnd: Int): ListBuffer[Quad] = {
    var cont = ListBuffer[Quad]()
    if (contextEnd == 0)
      return ListBuffer()
    cont += nifContext(contextUri, RdfNamespace.RDF.append("type"), RdfNamespace.NIF.append("Context"), sourceUrl, null)
    cont += nifContext(contextUri, RdfNamespace.NIF.append("beginIndex"), "0", sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger") )
    cont += nifContext(contextUri, RdfNamespace.NIF.append("endIndex"), contextEnd.toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger") )
    cont += nifContext(contextUri, RdfNamespace.NIF.append("sourceUrl"), sourceUrl, sourceUrl, null)
    cont += nifContext(contextUri, RdfNamespace.NIF.append("isString"), text, sourceUrl, RdfNamespace.XSD.append("string"))
    cont
  }

  private def makeStructureElements(section: ExtractedSection, contextUri: String, sourceUrl: String, offset: Int): ListBuffer[Quad] = {
    var triples = ListBuffer[Quad]()
    var off = offset
    val sectionUri = section.getSectionIri(offset)
    triples += nifStructure(sectionUri, RdfNamespace.RDF.append("type"), RdfNamespace.NIF.append("Section"), sourceUrl, null)
    triples += nifStructure(sectionUri, RdfNamespace.SKOS.append("notation"), section.ref, sourceUrl, RdfNamespace.RDFS.append("Literal"))
    triples += nifStructure(sectionUri, RdfNamespace.NIF.append("beginIndex"), offset.toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
    triples += nifStructure(sectionUri, RdfNamespace.NIF.append("endIndex"), (offset + section.getExtractedLength).toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
    triples += nifStructure(sectionUri, RdfNamespace.NIF.append("referenceContext"), contextUri, sourceUrl, null)

    //adding navigational properties
    section.getPrev match{
      case Some(p) =>
        triples += nifStructure(sectionUri, RdfNamespace.NIF.append("previousSection"), p.getSectionIri(), sourceUrl, null)
        triples += nifStructure(p.getSectionIri(), RdfNamespace.NIF.append("nextSection"), sectionUri, sourceUrl, null)
      case None =>
    }
    section.getTop match{
      case Some(p) =>
        triples += nifStructure(sectionUri, RdfNamespace.NIF.append("superString"), p.getSectionIri(), sourceUrl, null)
        triples += nifStructure(p.getSectionIri(), RdfNamespace.NIF.append("hasSection"), sectionUri, sourceUrl, null)
      case None =>
    }

    if(section.top.isEmpty) {
      triples += nifStructure(sectionUri, RdfNamespace.NIF.append("superString"), contextUri, sourceUrl, null)
      triples += nifStructure(contextUri, RdfNamespace.NIF.append("hasSection"), sectionUri, sourceUrl, null)
      if (section.prev.isEmpty)
        triples += nifStructure(contextUri, RdfNamespace.NIF.append("firstSection"), sectionUri, sourceUrl, null)
      if (section.next.isEmpty)
        triples += nifStructure(contextUri, RdfNamespace.NIF.append("lastSection"), sectionUri, sourceUrl, null)
    }
    else{
      triples += nifStructure(sectionUri, RdfNamespace.NIF.append("superString"), section.getTop.get.getSectionIri(), sourceUrl, null)
      triples += nifStructure(section.getTop.get.getSectionIri(), RdfNamespace.NIF.append("hasSection"), sectionUri, sourceUrl, null)
      if (section.prev.isEmpty)
        triples += nifStructure(section.getTop.get.getSectionIri(), RdfNamespace.NIF.append("firstSection"), sectionUri, sourceUrl, null)
      if (section.next.isEmpty)
        triples += nifStructure(section.getTop.get.getSectionIri(), RdfNamespace.NIF.append("lastSection"), sectionUri, sourceUrl, null)
    }

    //further specifying paragraphs of every section
    var lastParagraph: String = null

    for(i <- section.paragraphs.indices) {
      if(section.paragraphs(i).getTagName.matches("h\\d")) {
        //titles were extracted as paragraphs -> create title
        triples ++= createSectionTitle(section, contextUri, sourceUrl, offset)
        off += Paragraph.GetEscapedStringLength(section.title) + 1
      }
      else
        writeParagraph(i)
    }


    def writeParagraph(i: Int): Unit = {
      //add raw tables (text length might be 0)
      if(section.paragraphs(i).getTagName == "table")
        triples ++= saveRawTables(section.paragraphs(i).getHtmlStrings.asScala.toList, section, contextUri, sourceUrl, off)

      //add equations MathML
      if(section.paragraphs(i).getTagName == "math")
        triples ++= saveEquations(section.paragraphs(i).getHtmlStrings.asScala.toList, section, contextUri, sourceUrl, off)

      if(section.paragraphs(i).getLength == 0)
        return

      val paragraph = getNifIri("paragraph", section.paragraphs(i).getBegin(off), section.paragraphs(i).getEnd(off))

      if (lastParagraph != null) //provide the nextParagraph triple
        triples += nifStructure(lastParagraph, RdfNamespace.NIF.append("nextParagraph"), paragraph, sourceUrl, null)

      triples += nifStructure(paragraph, RdfNamespace.RDF.append("type"), RdfNamespace.NIF.append("Paragraph"), sourceUrl, null)
      triples += nifStructure(paragraph, RdfNamespace.NIF.append("beginIndex"), off.toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
      triples += nifStructure(paragraph, RdfNamespace.NIF.append("endIndex"), section.paragraphs(i).getEnd(off).toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
      triples += nifStructure(paragraph, RdfNamespace.NIF.append("referenceContext"), contextUri, sourceUrl, null)
      triples += nifStructure(paragraph, RdfNamespace.NIF.append("superString"), sectionUri, sourceUrl, null)
      if (writeStrings)
        triples += nifStructure(paragraph, RdfNamespace.NIF.append("anchorOf"), section.paragraphs(i).getText, sourceUrl, RdfNamespace.XSD.append("string"))
      triples += nifStructure(sectionUri, RdfNamespace.NIF.append("hasParagraph"), paragraph, sourceUrl, null)
      if (i == 0)
        triples += nifStructure(sectionUri, RdfNamespace.NIF.append("firstParagraph"), paragraph, sourceUrl, null)
      if (i == section.paragraphs.indices.last)
        triples += nifStructure(sectionUri, RdfNamespace.NIF.append("lastParagraph"), paragraph, sourceUrl, null)

      lastParagraph = paragraph
      triples ++= makeWordsFromLinks(section.paragraphs(i).getLinks.asScala.toList, contextUri, paragraph, sourceUrl, off)

      off += section.paragraphs(i).getLength + (if(Paragraph.FollowedByWhiteSpace(section.paragraphs(i).getText)) 1 else 0)
    }

    triples
  }

  private def saveRawTables(tables: List[HtmlString], section: PageSection, contextUri: String, sourceUrl: String, offset: Int): ListBuffer[Quad] = {
    val triples = ListBuffer[Quad]()
    for(table <- tables){
      section.tableCount = section.tableCount+1
      val position = offset + table.getOffset
      val tableUri = getNifIri("table", position, position).replaceFirst("&char=.*", "&ref=" + section.ref + "_" + section.tableCount)

      triples += rawTables(tableUri, RdfNamespace.RDF.append("type"), RdfNamespace.NIF.append("OffsetBasedString"), sourceUrl, null)
      triples += rawTables(tableUri, RdfNamespace.NIF.append("referenceContext"), contextUri, sourceUrl, null)
      triples += rawTables(tableUri, RdfNamespace.NIF.append("beginIndex"), position.toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
      triples += rawTables(tableUri, RdfNamespace.NIF.append("endIndex"), position.toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
      triples += rawTables(tableUri, RdfNamespace.HTML.append("class"), table.getOuterClass, sourceUrl, RdfNamespace.XSD.append("string"))
      triples += rawTables(tableUri, RdfNamespace.DC.append("source"), table.getHtml, sourceUrl, RdfNamespace.RDF.append("XMLLiteral"))
    }
    triples
  }

  private def saveEquations(equs: List[HtmlString], section: PageSection, contextUri: String, sourceUrl: String, offset: Int): ListBuffer[Quad] = {
    val triples = ListBuffer[Quad]()
    for(equ <- equs){
      section.equationCount = section.equationCount+1
      val position = offset + equ.getOffset
      val equUri = getNifIri("equation", position, position).replaceFirst("&char=.*", "&ref=" + section.ref + "_" + section.equationCount)

      triples += equations(equUri, RdfNamespace.RDF.append("type"), RdfNamespace.NIF.append("OffsetBasedString"), sourceUrl, null)
      triples += equations(equUri, RdfNamespace.NIF.append("referenceContext"), contextUri, sourceUrl, null)
      triples += equations(equUri, RdfNamespace.NIF.append("beginIndex"), position.toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
      triples += equations(equUri, RdfNamespace.NIF.append("endIndex"), position.toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
      triples += equations(equUri, RdfNamespace.DC.append("source"), equ.getHtml, sourceUrl, RdfNamespace.RDF.append("XMLLiteral"))
    }
    triples
  }

  private def createSectionTitle(section: ExtractedSection, contextUri: String, sourceUrl: String, offset: Int): ListBuffer[Quad] = {
    val triples = ListBuffer[Quad]()
    if(section.title == "abstract")
      return triples                //the abstract has no title

    val tableUri = getNifIri("title", offset, offset + section.title.length)
    section.tableCount = section.tableCount+1

    triples += nifStructure(tableUri, RdfNamespace.RDF.append("type"), RdfNamespace.NIF.append("Title"), sourceUrl, null)
    triples += nifStructure(tableUri, RdfNamespace.NIF.append("referenceContext"), contextUri, sourceUrl, null)
    triples += nifStructure(tableUri, RdfNamespace.NIF.append("beginIndex"), offset.toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
    triples += nifStructure(tableUri, RdfNamespace.NIF.append("endIndex"), (offset + section.title.length).toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
    triples += nifStructure(tableUri, RdfNamespace.NIF.append("superString"), section.getSectionIri(), sourceUrl, null)
    if(writeLinkAnchors)
      triples += nifStructure(tableUri, RdfNamespace.NIF.append("anchorOf"), section.title, sourceUrl, RdfNamespace.XSD.append("string"))
    triples
  }

  private def makeWordsFromLinks(links: List[Link], contextUri: String, paragraphUri: String, sourceUrl: String, offset: Int): ListBuffer[Quad] = {
    var words = ListBuffer[Quad]()
    for (link <- links) {
      if (link.getWordEnd - link.getWordStart > 0) {
        val typ = if (link.getLinkText.split(" ").length > 1) "Phrase" else "Word"
        val word = getNifIri(typ.toString.toLowerCase, offset + link.getWordStart, offset + link.getWordEnd)
        words += nifLinks(word, RdfNamespace.RDF.append("type"), RdfNamespace.NIF.append(typ), sourceUrl, null)
        words += nifLinks(word, RdfNamespace.NIF.append("referenceContext"), contextUri, sourceUrl, null)
        words += nifLinks(word, RdfNamespace.NIF.append("beginIndex"), (offset + link.getWordStart).toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
        words += nifLinks(word, RdfNamespace.NIF.append("endIndex"), (offset + link.getWordEnd).toString, sourceUrl, RdfNamespace.XSD.append("nonNegativeInteger"))
        words += nifLinks(word, RdfNamespace.NIF.append("superString"), paragraphUri, sourceUrl, null)
        UriUtils.createURI(link.getUri) match{
          case Success(s) => words += nifLinks(word, "http://www.w3.org/2005/11/its/rdf#taIdentRef", s.toString, sourceUrl, null)  //TODO IRI's might throw exception in org.dbpedia.extraction.destinations.formatters please check this
          case Failure(f) =>
        }
        if(writeLinkAnchors)
          words += nifLinks(word, RdfNamespace.NIF.append("anchorOf"), link.getLinkText, sourceUrl, RdfNamespace.XSD.append("string"))
      }
    }
    words
  }

  protected def extractTextFromHtml(pageSection: PageSection, extractionContext: NifExtractorContext): Try[ExtractedSection] = {
    Try {
      val section = new ExtractedSection(pageSection, List(), List())
      val element = new Element(Tag.valueOf("div"), "")
      pageSection.content.foreach(element.appendChild)

      val extractor: LinkExtractor = new LinkExtractor(extractionContext)
      val traversor: NodeTraversor = new NodeTraversor(extractor)
      traversor.traverse(element)
      if (extractor.getParagraphs.size() > 0){
        section.addParagraphs(extractor.getParagraphs.asScala.toList)
        section.addErrors(extractor.getErrors.asScala.toList)
      }
      else if(extractor.getTableCount > 0){
        section.addParagraphs(extractor.getParagraphs.asScala.toList)
        section.addErrors(extractor.getErrors.asScala.toList)
      }
      section
    }
  }

  protected def latexToMathMl(formula: String): Try[String] = {
    /* Create vanilla SnuggleEngine and new SnuggleSession */
    val engine = new SnuggleEngine()
    val session = engine.createSession()

    /* Parse some very basic Math Mode input */
    val input = new SnuggleInput(formula)
    session.parseInput(input)

    //build options
    val options = new XMLStringOutputOptions()
    options.setSerializationMethod(SerializationMethod.XHTML)
    options.setIndenting(true)
    options.setEncoding("UTF-8")
    options.setAddingMathSourceAnnotations(true)

    /* Convert the results to an XML String, which in this case will
     * be a single MathML <math>...</math> element. */
    Try{session.buildXMLString(options)}
  }

  private def cleanHtml(str: String): String = {
    StringEscapeUtils.unescapeHtml4(str).replaceAll("\n", "")
    //text = StringEscapeUtils.unescapeJava(text)
  }

  protected def getJsoupDoc(html: String): Document = {
    val doc = Jsoup.parse(html.replaceAll("\n", ""))

    //delete queries
    for(query <- cssSelectorConfigMap.removeElements)
      for(item <- doc.select(query).asScala)
        item.remove()

    // get preserve elements
    val codes = doc.select("code, text[xml:space='preserve']").clone().asScala.toList

    // get all tables and save them as is (after delete, since we want the same number of tables before and after)
    val tables = doc.select("table").clone().asScala

    val equations = for(e <- doc.select("span.tex").asScala) yield latexToMathMl(e.text())

    //hack to number ol items (cant see a way to do it with css selectors in a sufficient way)
    for(ol <- doc.select("ol").asScala){
      val li = ol.children().select("li").asScala
      for(i <- li.indices)
        li(i).before("<span> " + (i+1) + ". </span>")
    }

    //if h1 titles exist in page content, replace all titles with 'h'+(i+1)
    if(doc.select("h1").size() > 0) {
      for (i <- 4 to 1 by -1) {
        for (item <- doc.select("h"+i.toString).asScala) {
          val replaceElement = new Element(Tag.valueOf("h" + (i+1).toString), "")
          for(child <- item.children().asScala)
            replaceElement.appendChild(child)
          item.replaceWith(replaceElement)
        }
      }
    }

    //replace queries
    for(css <- cssSelectorConfigMap.replaceElements) {
      val query = css.split("->")
      for (item <- doc.select(query(0)).asScala) {
        val before = query(1).substring(0, query(1).indexOf("$c"))
        val after = query(1).substring(query(1).indexOf("$c") + 2)
        item.before("<span>" + before  + "</span>")
        item.after("<span>" + after  + "</span>")
      }
    }

    //encircle notes
    for(css <- cssSelectorConfigMap.noteElements) {
      val query = css.split("\\s*->\\s*")
      for (item <- doc.select(query(0)).asScala) {
        val before = query(1).substring(0, query(1).indexOf("$c"))
        val after = query(1).substring(query(1).indexOf("$c") + 2)
        item.before("<span class='notebegin'>" + before + "</span>")
        item.after("<span class='noteend'>" + after  + "</span>")
      }
    }

    //deal with <code> elements
    val cods = doc.select("code, text[xml:space='preserve']").asScala
    if(cods.size != codes.size)
      throw new Exception("An error occurred due to differing codes counts")
    for(i <- codes.indices){
        var code = ""
        for (child <- codes(i).childNodes().asScala) {
          code += child.toString
        }
        val replaceElement = new Element(Tag.valueOf("span"), "")
        replaceElement.appendChild(new TextNode(StringEscapeUtils.unescapeHtml4(code), ""))
        cods(i).replaceWith(replaceElement)
      }

    //revert to original tables, which might be corrupted by alterations above -> tables shall be untouched by alterations!
    val zw = doc.select("table").asScala
    if(zw.size != tables.size)
      throw new Exception("An error occurred due to differing table counts")
    for(i <- zw.indices)
      zw(i).replaceWith(tables(i))

    //replace latex equations with mathML
    val eqhs = doc.select("span.tex").asScala
    for(i <- eqhs.indices)
      equations(i) match{
        case Success(e) => eqhs(i).replaceWith(Jsoup.parseBodyFragment(e).body().child(0))
        case Failure(f) => eqhs(i).replaceWith(Jsoup.parseBodyFragment("<span/>").body().child(0))
      }


    doc
  }

  protected def getNifIri(nifClass: String, beginIndex: Int, endIndex: Int): String ={
    UriUtils.createURI(nifContextIri) match{
      case Success(uri) =>
        var iri = uri.getScheme + "://" + uri.getHost + (if(uri.getPort > 0) ":" + uri.getPort else "") + uri.getPath + "?"
          val m = uri.getQuery.split("&").map(_.trim).collect{ case x if !x.startsWith("nif=") => x}
        iri += m.foldRight("")(_+"&"+_) + "nif=" + nifClass + "&char=" + beginIndex + "," + endIndex
        iri.replace("?&", "?")
      case Failure(f) => throw f
    }
  }

  protected class PageSection(
   var prev: Option[PageSection],
   var top: Option[PageSection],
   var next: Option[PageSection],
   var sub: Option[PageSection],
   val id: String,
   val title: String,
   val ref: String,
   var tableCount: Integer,
   var equationCount: Integer,
   val content: Seq[org.jsoup.nodes.Node]
 ) {

    private def getExtractedVersion(section: PageSection): Option[ExtractedSection] = sectionMap.get(section)

    def getSub: Option[ExtractedSection] = sub match{
      case Some(s) => getExtractedVersion(s)
      case None => None
    }
    def getTop: Option[ExtractedSection] = top match{
      case Some(s) => getExtractedVersion(s)
      case None => None
    }
    def getNext: Option[ExtractedSection] = next match{
      case Some(s) => getExtractedVersion(s)
      case None => None
    }
    def getPrev: Option[ExtractedSection] = prev match{
      case Some(s) => getExtractedVersion(s)
      case None => None
    }
  }

  protected class ExtractedSection(
    val section: PageSection,
    var paragraphs: List[Paragraph],
    var errors: List[String]
  ) extends PageSection(section.prev, section.top, section.next, section.sub, section.id, section.title, section.ref, section.tableCount, section.equationCount, section.content)
  {
    private var offset = 0
    def getSectionIri(offset: Int = offset): String = {
      this.offset = offset
      getNifIri("section", getBeginIndex(offset), getEndIndex(offset))
    }

    private var extractedText: String = _
    def getExtractedText: String ={
      if(extractedText == null)
        extractedText = HtmlNifExtractor.ExtractTextFromParagraphs(paragraphs)
      extractedText
    }

    def getExtractedLength: Int ={
      if(extractedText == null)
        extractedText = HtmlNifExtractor.ExtractTextFromParagraphs(paragraphs)
      Paragraph.GetEscapedStringLength(extractedText)
    }

    def getBeginIndex(offset: Int = offset): Int = {
      this.offset = offset
      if(paragraphs.nonEmpty)
        paragraphs.head.getBegin(offset)
      else
        offset
    }

    def getEndIndex(offset: Int = offset): Int ={
      this.offset = offset
      if(paragraphs.nonEmpty)
        getBeginIndex(offset) + getExtractedLength
      else
        offset
    }

    def addParagraphs(p: List[Paragraph]): Unit = paragraphs ++= p

    def addErrors(e: List[String]): Unit = errors ++= e

/*    def whiteSpaceAfterSection = {
      val text = getExtractedText
      if(text.length > 0)
        !nonSpaceChars.contains(text.charAt(text.length - 1))
      else
        false
    }*/

    private var tablecounty = 0
    private def tablecount = {
      tablecounty= tablecounty+1
      tablecounty
    }
  }

  object HtmlNifExtractor{

    def ExtractTextFromParagraphs(paragraphs: Seq[Paragraph]): String = {
      var length = 0
      var text = ""
      for (paragraph <- paragraphs)
        if (paragraph.getLength > 0) {
          if (text.length != 0) {
            if (paragraph.getTagName == "note") {
              text += paragraph.getText + "\n"
              length += paragraph.getLength + 1
            }
            else if (paragraph.getTagName.matches("h\\d")) {
              text += paragraph.getText + "\n"
              length += paragraph.getLength + 1
            }
            else if (Paragraph.FollowedByWhiteSpace(text)) {
              text += " " + paragraph.getText
              length += paragraph.getLength + 1
            }
            else {
              text += paragraph.getText
              length += paragraph.getLength
            }
          }
          else {
            if (paragraph.getTagName.matches("h\\d")) {
              text += paragraph.getText + "\n"
              length += paragraph.getLength + 1
            }
            else {
              text += paragraph.getText
              length += paragraph.getLength
            }
          }
        }
      text
    }
  }
}

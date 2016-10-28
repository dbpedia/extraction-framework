package org.dbpedia.extraction.mappings

import org.apache.commons.lang3.{StringEscapeUtils}
import org.dbpedia.extraction.nif.LinkExtractor.LinkExtractorContext
import org.dbpedia.extraction.nif.{Link, LinkExtractor, Paragraph}
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.NodeTraversor

import scala.collection.mutable.ListBuffer
import scala.language.reflectiveCalls
import org.dbpedia.extraction.destinations.{QuadBuilder, DBpediaDatasets, Quad}
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.{WikiUtil, Language}
import scala.collection.convert.decorateAsScala._

/**
  * Extracts page abstract html.
  *
  * Based on AbstractExtractor, major difference is the parameter
  * apiParametersFormat = "action=parse&prop=text&section=0&format=xml&page=%s"
  *
  * Should a new AbstractExtractor be developed, setting this api parameters
  * should result in the same functionality.
  */

class AbstractLinkExtractor(
     context : {
       def ontology : Ontology
       def language : Language
     }
   )
  extends AbstractExtractor(context)
{
  //API parameters to geht HTML of first section
  override val apiParametersFormat = "uselang="+language + protectedParams.get("apiNifParametersFormat").get.asText()

  override val xmlPath = protectedParams.get("apiNifXmlPath").get.asText.split(",").map(_.trim)

  protected val writeStrings = protectedParams.get("writeNifStrings").get.asBoolean()
  protected val shortAbstractLength = protectedParams.get("minShortAbstractLength").get.asInt()

  override val datasets = Set(DBpediaDatasets.NifAbstractContext,DBpediaDatasets.NifPageStructure,DBpediaDatasets.NifTextLinks,DBpediaDatasets.LongAbstracts, DBpediaDatasets.ShortAbstracts)

  protected lazy val nifContext = QuadBuilder.dynamicPredicate(context.language.isoCode, DBpediaDatasets.NifAbstractContext.name) _
  protected lazy val nifStructure = QuadBuilder.dynamicPredicate(context.language.isoCode, DBpediaDatasets.NifPageStructure.name) _
  protected lazy val nifLinks = QuadBuilder.dynamicPredicate(context.language.isoCode, DBpediaDatasets.NifTextLinks.name) _

  private val templateString = Namespaces.names(context.language).get(Namespace.Template.code) match {
    case Some(x) => x
    case None => "Template"
  }

  override def extract(pageNode : PageNode, subjectUri : String, pageContext : PageContext): Seq[Quad] =
  {
    //Only extract abstracts for pages from the Main namespace
    if(pageNode.title.namespace != Namespace.Main) return Seq.empty

    //Don't extract abstracts from redirect and disambiguation pages
    if(pageNode.isRedirect || pageNode.isDisambiguation) return Seq.empty

    //Retrieve page text
    var html = super.retrievePage(pageNode.title /*, abstractWikiText*/)

    html = super.postProcess(pageNode.title, html)

    if (html.trim.isEmpty)
      return Seq.empty

    extractNif(pageNode.sourceUri, subjectUri, html)
  }

  def extractNif(sourceUrl: String, subjectUri: String, html: String): List[Quad] = {
    val paragraphs = getRelevantParagraphs(html)

    val extractionResults = getLinkAndText(paragraphs, new LinkExtractorContext(language, subjectUri, templateString))

    val context = makeContext(extractionResults._1, subjectUri, sourceUrl, extractionResults._2)

    val words = if (context.nonEmpty) makeStructureElements(extractionResults._3, context.head.subject, extractionResults._2).toList else List()

    if(!isTestRun && context.nonEmpty) {
      context += longQuad(subjectUri, extractionResults._1, sourceUrl)
      context += shortQuad(subjectUri, getShortAbstract(extractionResults._3), sourceUrl)
    }
    context.toList ::: words
  }

  private def getShortAbstract(paragraphs: List[Paragraph]): String = {
    var shortAbstract = ""
    for (p <- paragraphs) {
      if (shortAbstract.length <= shortAbstractLength || shortAbstract.length + p.getText.length < shortAbstractLength * 3) //create short Abstract between [shortAbstractLength, shortAbstractLength*3]
        shortAbstract += p.getText
    }
    if (shortAbstract.length > shortAbstractLength * 4) //only cut abstract if the first paragraph is exceedingly long
      shortAbstract = shortAbstract.substring(0, shortAbstractLength * 4)
    shortAbstract
  }

  private def makeContext(text: String, resource: String, sourceUrl: String, contextEnd: Int): ListBuffer[Quad] = {
    var cont = ListBuffer[Quad]()
    //TODO: dbp version!
    val res = resource + "#nif=context"
    if (contextEnd == 0)
      return ListBuffer()
    cont += nifContext(res, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#String", null, null)
    cont += nifContext(res, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#OffsetBasedString", null, null)
    cont += nifContext(res, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#Context", null, null)
    cont += nifContext(res, "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#beginIndex", "0", null, "http://www.w3.org/2001/XMLSchema#nonNegativeInteger" )
    cont += nifContext(res, "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#endIndex", contextEnd.toString, null, "http://www.w3.org/2001/XMLSchema#nonNegativeInteger" )
    cont += nifContext(res, "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#sourceUrl", sourceUrl, null, null)
    cont += nifContext(res, "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#isString", text, null, "http://www.w3.org/2001/XMLSchema#string")
    cont += nifContext(res, "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#predLang", "http://lexvo.org/id/iso639-3/" + context.language.iso639_3, null, null)
    cont
  }

  //TODO update this for whole page structure - at the moment we collect only paragraphs from the abstract - the abstract is section 0 while other sections are numbered by the TOC
  private def makeStructureElements(paragraphs: List[Paragraph], contextUri: String, contextEnd: Int): ListBuffer[Quad] = {
    var triples = ListBuffer[Quad]()
    val abstractUri = contextUri.substring(0, contextUri.indexOf('#')) + "#nif=section&sec=0&dbpv=2016-04&offset=0" + "_" + contextEnd
    triples += nifStructure(abstractUri, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#String", null, null)
    triples += nifStructure(abstractUri, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#OffsetBasedString", null, null)
    triples += nifStructure(abstractUri, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#Section", null, null)
    triples += nifStructure(abstractUri, "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#beginIndex", "0", null, "http://www.w3.org/2001/XMLSchema#nonNegativeInteger")
    triples += nifStructure(abstractUri, "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#endIndex", contextEnd.toString, null, "http://www.w3.org/2001/XMLSchema#nonNegativeInteger")
    triples += nifStructure(abstractUri, "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#referenceContext", contextUri, null, null)
    for(i <- paragraphs.indices) {
      val paragraph = contextUri.substring(0, contextUri.indexOf('#')) + "#nif=paragraph&sec=0&dbpv=2016-04&offset=" + paragraphs(i).getBegin + "_" + paragraphs(i).getEnd
      triples += nifStructure(paragraph, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#String", null, null)
      triples += nifStructure(paragraph, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#OffsetBasedString", null, null)
      triples += nifStructure(paragraph, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#Paragraph", null, null)
      triples += nifStructure(paragraph, "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#beginIndex", paragraphs(i).getBegin.toString, null, "http://www.w3.org/2001/XMLSchema#nonNegativeInteger")
      triples += nifStructure(paragraph, "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#endIndex", paragraphs(i).getEnd.toString, null, "http://www.w3.org/2001/XMLSchema#nonNegativeInteger")
      triples += nifStructure(paragraph, "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#referenceContext", contextUri, null, null)
      if (writeStrings)
        triples += nifStructure(paragraph, "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#anchorOf", paragraphs(i).getText, null, "http://www.w3.org/2001/XMLSchema#string")
      triples += nifStructure(abstractUri, "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#hasParagraph", paragraph, null, null)
      if(i == 0)
        triples += nifStructure(abstractUri, "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#firstParagraph", paragraph, null, null)
      if(i == paragraphs.indices.last)
        triples += nifStructure(abstractUri, "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#lastParagraph", paragraph, null, null)

      triples ++= makeWordsFromLinks(paragraphs(i).getLinks.asScala.toList, contextUri, paragraph)
    }
    triples
  }

  private def makeWordsFromLinks(links: List[Link], contextUri: String, paragraphUri: String): ListBuffer[Quad] = {
    var words = ListBuffer[Quad]()
      for (link <- links) {
        if (link.getWordEnd - link.getWordStart > 0) {
          val typ = if (link.getLinkText.split(" ").length > 1)
            "Phrase"
          else
            "Word"
          //TODO: dbp version!
          val word = contextUri.substring(0, contextUri.indexOf('#')) + "#nif=" + typ.toString.toLowerCase + "&sec=0&dbpv=2016-04&offset=" + link.getWordStart + "_" + link.getWordEnd
          words += nifLinks(word, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#String", null, null)
          words += nifLinks(word, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#OffsetBasedString", null, null)
          words += nifLinks(word, "http://www.w3.org/1999/02/22-rdf-syntax-ns#type", "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#" + typ, null, null)
          words += nifLinks(word, "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#referenceContext", contextUri, null, null)
          words += nifLinks(word, "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#beginIndex", link.getWordStart.toString, null, "http://www.w3.org/2001/XMLSchema#nonNegativeInteger")
          words += nifLinks(word, "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#endIndex", link.getWordEnd.toString, null, "http://www.w3.org/2001/XMLSchema#nonNegativeInteger")
          words += nifLinks(word, "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#superString", paragraphUri, null, null)
          words += nifLinks(word, "http://www.w3.org/2005/11/its/rdf#taIdentRef", link.getUri, null, null)
          if(writeStrings)
            words += nifLinks(word, "http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#anchorOf", link.getLinkText, null, "http://www.w3.org/2001/XMLSchema#string")
        }
      }
    words
  }

  private def getLinkAndText(line: String, extractionContext: LinkExtractorContext): (String, Int, List[Paragraph]) = {
    var paragraphs = List[Paragraph]()
    val doc: Document = Jsoup.parse("<span>" + line + "</span>")
    var abstractText: String = ""
    val nodes = doc.select("body").first.childNodes.asScala
    var offset: Int = 0
    for (elementNode <- nodes) {
      if (elementNode.nodeName == "#text") {
        val txtParagraph = extractTextParagraph(elementNode.toString.trim)
        if(txtParagraph._1.length > 0 && abstractText.length > 0) {
          abstractText += " " + txtParagraph._1
          offset += 1 + txtParagraph._2
        }
        else
          {
            abstractText += txtParagraph._1
            offset += txtParagraph._2
          }
      }
      else {
        val extractor: LinkExtractor = new LinkExtractor(offset, extractionContext)
        val traversor: NodeTraversor = new NodeTraversor(extractor)
        traversor.traverse(elementNode)
        val cleanedLinkText = WikiUtil.cleanSpace(extractor.getText).trim
          if (cleanedLinkText.length > 0 && abstractText.length > 0) {
            offset = 1 + extractor.getOffset - (extractor.getText.length - cleanedLinkText.length)
            abstractText += " " + cleanedLinkText
          }
          else {
            offset = extractor.getOffset - (extractor.getText.length - cleanedLinkText.length)
            abstractText += cleanedLinkText
          }
          paragraphs ++= extractor.getParagraphs.asScala
      }
    }
    var beforeTrim: Int = 0
    var offsetReduce: Int = 0
    if (!abstractText.startsWith(" ") && abstractText.endsWith(" ")) {
      beforeTrim = abstractText.length
      abstractText = abstractText.trim
      if (beforeTrim > abstractText.length) {
        offsetReduce = beforeTrim - abstractText.length
      }
    }
    if (offsetReduce > 0) {
      offset -= offsetReduce
    }
    (abstractText, offset, paragraphs)
  }

  private def extractTextParagraph(text: String): (String, Int) ={
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
    (cleanUpWhiteSpaces(tempText), tempText.length - escapeCount)
  }

  private def cleanHtml(str: String): String ={
    var text = StringEscapeUtils.unescapeHtml4(str)
    text = text.replace("&apos;", "'").replace("<b>", "").replace("</b>", "").replace("<i>", "").replace("</i>", "").replace("<p></p>", "")
    StringEscapeUtils.unescapeJava(text)
  }

  private def getRelevantParagraphs (html: String): String = {
    var clean = cleanHtml(html)
    if (clean.contains("<ol class=\"references\">")) {
      clean = clean.substring(0, clean.lastIndexOf("<ol class=\"references\">"))
    }
    else {
      clean = clean.substring(0, Math.min(clean.lastIndexOf("</p>") + 4, clean.length))
    }
    clean
  }

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
}

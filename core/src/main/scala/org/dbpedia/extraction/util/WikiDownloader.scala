package org.dbpedia.extraction.util

import java.io.{File,FileOutputStream,IOException,InputStreamReader,OutputStreamWriter}
import java.net.URL
import java.nio.charset.Charset

import scala.collection.Iterator.empty
import scala.collection.Iterator.single
import scala.collection.JavaConversions.asJavaIterator

import org.dbpedia.extraction.wikiparser.WikiTitle.Namespace

import javax.xml.namespace.QName
import javax.xml.stream.events.{Attribute,StartDocument,StartElement}
import javax.xml.stream.{XMLEventFactory,XMLEventReader,XMLEventWriter,XMLInputFactory,XMLOutputFactory}


/**
 * Downloads all pages for a given list of namespaces from api.php and transforms them
 * into the format of the dump files (because XMLSource understands that format). 
 */
class WikiDownloader(val apiUrl : String) {
  
  // TODO: get the charset from the URL? Difficult - writing starts before reading.
  val charset = Charset.forName("UTF-8")
  
  // newInstance() is deprecated, but the newer methods don't exist in many JDK 6 versions
  val inFactory = XMLInputFactory.newInstance
  val outFactory = XMLOutputFactory.newInstance
  val events = XMLEventFactory.newInstance
  
  def buildURL(namespace : Namespace, gapfrom : String) : URL = 
  {
    var sb = new StringBuilder
    sb append apiUrl
    sb append "?action=query&generator=allpages&prop=revisions&rvprop=ids|content&format=xml"
    sb append "&gapnamespace=" append namespace.id append "&gaplimit=50"
    if (gapfrom != null) sb append "&gapfrom=" append gapfrom.replace(' ', '_')
    // I'm not sure what kind of escaping URL is doing. Seems ok if we just replace spaces.
    new URL(sb.toString)
  }

  def download(file : File, namespaces : Namespace *) : Unit =
  {
    val out = new FileOutputStream(file)
    try
    {
      val writer = new OutputStreamWriter(out, charset)
      implicit val xmlOut = outFactory.createXMLEventWriter(writer)
      writeDocument(charset.name){
        // TODO: the generated file is probably not valid according to this schema. 
        addElementNS("mediawiki", "http://www.mediawiki.org/xml/export-0.6/") {
          for (namespace <- namespaces) addPages(namespace)
        }
      }
      xmlOut.close
    }
    finally out.close
  }
    
  def addPages(namespace : Namespace)(implicit xmlOut : XMLEventWriter) : Unit =
  {
    var gapfrom = ""
    do
    {
      val url = buildURL(namespace, gapfrom)
      gapfrom = null
      
      val in = url.openStream
      try
      {
        val reader = new InputStreamReader(in, charset)
        implicit val xmlIn = inFactory.createXMLEventReader(reader)
        readDocument { _ =>
          getElement("api") { _ =>
            ifElement("error") { error => throw new IOException(error getAttr "info") }
            ifElement("query-continue") { _ =>
              getElement("allpages") { allpages => gapfrom = allpages getAttr "gapfrom" } 
            }
            ifElement("query") { _ => // empty namespace returns no <query> 
              getElement("pages") { _ =>
                forElements("page") { page =>
                  addElement("page") {
                    addElement("title") { addText( page getAttr "title" ) }
                    addElement("ns") { addText( page getAttr "ns" ) }
                    addElement("id") { addText( page getAttr "pageid" ) }
                    getElement("revisions") { _ =>
                      getElement("rev") { rev =>
                      forText { text =>
                        addElement ("revision") {
                          addElement ("id") { addText( rev getAttr "revid" ) }
                            addElement ("text") { addText(text) }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
        xmlIn.close
      }
      finally in.close
    } while (gapfrom != null)
  }
  
  /**
   * generate document, callback descends into it.
   */
  def writeDocument(encoding : String)(go : => Unit)(implicit xmlOut : XMLEventWriter) : Unit = {
    xmlOut.add(events.createStartDocument(encoding))
    go
    xmlOut add events.createEndDocument()
  }
  
  /**
   * add element with whitespace, callback descends into it. 
   */
  def addElementNS(name : String, namespace : String, attributes : Attribute *)(go : => Unit)(implicit xmlOut : XMLEventWriter) : Unit = {
    val namespaces = if (namespace == null) empty else single(events.createNamespace(namespace))
    xmlOut.add(events.createStartElement("", namespace, name, attributes.iterator, namespaces))
    go
    xmlOut add events.createEndElement("", namespace, name)
  }
  
  /**
   * add element without whitespace, callback descends into it. 
   */
  def addElement(name : String, attributes : Attribute *)(go : => Unit)(implicit xmlOut : XMLEventWriter) : Unit = {
    addElementNS(name, null, attributes : _*)(go)
  }
  
  /**
   * read document, callback descends into it.
   */
  def readDocument(go : StartDocument => Unit)(implicit xmlIn : XMLEventReader) : Unit = {
    require(xmlIn.hasNext, "expected document start, found nothing")
    var event = xmlIn.nextEvent
    require(event.isStartDocument, "expected document start, found "+event)
    go(event.asInstanceOf[StartDocument])
    require(xmlIn.hasNext, "expected document end, found nothing")
    event = xmlIn.nextEvent
    require(event.isEndDocument, "expected document end, found "+event)
  }
  
  /**
   * process zero or more elements, callback descends into each one.
   */
  def forElements(name : String)(go : StartElement => Unit)(implicit xmlIn : XMLEventReader) : Unit =
  {
    while(ifElement(name)(go)) {}
  }
  
  /**
   * process zero or one element, callback descends into it.
   */
  def ifElement(name : String)(go : StartElement => Unit)(implicit xmlIn : XMLEventReader) : Boolean =
  {
    val tag = if (name != null) name else "*" 
    if (! xmlIn.hasNext) return false
    var event = xmlIn.peek
    if (! event.isStartElement) return false
    val start = event.asStartElement
    if (name != null && start.getName.getLocalPart != name) return false
    xmlIn.nextEvent // take it
    go(start)
    require(xmlIn.hasNext, "expected </"+tag+">, found nothing")
    event = xmlIn.nextEvent
    require(event.isEndElement, "expected </"+tag+">, found "+event)
    val end = event.asEndElement
    if (name != null) require(end.getName.getLocalPart == name)
    true
  }
  
  /**
   * process one element, callback descends into it.
   */
  def getElement(name : String)(go : StartElement => Unit)(implicit xmlIn : XMLEventReader) : Unit =
  {
    val tag = if (name != null) name else "*" 
    require(xmlIn.hasNext, "expected <"+tag+">, found nothing")
    var event = xmlIn.nextEvent
    require(event.isStartElement, "expected <"+tag+">, found "+event)
    val start = event.asStartElement
    if (name != null) require(start.getName.getLocalPart == name, "expected <"+tag+">, found "+event)
    go(start)
    require(xmlIn.hasNext, "expected </"+tag+">, found nothing")
    event = xmlIn.nextEvent
    require(event.isEndElement, "expected </"+name+">, found "+event)
    val end = event.asEndElement
    if (name != null) require(end.getName.getLocalPart == name, "expected </"+name+">, found "+event)
  }
  
  /**
   * get attribute from given element. TODO: find a more elegant way.
   */
  def getAttr(element : StartElement, name : String)(go : String => Unit) : String = {
    val attr = element.getAttributeByName(new QName(name))
    require(attr != null, "expected @"+name+", found nothing")
    val value = attr.getValue
    go(value)
    value
  }
  
  /**
   * Wrapper class for StartElement so we can use getAttr.
   */
  class RichStartElement(val element : StartElement)
  {
    def getAttr(name : String) : String = {
      val attr = element.getAttributeByName(new QName(name))
      require(attr != null, "expected @"+name+", found nothing")
      attr.getValue
    }
  }
  
  /**
   * implicit wrapper for StartElement so we can use getAttr.
   */
  implicit def richStartElement (element : StartElement) : RichStartElement = { 
    new RichStartElement(element)
  }
  
  /**
   * process text content, callback does something with it.
   */
  def forText(go : String => Unit)(implicit xmlIn : XMLEventReader) : Unit = {
    val sb = new StringBuilder
    var found = true
    do {
      found = false
      if (xmlIn.hasNext) {
        var event = xmlIn.peek
        if (event.isCharacters) {
          xmlIn.nextEvent // take it
          sb append event.asCharacters.getData
          found = true
        }
      }
    } while (found)
    go(sb toString)
  }
  
  /**
   * Generate text content in current position.
   */
  def addText(text : String)(implicit xmlOut : XMLEventWriter) : Unit = {
    xmlOut.add(events.createCharacters(text))
  }
}

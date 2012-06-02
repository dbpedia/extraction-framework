package org.dbpedia.extraction.util

import javax.xml.namespace.QName
import javax.xml.stream.events.{StartDocument,StartElement}
import javax.xml.stream.{XMLEventReader}

/**
 * Wraps an XMLEventReader in a fluent API.
 */
class XMLEventAnalyzer(private val xmlIn : XMLEventReader) {
  
  /**
   * read document, callback descends into it.
   */
  def document[R](go : StartDocument => R) : R = {
    require(xmlIn.hasNext, "expected document start, found nothing")
    var event = xmlIn.nextEvent
    require(event.isStartDocument, "expected document start, found "+event)
    val result = go(event.asInstanceOf[StartDocument])
    require(xmlIn.hasNext, "expected document end, found nothing")
    event = xmlIn.nextEvent
    require(event.isEndDocument, "expected document end, found "+event)
    result
  }
  
  /**
   * process zero or more elements, callback descends into each one.
   * TODO: add a return type. But how? With another function parameter?
   */
  def elements(name : String)(go : StartElement => Unit) : Unit =
  {
    while(ifElement(name)(go)) {}
  }
  
  /**
   * process zero or one element, callback descends into it.
   * TODO: add a return type. But how? With another function parameter?
   */
  def ifElement(name : String)(go : StartElement => Unit) : Boolean =
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
  def element[R](name : String)(go : StartElement => R) : R =
  {
    val tag = if (name != null) name else "*" 
    require(xmlIn.hasNext, "expected <"+tag+">, found nothing")
    var event = xmlIn.nextEvent
    require(event.isStartElement, "expected <"+tag+">, found "+event)
    val start = event.asStartElement
    if (name != null) require(start.getName.getLocalPart == name, "expected <"+tag+">, found "+event)
    val result = go(start)
    require(xmlIn.hasNext, "expected </"+tag+">, found nothing")
    event = xmlIn.nextEvent
    require(event.isEndElement, "expected </"+name+">, found "+event)
    val end = event.asEndElement
    if (name != null) require(end.getName.getLocalPart == name, "expected </"+name+">, found "+event)
    result
  }
  
  /**
   * process text content, callback does something with it.
   */
  def text[R](go : String => R) : R = {
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
  
}

object RichStartElement {
  
  /**
   * implicit wrapper for StartElement so we can use attr.
   */
  implicit def richStartElement (element : StartElement) : RichStartElement = { 
    new RichStartElement(element)
  }
  
}

/**
 * Wrapper class for StartElement so we can use attr.
 */
class RichStartElement(val element : StartElement)
{
  def attr(name : String) : String = {
    val attr = element.getAttributeByName(new QName(name))
    require(attr != null, "expected @"+name+", found nothing")
    attr.getValue
  }
}


package org.dbpedia.extraction.util

import scala.collection.Iterator.empty
import scala.collection.Iterator.single
import scala.collection.JavaConversions.asJavaIterator

import javax.xml.stream.events.Attribute
import javax.xml.stream.{XMLEventFactory,XMLEventWriter}

/**
 * Wraps an XMLEventWriter in a fluent API.
 */
class XMLEventBuilder(private val xmlOut : XMLEventWriter, private val events : XMLEventFactory) {
  
  /**
   * generate document, callback descends into it.
   */
  def document(encoding : String)(go : => Unit) : Unit = {
    xmlOut.add(events.createStartDocument(encoding))
    go
    xmlOut add events.createEndDocument()
  }
  
  /**
   * add element with whitespace, callback descends into it. 
   */
  def elementNS(name : String, namespace : String, attributes : Attribute *)(go : => Unit) : Unit = {
    val namespaces = if (namespace == null) empty else single(events.createNamespace(namespace))
    xmlOut.add(events.createStartElement("", namespace, name, attributes.iterator, namespaces))
    go
    xmlOut add events.createEndElement("", namespace, name)
  }
  
  /**
   * add element without whitespace, callback descends into it. 
   */
  def element(name : String, attributes : Attribute *)(go : => Unit) : Unit = {
    elementNS(name, null, attributes : _*)(go)
  }
  
  /**
   * Generate text content in current position.
   */
  def text(text : String) : Unit = {
    xmlOut.add(events.createCharacters(text))
  }
}

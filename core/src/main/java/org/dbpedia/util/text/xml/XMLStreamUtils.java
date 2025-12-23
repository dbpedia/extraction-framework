package org.dbpedia.util.text.xml;

import static javax.xml.stream.XMLStreamConstants.*;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 */
public class XMLStreamUtils
{
  /**
   * Test if the current event is a element start tag with the given namespace and name.
   * If the namespace URI is null it is not checked for equality,
   * if the local name is null it is not checked for equality.
   * @param reader must not be {@code null}
   * @param uri the namespace uri of the element, may be null
   * @param name the local name of the element, may be null
   * @return true if the required values are matched, false otherwise
   */
  public static boolean isStartElement( XMLStreamReader reader, String uri, String name ) 
  {
    return isElement(reader, START_ELEMENT, uri, name);
  }

  /**
   * Test if the current event is an element end tag with the given namespace and name.
   * If the namespace URI is null it is not checked for equality,
   * if the local name is null it is not checked for equality.
   * @param reader must not be {@code null}
   * @param uri the namespace uri of the element, may be null
   * @param name the local name of the element, may be null
   * @return true if the required values are matched, false otherwise
   */
  public static boolean isEndElement( XMLStreamReader reader, String uri, String name ) 
  {
    return isElement(reader, END_ELEMENT, uri, name);
  }

  /**
   * Test if the current event is an element tag with the given namespace and name.
   * If the namespace URI is null it is not checked for equality,
   * if the local name is null it is not checked for equality.
   * @param reader must not be {@code null}
   * @param event START_ELEMENT or END_ELEMENT
   * @param uri the namespace URI of the element, may be null
   * @param name the local name of the element, may be null
   * @return true if the required values are matched, false otherwise
   */
  private static boolean isElement( XMLStreamReader reader, int event, String uri, String name )
  {
    if (reader.getEventType() != event) return false;
    
    if (uri != null)
    {
      String found = reader.getNamespaceURI();
      if (! found.equals(uri)) return false;
    }
    
    if (name != null)
    {
      String found = reader.getLocalName();
      if (! found.equals(name)) return false;
    }
    
    return true;
  }

  /**
   * Test if the current event is a start element with the given namespace and name.
   * If the namespace URI is null it is not checked for equality,
   * if the local name is null it is not checked for equality.
   * @param reader must not be {@code null}
   * @param uri the namespace uri of the element, may be null
   * @param name the local name of the element, may be null
   * @throws XMLStreamException if the required values are not matched.
   */
  public static void requireStartElement( XMLStreamReader reader, String uri, String name ) 
  throws XMLStreamException
  {
    requireElement(reader, START_ELEMENT, uri, name, "");
  }

  /**
   * Test if the current event is an end element with the given namespace and name.
   * If the namespace URI is null it is not checked for equality,
   * if the local name is null it is not checked for equality.
   * @param reader must not be {@code null}
   * @param uri the namespace URI of the element, may be null
   * @param name the local name of the element, may be null
   * @throws XMLStreamException if the required values are not matched.
   */
  public static void requireEndElement( XMLStreamReader reader, String uri, String name ) 
  throws XMLStreamException
  {
    requireElement(reader, END_ELEMENT, uri, name, "/");
  }

  /**
   * Test if the current event is an element tag with the given namespace and name.
   * If the namespace URI is null it is not checked for equality,
   * if the local name is null it is not checked for equality.
   * @param reader must not be {@code null}
   * @param event START_ELEMENT or END_ELEMENT
   * @param uri the namespace URI of the element, may be null
   * @param name the local name of the element, may be null
   * @param slash "" or "/", for error message
   * @throws XMLStreamException if the required values are not matched.
   */
  private static void requireElement( XMLStreamReader reader, int event, String uri, String name, String slash )
  throws XMLStreamException
  {
    // Note: reader.require(event, uri, name) has a lousy error message
    
    if (reader.getEventType() != event) throw new XMLStreamException("expected <"+slash+name+">", reader.getLocation());
    
    if (uri != null)
    {
      String found = reader.getNamespaceURI();
      if (! found.equals(uri)) throw new XMLStreamException("expected <"+slash+name+"> with namespace ["+uri+"], found ["+found+"]", reader.getLocation());
    }
    
    if (name != null)
    {
      String found = reader.getLocalName();
      if (! found.equals(name)) throw new XMLStreamException("expected <"+slash+name+">, found <"+slash+found+">", reader.getLocation());
    }
  }

  /**
   * Skip current element, including all its content.
   * Precondition: the current event is START_ELEMENT.
   * Postcondition: the current event is the corresponding END_ELEMENT.
   * Similar to {@link XMLStreamReader#nextTag()}, but also skips text content.
   * @param reader must not be {@code null}
   * @throws XMLStreamException if the current event is not START_ELEMENT or there is an error processing the underlying XML source
   */
  public static void skipElement( XMLStreamReader reader )
  throws XMLStreamException
  {
    if (reader.getEventType() != START_ELEMENT) throw new XMLStreamException("expected start of element", reader.getLocation());
    
    int depth = 0;
    while (reader.hasNext())
    {
      int event = reader.next();
      if (event == START_ELEMENT)
      {
        ++depth;
      }
      else if (event == END_ELEMENT)
      {
        --depth;
        if (depth == -1) break;
      }
    }
  }

  /**
   * @param reader
   * @param elem element name, for error message
   * @param attr attribute name
   * @return attribute value, never null or empty
   * @throws XMLStreamException
   */
  public static String requireAttr( XMLStreamReader reader, String elem, String attr )
  throws XMLStreamException
  {
    String str = reader.getAttributeValue(null, attr);
    if (str == null || str.length() == 0) throw new XMLStreamException("expected non-empty attribute ["+attr+"] on element ["+elem+"]", reader.getLocation());
    return str;
  }
}

package org.dbpedia.extraction.sources;

import org.dbpedia.extraction.util.Language;
import org.dbpedia.extraction.wikiparser.*;
import org.dbpedia.util.Exceptions;
import org.dbpedia.util.text.xml.XMLStreamUtils;

import scala.Function1;
import scala.util.control.ControlThrowable;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

public class WikipediaDumpParser
{
  /** the logger */
  private static final Logger logger = Logger.getLogger(WikipediaDumpParser.class.getName());

  /** */
  private static final String ROOT_ELEM = "mediawiki";
  
  /** */
  private static final String SITEINFO_ELEM = "siteinfo";
  
  /** */
  private static final String BASE_ELEM = "base";
  
  /** */
  private static final String PAGE_ELEM = "page";
  
  /** */
  private static final String TITLE_ELEM = "title";
  
  /** */
  private static final String REDIRECT_ELEM = "redirect";

  /** */
  private static final String ID_ELEM = "id";
  
  /** */
  private static final String NS_ELEM = "ns";
  
  /** */
  private static final String REVISION_ELEM = "revision";
  
  /** */
  private static final String TEXT_ELEM = "text";
  
  private static final String TIMESTAMP_ELEM = "timestamp";

  /** the character stream */
  private Reader _stream;

  /** the reader, null before and after run() */
  private XMLStreamReader _reader;
  
  /** 
   * This parser is currently only compatible with the 0.6 format.
   * TODO: make the parser smarter, ignore elements that are not present in older formats.
   */
  private final String _namespace = "http://www.mediawiki.org/xml/export-0.6/";
  
  /**
   * Language used to parse page titles. If null, get language from siteinfo.
   * If given, ignore siteinfo element.
   */
  private Language _language;
  
  /** */
  private final Function1<WikiTitle, Boolean> _filter;
  
  /** page processor, called for each page */
  private final Function1<WikiPage, ?> _processor;

  /**
   * @param stream The character stream. Will be closed after reading. We have to use a Reader instead 
   * of an InputStream because of this bug: https://issues.apache.org/jira/browse/XERCESJ-1257
   * @param namespace expected namespace. If null, namespace is not checked, only local element names.
   * @param language language used to parse page titles. If null, get language from siteinfo.
   * If given, ignore siteinfo element. TODO: use a boolean parameter instead to decide if siteinfo should be used.
   * @param filter page filter. Only matching pages will be processed.
   * @param processor page processor
   */
  public WikipediaDumpParser(Reader stream, String namespace, Language language, Function1<WikiTitle, Boolean> filter, Function1<WikiPage, ?> processor)
  {
    if (stream == null) throw new NullPointerException("file");
    if (processor == null) throw new NullPointerException("processor");
    
    _stream = stream;
    if (namespace != null && ! namespace.equals(_namespace)) throw new IllegalArgumentException("namespace must be ["+_namespace+"] or null, but is ["+namespace+"]");
    // _namespace = namespace;
    _language = language;
    _filter = filter;
    _processor = processor;
  }
  
  public void run()
  throws IOException, XMLStreamException, InterruptedException
  {
    XMLInputFactory factory = XMLInputFactory.newInstance();
    _reader = factory.createXMLStreamReader(_stream);
    try
    {
      readDump();
    }
    finally
    {
      _stream.close();
      _stream = null;
      _reader.close();
      _reader = null;
    }
  }

  private void readDump()
  throws XMLStreamException, InterruptedException
  {
    nextTag();
    // consume <mediawiki> tag
    requireStartElement(ROOT_ELEM);
    nextTag();
    
    if (_language == null) 
    {
      _language = readSiteInfo();
    } 
    else 
    {
      if (isStartElement(SITEINFO_ELEM)) skipElement(SITEINFO_ELEM, true); 
    }
    // now after </siteinfo>
    
    readPages();
    
    requireEndElement(ROOT_ELEM);
  }

  private Language readSiteInfo()
  throws XMLStreamException
  {
    requireStartElement(SITEINFO_ELEM);
    nextTag();

    //Consume <sitename> tag
    skipElement("sitename", true);

    requireStartElement(BASE_ELEM);
    //Read contents of <base>: http://xx.wikipedia.org/wiki/...
    String uri = _reader.getElementText();
    String wikiCode = uri.substring(uri.indexOf("://") + 3, uri.indexOf('.'));
    Language language = Language.apply(wikiCode);
    nextTag();

    //Consume <generator> tag
    skipElement("generator", true);

    //Consume <case> tag
    skipElement("case", true);

    //Consume <namespaces> tag
    // TODO: read namespaces, use them to parse page titles
    skipElement("namespaces", true);

    requireEndElement(SITEINFO_ELEM);
    // now at </siteinfo>
    nextTag();

    return language;
  }
  
  private void readPages()
  throws XMLStreamException, InterruptedException
  {
    while (isStartElement(PAGE_ELEM))
    {
      readPage();
      // now at </page>
      
      nextTag();
    }
  }

  private void readPage()
  throws XMLStreamException
  {
    requireStartElement(PAGE_ELEM);
    nextTag();
    
    //Read title
    requireStartElement(TITLE_ELEM);
    String titleStr = _reader.getElementText();
    WikiTitle title = parseTitle(titleStr);
    _reader.nextTag();
    // now after </title>

    //Skip filtered pages
    if(title == null || ! _filter.apply(title))
    {
        while(! isEndElement(PAGE_ELEM)) _reader.next();
        return;
    }

    int nsCode = (int)requireLong(NS_ELEM, true);
    // now after </ns>
    
    if (title.namespace().code() != nsCode)
    {
      Namespace expected = Namespace.values().apply(nsCode);
      logger.log(Level.WARNING, "Error parsing title: found namespace "+title.namespace()+", expected "+expected+" in title "+titleStr);
    }

    //Read page id
    long pageId = requireLong(ID_ELEM, false);
    // now at </id>

    //Read page
    WikiPage page = null;
    WikiTitle redirect = null;
    while (nextTag() == START_ELEMENT)
    {
      if (isStartElement(REDIRECT_ELEM))
      {
        redirect = parseTitle(_reader.getAttributeValue(null, TITLE_ELEM));
        nextTag();
        // now at </redirect>
      }
      else if (isStartElement(REVISION_ELEM))
      {
        page = readRevision(title, redirect, pageId);
        // now at </revision>
      }
      else
      {
        // skip all other elements, don't care about the name, don't skip end tag
        skipElement(null, false);
      }
    }
    
    if (page != null)
    {
      try
      {
          _processor.apply(page);
      }
      catch (Exception e)
      {
        // emulate Scala exception handling. Ugly...
        if (e instanceof ControlThrowable) throw Exceptions.unchecked(e);
        if (e instanceof InterruptedException) throw Exceptions.unchecked(e);
        else logger.log(Level.WARNING, "Error processing page  " + title, e);
      }
    }
    
    requireEndElement(PAGE_ELEM);
  }

  private WikiPage readRevision(WikiTitle title, WikiTitle redirect, long pageId)
  throws XMLStreamException
  {
    String text = null;
    String timestamp = null;
    long revisionId = -1;
    
    while (nextTag() == START_ELEMENT)
    {
      if (isStartElement(TEXT_ELEM))
      {
        text = _reader.getElementText();
        // now at </text>
      }
      else if (isStartElement(TIMESTAMP_ELEM))
      {
        timestamp = _reader.getElementText();
        // now at </timestamp>
      }
      else if (isStartElement(ID_ELEM))
      {
        revisionId = requireLong(ID_ELEM, false);
        // now at </id>
      }
      else
      {
        // skip all other elements, don't care about the name, don't skip end tag
        skipElement(null, false);
      }
    }
    
    requireEndElement(REVISION_ELEM);
    // now at </revision>
    
    return new WikiPage(title, redirect, pageId, revisionId, timestamp, text);
  }
  
  /* Methods for low-level work. Ideally, only these methods would access _reader while the
   * higher-level methods would only use these.
   */
  
  /**
   * @param name expected name of element. if null, don't check name.
   * @param nextTag should we advance to the next tag after the closing tag of this element?
   * @return null if title cannot be parsed for some reason
   * @throws XMLStreamException
   */
  private WikiTitle parseTitle( String titleString )
  {
    try
    {
        return WikiTitle.parse(titleString, _language);
    }
    catch (Exception e)
    {
      logger.log(Level.WARNING, "Error parsing page title ["+titleString+"]", e);
      return null;
    }
  }
  
  /**
   * @param name expected name of element. if null, don't check name.
   * @param nextTag should we advance to the next tag after the closing tag of this element?
   * @return long value
   * @throws XMLStreamException
   * @throws IllegalArgumentException if element content cannot be parsed as long
   */
  private long requireLong( String name, boolean nextTag ) throws XMLStreamException
  {
    XMLStreamUtils.requireStartElement(_reader, _namespace, name);
    try
    {
      long result = Long.parseLong(_reader.getElementText());
      if (nextTag) _reader.nextTag();
      return result;
    }
    catch (NumberFormatException e)
    {
      throw new IllegalArgumentException("cannot parse content of element ["+name+"] as long", e);
    }
  }
  
  private void skipElement(String name, boolean nextTag) throws XMLStreamException
  {
    XMLStreamUtils.requireStartElement(_reader, _namespace, name); 
    XMLStreamUtils.skipElement(_reader); 
    if (nextTag) _reader.nextTag();
  }
  
  private boolean isStartElement(String name) throws XMLStreamException
  {
    return XMLStreamUtils.isStartElement(_reader, _namespace, name);
  }
  
  private boolean isEndElement(String name) throws XMLStreamException
  {
    return XMLStreamUtils.isEndElement(_reader, _namespace, name);
  }
  
  private void requireStartElement(String name) throws XMLStreamException
  {
    XMLStreamUtils.requireStartElement(_reader, _namespace, name); 
  }
  
  private void requireEndElement(String name) throws XMLStreamException
  {
    XMLStreamUtils.requireEndElement(_reader, _namespace, name); 
  }
  
  private int nextTag() throws XMLStreamException
  {
    return _reader.nextTag();
  }
}

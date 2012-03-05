package org.dbpedia.extraction.sources;

import org.dbpedia.extraction.util.Language;
import org.dbpedia.extraction.wikiparser.WikiTitle;
import org.dbpedia.util.Exceptions;
import scala.Function1;
import scala.Option;
import scala.util.control.ControlThrowable;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static org.dbpedia.util.text.xml.XMLStreamUtils.*;

public class WikipediaDumpParser
{
  /** the logger */
  private static final Logger logger = Logger.getLogger(WikipediaDumpParser.class.getName());

  /** 
   * Note: current namespace URI is "http://www.mediawiki.org/xml/export-0.4/",
   * but older dumps use 0.3, so we just ignore the namespace URI.
   * TODO: make this configurable, or use two different subclasses of this class.
   */
  private static final String MEDIAWIKI_NS = null;
  
  /** */
  private static final String ROOT_ELEM = "mediawiki";
  
  /** */
  private static final String SITEINFO_ELEM = "siteinfo";
  
  /** */
  private static final String PAGE_ELEM = "page";
  
  /** */
  private static final String TITLE_ELEM = "title";
  
  /** */
  private static final String REDIRECT_ELEM = "redirect";

  /** */
  private static final String ID_ELEM = "id";
  
  /** */
  private static final String REVISION_ELEM = "revision";
  
  /** */
  private static final String TEXT_ELEM = "text";

  /** */
  private final InputStream _stream;

  /** */
  private final Function1<WikiPage, ?> _processor;

  private final Function1<WikiTitle, Boolean> _filter;
  
  /** the reader, null before and after run() */
  private XMLStreamReader _reader;
  
  /**
   * @param stream The input stream. Will be closed after reading.
   * @param processor page processor
   */
  public WikipediaDumpParser(InputStream stream, Function1<WikiPage, ?> processor, Function1<WikiTitle, Boolean> filter)
  {
    if (stream == null) throw new NullPointerException("file");
    if (processor == null) throw new NullPointerException("processor");
    
    _stream = stream;
    _processor = processor;
    _filter = filter;
  }
  
  public void run()
  throws IOException, XMLStreamException, InterruptedException
  {
    XMLInputFactory factory = XMLInputFactory.newInstance();

      _reader = factory.createXMLStreamReader(_stream, "UTF-8");
      try
      {
        readDump();
      }
      finally
      {
        _reader.close();
        _reader = null;
      }
  }

  private void readDump()
  throws XMLStreamException, InterruptedException
  {
    _reader.nextTag();
    requireStartElement(_reader, MEDIAWIKI_NS, ROOT_ELEM);

    // consume <mediawiki> tag
    _reader.nextTag();
    
    Language language = readSiteInfo();
    
    readPages(language);
    
    requireEndElement(_reader, MEDIAWIKI_NS, ROOT_ELEM);
  }

  private Language readSiteInfo()
  throws XMLStreamException
  {
    requireStartElement(_reader, MEDIAWIKI_NS, SITEINFO_ELEM);
    _reader.nextTag();

    //Consume <sitename> tag
    skipElement(_reader);
    _reader.nextTag();  

    //Read contents of <base>
    String uri = _reader.getElementText();
    _reader.nextTag();
      
    //Retrieve wiki code
    String wikiCode = uri.substring(uri.indexOf('/') + 2, uri.indexOf('.'));
    Option<Language> langOpt = (wikiCode.toLowerCase().equals("commons")) ? Language.fromWikiCode("en") : Language.fromWikiCode(wikiCode);
    if(langOpt.isEmpty()) throw new  XMLStreamException("Invalid wiki language code: '" + wikiCode + "'");
    Language language = (Language)langOpt.get();

    //Consume <generator> tag
    skipElement(_reader);
    _reader.nextTag();

    //Consume <case> tag
    skipElement(_reader);
    _reader.nextTag();

    //Consume <namespace> tag
    // TODO: read namespaces?
    skipElement(_reader);
    _reader.nextTag();

    // Note: we're now at </siteinfo>
    requireEndElement(_reader, MEDIAWIKI_NS, SITEINFO_ELEM);
    _reader.nextTag();

    return language;
  }
  
  private void readPages(Language lang)
  throws XMLStreamException, InterruptedException
  {
    for (;;)
    {
      if (! isStartElement(_reader, MEDIAWIKI_NS, PAGE_ELEM)) return;
      
      readPage(lang);
      // Note: we're now at </page>
      
      _reader.nextTag();
    }
  }

  private void readPage(Language lang)
  throws XMLStreamException
  {
    // consume <page> tag
    _reader.nextTag();
    
    //Read title
    requireStartElement(_reader, MEDIAWIKI_NS, TITLE_ELEM);
    String titleString = _reader.getElementText();
    _reader.nextTag();

    WikiTitle title = null;
    try
    {
        title = WikiTitle.parse(titleString, lang);
    }
    catch (Exception e)
    {
      logger.log(Level.WARNING, "Error parsing title: " + titleString, e);
    }

    //Skip filtered pages
    if(title == null || !(Boolean)_filter.apply(title))
    {
        while(_reader.getEventType() != END_ELEMENT || !PAGE_ELEM.equals(_reader.getLocalName()) ) _reader.next();
        return;
    }

    //Read page id
    long pageId = Long.parseLong(_reader.getElementText());

    //Read page
    WikiPage page = null;
    while (_reader.nextTag() == START_ELEMENT)
    {
      if (isStartElement(_reader, MEDIAWIKI_NS, REVISION_ELEM))
      {
        page = readRevision(title, pageId);
        // Note: we're now at </revision>
      }
      else
      {
        skipElement(_reader);
      }
    }
    
    // Note: we're now at </page>
    
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
  }

  private WikiPage readRevision(WikiTitle title, long pageId)
  throws XMLStreamException
  {
    String text = null;
    long revisionId = -1;
    boolean redirect = false;
    
    while (_reader.nextTag() == START_ELEMENT)
    {
      if (isStartElement(_reader, MEDIAWIKI_NS, TEXT_ELEM))
      {
        text = _reader.getElementText();
        // Note: we're now at </text>
      }
      else if (isStartElement(_reader, MEDIAWIKI_NS, REDIRECT_ELEM))
      {
        redirect = true;
        skipElement(_reader);
        // Note: we're now at </redirect>
      }
      else if (isStartElement(_reader, MEDIAWIKI_NS, ID_ELEM))
      {
        revisionId = Long.parseLong(_reader.getElementText());
        // Note: we're now at </id>
      }
      else
      {
        skipElement(_reader);
      }
    }
    
    // Note: we're now at </revision>
    
    return redirect ? null : new WikiPage(title, pageId, revisionId, text);
  }
}

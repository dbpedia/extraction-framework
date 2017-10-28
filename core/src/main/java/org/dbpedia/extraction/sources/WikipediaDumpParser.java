package org.dbpedia.extraction.sources;

import org.dbpedia.extraction.config.DefaultEntry;
import org.dbpedia.extraction.config.RecordCause;
import org.dbpedia.extraction.util.Language;
import org.dbpedia.extraction.wikiparser.Namespace;
import org.dbpedia.extraction.wikiparser.WikiPage;
import org.dbpedia.extraction.wikiparser.WikiTitle;
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces;
import org.dbpedia.util.Exceptions;
import org.dbpedia.util.text.xml.XMLStreamUtils;
import scala.Enumeration;
import scala.Function1;
import scala.Tuple3;
import scala.util.control.ControlThrowable;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

public class WikipediaDumpParser
{
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
  private static final String CONTRIBUTOR_ELEM = "contributor";
  private static final String CONTRIBUTOR_ID = "id";
  private static final String CONTRIBUTOR_IP = "ip";
  private static final String CONTRIBUTOR_NAME = "username";

  /** */
  private static final String TEXT_ELEM = "text";
  
  private static final String TIMESTAMP_ELEM = "timestamp";

  private static final String FORMAT_ELEM = "format";

  /** the character stream */
  private Reader _stream;

  /** the reader, null before and after run() */
  private XMLStreamReader _reader;
  
  /** 
   * This parser is currently only compatible with the 0.8 format.
   * TODO: make the parser smarter, ignore elements that are not present in older formats.
   */
  private final String _namespace = null;  //"http://www.mediawiki.org/xml/export-0.8/";
  
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
   * @param language language used to parse page titles. If null, get language from siteinfo.
   * If given, ignore siteinfo element. TODO: use a boolean parameter instead to decide if siteinfo should be used.
   * @param filter page filter. Only matching pages will be processed.
   * @param processor page processor
   */
  public WikipediaDumpParser(Reader stream, Language language, Function1<WikiTitle, Boolean> filter, Function1<WikiPage, ?> processor)
  {
    if (stream == null) throw new NullPointerException("file");
    if (processor == null) throw new NullPointerException("processor");
    
    _stream = stream;
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

    // Consume <sitename> tag
    skipElement("sitename", true);

    // Read contents of <base>: http://xx.wikipedia.org/wiki/...
    String uri = readString(BASE_ELEM, true);
    String wikiCode = uri.substring(uri.indexOf("://") + 3, uri.indexOf('.'));
    Language language = Language.apply(wikiCode);

    // Consume <generator> tag
    // TODO: read MediaWiki version from generator element
    skipElement("generator", true);

    // Consume <case> tag
    skipElement("case", true);

    // Consume <namespaces> tag
    // TODO: read namespaces, use them to parse page titles?
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
  throws XMLStreamException, InterruptedException
  {
    //record any error or warning
    ArrayList<Tuple3<String, Throwable, scala.Enumeration.Value>> records = new ArrayList<>();

    requireStartElement(PAGE_ELEM);
    nextTag();
    
    //Read title
    String titleStr = readString(TITLE_ELEM, true);
    // now after </title>

    int nsCode;
    try
    {
      nsCode = Integer.parseInt(readString(NS_ELEM, true));
    }
    catch (NumberFormatException e)
    {
      throw new IllegalArgumentException("cannot parse content of element ["+NS_ELEM+"] as int", e);
    }

    //Read page id
    String pageId = readString(ID_ELEM, false);
    // now at </id>

    //create title now with pageId

      WikiTitle title = null;
      try
      {
          title = parseTitle(titleStr, pageId);
      }
      catch (Exception e)
      {
          records.add(new Tuple3<String, Throwable, Enumeration.Value>("Error parsing page title " + titleStr, e, RecordCause.Warning()));
          //logger.log(Level.WARNING, _language.wikiCode() + ": error parsing page title ["+titleString+"]: "+Exceptions.toString(e, 200));
      }


    // now after </ns>

    if (title != null && title.namespace().code() != nsCode)
    {
      try
      {
        Namespace expNs = new Namespace(nsCode, Namespaces.names(_language).get(nsCode).get(), false);
        records.add(new Tuple3<String, Throwable, Enumeration.Value>("Error parsing title: found namespace " + title.namespace() + ", expected " + expNs + " in title " + titleStr, null, RecordCause.Info()));
        //logger.log(Level.WARNING, _language.wikiCode() + ": Error parsing title: found namespace " + title.namespace() + ", expected " + expNs + " in title " + titleStr);
        title.otherNamespace_$eq(expNs);
      }
      catch (NoSuchElementException e)
      {
        records.add(new Tuple3<String, Throwable, Enumeration.Value>(String.format("Error parsing title: found namespace %s, title %s , key %s", title.namespace(),titleStr, nsCode), e, RecordCause.Warning()));
        //logger.log(Level.WARNING, String.format(_language.wikiCode() + ": Error parsing title: found namespace %s, title %s , key %s", title.namespace(),titleStr, nsCode));
        skipTitle();
        return;
      }
    }

    //Skip bad titles and filtered pages
    if (title == null || ! _filter.apply(title))
    {
      skipTitle();
      return;
    }

    //Read page
    WikiPage page = null;
    //WikiTitle redirect = null;
    while (nextTag() == START_ELEMENT)
    {
      if (isStartElement(REDIRECT_ELEM))
      {
        String titleString = _reader.getAttributeValue(null, TITLE_ELEM);
/*        try
        {
          redirect = parseTitle(titleString, null);
        }
        catch (Exception e)
        {
          records.add(new Tuple3<String, Throwable, Enumeration.Value>("Error parsing page title " + titleString, e, RecordSeverity.Warning()));
        }*/
        nextTag();
        // now at </redirect>
      }
      else if (isStartElement(REVISION_ELEM))
      {
        page = readRevision(title, pageId);
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
      for(Tuple3<String, Throwable, scala.Enumeration.Value> record : records){
        page.addExtractionRecord(new DefaultEntry(record._1(), RecordCause.Warning(), record._2(), null));
      }
      try
      {
          _processor.apply(page);
      }
      catch (Exception e)
      {
        // emulate Scala exception handling. Ugly...
        if (e instanceof ControlThrowable) throw Exceptions.unchecked(e);
        if (e instanceof InterruptedException) throw (InterruptedException)e;
        else page.addExtractionRecord(new DefaultEntry("Could not process page: " + page.title().encoded(), RecordCause.Warning(), e, null));
      }
    }
    requireEndElement(PAGE_ELEM);
  }

  private void skipTitle() throws XMLStreamException {
    while(! isEndElement(PAGE_ELEM)) _reader.next();
  }

  private WikiPage readRevision(WikiTitle title, String pageId)
  throws XMLStreamException
  {
    String text = null;
    String timestamp = null;
    String revisionId = null;
    String contributorID = null;
    String contributorName = null;
    String format = null;
    
    while (nextTag() == START_ELEMENT)
    {
      if (isStartElement(TEXT_ELEM))
      {
        text = readString(TEXT_ELEM, false);
        // now at </text>
      }
      else if (isStartElement(TIMESTAMP_ELEM))
      {
        timestamp = readString(TIMESTAMP_ELEM, false);
        // now at </timestamp>
      }
      else if (isStartElement(ID_ELEM))
      {
        revisionId = readString(ID_ELEM, false);
        // now at </id>
      }
      else if (isStartElement(CONTRIBUTOR_ELEM))
      {
        // Check if this is an empty (deleted) contributor tag (i.e. <contributor deleted="deleted" /> )
        // which has no explicit </contributor> end element. If it is - skip it.
        String deleted = _reader.getAttributeValue(null, "deleted");
        if (deleted != null && deleted.equals("deleted")) {
          nextTag();
        } else {
          // now at <contributor>, move to next tag
          nextTag();
          // now should have ip / (author & id), when ip is present we don't have author / id
          // TODO Create a getElementName function to make this cleaner
          if (isStartElement(CONTRIBUTOR_IP)) {
            contributorID = "0";
            contributorName = readString(CONTRIBUTOR_IP, false);
          }
          else
          {
            // usually we have contributor name first but we have to check
            if (isStartElement(CONTRIBUTOR_NAME))
            {
              contributorName = readString(CONTRIBUTOR_NAME, false);
              nextTag();
              if (isStartElement(CONTRIBUTOR_ID))
                contributorID = readString(CONTRIBUTOR_ID, false);
            }
            else
            {
              // when contributor ID is first
              if (isStartElement(CONTRIBUTOR_ID))
              {
                contributorID = readString(CONTRIBUTOR_ID, false);
                nextTag();
                if (isStartElement(CONTRIBUTOR_NAME))
                  contributorName = readString(CONTRIBUTOR_NAME, false);
              }
            }
          }
          nextTag();
          requireEndElement(CONTRIBUTOR_ELEM);
        }
      }
      else if (isStartElement(FORMAT_ELEM)) {
          format = readString(FORMAT_ELEM, false);
          // now at </format>
      }
      else
      {
        // skip all other elements, don't care about the name, don't skip end tag
        skipElement(null, false);
      }
    }
    
    requireEndElement(REVISION_ELEM);
    // now at </revision>
    
    return new WikiPage(title, pageId, revisionId, timestamp, contributorID, contributorName, text, format);
  }
  
  /* Methods for low-level work. Ideally, only these methods would access _reader while the
   * higher-level methods would only use these.
   */
  
  /**
   * @param titleString expected name of element. if null, don't check name.
   * @return null if title cannot be parsed for some reason
   */
  private WikiTitle parseTitle( String titleString, String pageId )
  {
    Long id = null;
    if(pageId != null) {
        try {
            id = Long.parseLong(pageId);
        } catch (Throwable e) {
        }
    }

    return WikiTitle.parseCleanTitle(titleString, _language, scala.Option.apply(id));
  }

  /**
   * @param name expected name of element. if null, don't check name.
   * @param nextTag should we advance to the next tag after the closing tag of this element?
   * @return long value
   * @throws XMLStreamException
   * @throws IllegalArgumentException if element content cannot be parsed as long
   */
  private String readString( String name, boolean nextTag ) throws XMLStreamException
  {
    XMLStreamUtils.requireStartElement(_reader, _namespace, name);
    String text = _reader.getElementText();
    if (nextTag) _reader.nextTag();
    return text;
  }

  private void skipElement(String name, boolean nextTag) throws XMLStreamException
  {
    XMLStreamUtils.requireStartElement(_reader, _namespace, name); 
    XMLStreamUtils.skipElement(_reader); 
    if (nextTag) _reader.nextTag();
  }
  
  private boolean isStartElement(String name)
  {
    return XMLStreamUtils.isStartElement(_reader, _namespace, name);
  }
  
  private boolean isEndElement(String name)
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

package org.dbpedia.extraction.sources;

import org.apache.jena.base.Sys;
import org.dbpedia.extraction.util.Language;
import org.dbpedia.extraction.util.RecordSeverity;
import org.dbpedia.extraction.wikiparser.*;
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

public class WikipediaDumpParserHistory
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

  /** NEW PARAMS */
  private static final String REVISION_MINOR_UPDATE = "minor";
  private static final String REVISION_PARENT_ID = "parentid";
  private static final String REVISION_COMMENT = "comment";
  private static final String REVISION_ID = "id";

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
  private final Function1<WikiPageWithRevisions, ?> _processor;

  /**
   * @param stream The character stream. Will be closed after reading. We have to use a Reader instead
   * of an InputStream because of this bug: https://issues.apache.org/jira/browse/XERCESJ-1257
   * @param language language used to parse page titles. If null, get language from siteinfo.
   * If given, ignore siteinfo element. TODO: use a boolean parameter instead to decide if siteinfo should be used.
   * @param filter page filter. Only matching pages will be processed.
   * @param processor page processor
   */
  public WikipediaDumpParserHistory(Reader stream, Language language, Function1<WikiTitle, Boolean> filter, Function1<WikiPageWithRevisions, ?> processor)
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
    System.out.println("BEFORE READ PAGES");
    readPages();
    System.out.println("AFTER READ PAGES");

    requireEndElement(ROOT_ELEM);

    System.out.println("EN READ PAGES");
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
    System.out.println("readPages");
    while (isStartElement(PAGE_ELEM))
    {
      System.out.println("XXXXXXXXXXXX NEW PAGE");
      readPage();
      // now at </page>
      System.out.println("XXXXXXXXXXXX NEXT PAGE");
      nextTag();
    }

    System.out.println("readPagesend");
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

    Integer last_size=0;
    WikiTitle title = null;
    try
    {
      title = parseTitle(titleStr, pageId);
    }
    catch (Exception e)
    {
      records.add(new Tuple3<String, Throwable, Enumeration.Value>("Error parsing page title " + titleStr, e, RecordSeverity.Warning()));
      //logger.log(Level.WARNING, _language.wikiCode() + ": error parsing page title ["+titleString+"]: "+Exceptions.toString(e, 200));
    }


    // now after </ns>

    if (title != null && title.namespace().code() != nsCode)
    {
      try
      {
        Namespace expNs = new Namespace(nsCode, Namespaces.names(_language).get(nsCode).get(), false);
        records.add(new Tuple3<String, Throwable, Enumeration.Value>("Error parsing title: found namespace " + title.namespace() + ", expected " + expNs + " in title " + titleStr, null, RecordSeverity.Info()));
        //logger.log(Level.WARNING, _language.wikiCode() + ": Error parsing title: found namespace " + title.namespace() + ", expected " + expNs + " in title " + titleStr);
        title.otherNamespace_$eq(expNs);
      }
      catch (NoSuchElementException e)
      {
        records.add(new Tuple3<String, Throwable, Enumeration.Value>(String.format("Error parsing title: found namespace %s, title %s , key %s", title.namespace(),titleStr, nsCode), e, RecordSeverity.Warning()));
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
    WikiPageWithRevisions page = null;
    WikiTitle redirect = null;

    System.out.println("> PAGE TITLE : "+title);
    System.out.println("> PAGE ID : "+pageId);
    //Read page
    //Long lastRev_id=0;
    ArrayList<RevisionNode> RevisionList = new ArrayList<>();
    while (nextTag() == START_ELEMENT)
    {
      if (isStartElement(REDIRECT_ELEM))
      {
        String titleString = _reader.getAttributeValue(null, TITLE_ELEM);
        try
        {
          redirect = parseTitle(titleString, null);
        }
        catch (Exception e)
        {
          records.add(new Tuple3<String, Throwable, Enumeration.Value>("Error parsing page title " + titleString, e, RecordSeverity.Warning()));
        }
        nextTag();
        // now at </redirect>
      }
      else if (isStartElement(REVISION_ELEM))
      {

        RevisionNode current_revision= null;

        current_revision= readRevision(title, redirect, pageId,last_size);
        RevisionList.add(current_revision);
        last_size=current_revision.text_size();
        //page = readRevision(title, redirect, pageId);
        // now at </revision>
      }
      else
      {
        // skip all other elements, don't care about the name, don't skip end tag
        skipElement(null, false);
      }
    }

    page = new WikiPageWithRevisions(title, redirect, pageId, "", "", "", "", "", "",RevisionList);
    System.out.println(page);
    if (page != null)
    {
      for(Tuple3<String, Throwable, scala.Enumeration.Value> record : records){
        page.addExtractionRecord(record._1(), record._2(), record._3());
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
        else page.addExtractionRecord("Could not process page: " + page.title().encoded(), e, RecordSeverity.Warning());
        System.out.println("ERRROR");
      }
    }
    requireEndElement(PAGE_ELEM);
  }


  private void skipTitle() throws XMLStreamException {
    while(! isEndElement(PAGE_ELEM)) _reader.next();
  }

  private RevisionNode readRevision(WikiTitle title, WikiTitle redirect, String pageId, Integer last_size)
          throws XMLStreamException
  {
    String revision_id = "";
    String parent_id= "";
    String timestamp= "";
    String contributorID= "";
    String contributorName= "";
    String contributorDeleted="false";
    String contributorIP= "";
    String comment= "";
    String text_size= "";
    String format= null;
    String minor_edit= "false";
    Integer text_delta= 0;

    while (nextTag() == START_ELEMENT)
    {
     // System.out.println("LOOP BEGIND");
     if (isStartElement(TEXT_ELEM))
      {
        String deleted = _reader.getAttributeValue(null, "bytes");
        text_size = _reader.getAttributeValue(null, "bytes");
        text_delta = Integer.parseInt(text_size) - last_size;
        //System.out.println(text_size);
        skipElement(null, false);
        // now at </text>

      }
      else if (isStartElement(TIMESTAMP_ELEM))
      {
        timestamp = readString(TIMESTAMP_ELEM, false);
        //System.out.println(">timestamp : "+timestamp);
        // now at </timestamp>
      }
      else if (isStartElement(REVISION_ID))
      {

        revision_id = readString(REVISION_ID, false);
        //System.out.println(">revision_id : "+revision_id);

        // now at </id>
      }
      else if (isStartElement(REVISION_PARENT_ID))
      {

        parent_id = readString(REVISION_PARENT_ID, false);
        //System.out.println(">parent_id : "+parent_id);

        // now at </id>
      } else if (isStartElement(REVISION_COMMENT))
      {

        comment = readString(REVISION_COMMENT, false);
        //System.out.println(">comment : "+comment);

        // now at </id>
      }else if ( isStartElement(REVISION_MINOR_UPDATE))
      {

        minor_edit = "true";
        //System.out.println(">minor_edit : "+minor_edit);

        skipElement(null, false);


        // now at </id>
      }
      else if (isStartElement(CONTRIBUTOR_ELEM))
      {
        // Check if this is an empty (deleted) contributor tag (i.e. <contributor deleted="deleted" /> )
        // which has no explicit </contributor> end element. If it is - skip it.
        String deleted = _reader.getAttributeValue(null, "deleted");
        if (deleted != null && deleted.equals("deleted")) {
          contributorDeleted = "true";
          //System.out.println(">contributorDeleted : "+contributorDeleted);
          nextTag();
        } else {
          // now at <contributor>, move to next tag
          nextTag();
          // now should have ip / (author & id), when ip is present we don't have author / id
          // TODO Create a getElementName function to make this cleaner
          if (isStartElement(CONTRIBUTOR_IP)) {
            //  System.out.println(">contributor CASE 0");
            contributorIP = readString(CONTRIBUTOR_IP, false);
            //  System.out.println(">contributorIP : "+contributorIP);
          }
          else
          {
            // usually we have contributor name first but we have to check
            if (isStartElement(CONTRIBUTOR_NAME))
            {
              //  System.out.println(">contributor CASE 1");
              contributorName = readString(CONTRIBUTOR_NAME, false);
              nextTag();
              // System.out.println(">contributorName : "+contributorName);
              if (isStartElement(CONTRIBUTOR_ID))
                contributorID = readString(CONTRIBUTOR_ID, false);
              // System.out.println(">contributorID : "+contributorID);
            }
            else
            {
              // when contributor ID is first
              if (isStartElement(CONTRIBUTOR_ID))
              {
                //  System.out.println(">contributor CASE 2");
                contributorID = readString(CONTRIBUTOR_ID, false);
                // System.out.println(">contributorID : "+contributorID);
                nextTag();
                if (isStartElement(CONTRIBUTOR_NAME))
                  contributorName = readString(CONTRIBUTOR_NAME, false);
                // System.out.println(">contributorName : "+contributorName);
              }
            }
          }
          nextTag();
          requireEndElement(CONTRIBUTOR_ELEM);
        }
      }
      else if (isStartElement(FORMAT_ELEM)) {
        format = readString(FORMAT_ELEM, false);
       //  System.out.println(">format : "+format);
        // now at </format>
      }
      else {

       /// skip all other elements, don't care about the name, don't skip end tag
        skipElement(null, false);
      }
      //System.out.println("LOOP END");
    }
    //System.out.println("AFTER LOOP ");
    requireEndElement(REVISION_ELEM);

    // System.out.println(">> End readRevision ");
    // now at </revision>

    String pageUri=title.pageIri() + "?" + "oldid=" + revision_id + "&"  + "ns=" + title.namespace().code();
    String parent_Uri=title.pageIri() + "?" + "oldid=" + parent_id + "&"  + "ns=" + title.namespace().code();
    System.out.println("=========================");
    System.out.println(pageUri);
    System.out.println("=========================");
    return new RevisionNode(revision_id,pageUri,parent_Uri,timestamp,contributorID,contributorName,contributorIP,contributorDeleted,comment,format,text_size,minor_edit,text_delta);
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

package org.dbpedia.extraction.live.feeder;

import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.core.Util;
import org.dbpedia.extraction.live.helper.MappingAffectedPagesHelper;
import org.dbpedia.extraction.live.transformer.NodeToRecordTransformer;
import org.dbpedia.extraction.live.util.*;
import org.dbpedia.extraction.sources.LiveExtractionXMLSource;
import org.w3c.dom.Document;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
//import scala.xml.*;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 28, 2010
 * Time: 2:38:54 PM
 * This class detects the update in mappings and then gets a list of all page IDs that are affected by a change in the
 * mapping and feeds those page IDs to a common priority pageQueue for further processing
 */
public class MappingUpdateFeeder extends Thread{

    private static Logger logger = Logger.getLogger(MappingUpdateFeeder.class);
    
    public static String lastResponseDateFile = "./MappingUpdateLastResponseDate.dat";
    private String startDate = "2010-07-01T15:00:00Z";

    public MappingUpdateFeeder (String name, int priority) {

        String proposedStartDate = LastResponseDateManager.getLastResponseDate(lastResponseDateFile);
        if(!Util.isStringNullOrEmpty(proposedStartDate))
           startDate = proposedStartDate;

        this.setPriority(priority);
        this.setName(name);
        this.start();
    }


    public MappingUpdateFeeder(String name){
        this(name, Thread.NORM_PRIORITY);
    }

    public MappingUpdateFeeder(){
        this("MappingUpdateFeeder", Thread.NORM_PRIORITY);
    }

    public void run(){

        String mappingsOAIUri = "http://mappings.dbpedia.org/index.php/Special:OAIRepository";
        String baseWikiUri = "http://mappings.dbpedia.org/wiki/";
        String oaiPrefix = "oai:en.wikipedia.org:enwiki:";


        int pollInterval = 2;
        int sleepInterval = 1;

        Calendar calendar = new GregorianCalendar();
        calendar.set(2010, 07, 1, 22, 0, 0);

         // Create an iterator which keeps polling the OAIRepository
        Iterator<Document> recordIterator =
            OAIUtil.createEndlessRecordIterator(mappingsOAIUri, startDate, pollInterval * 1000, sleepInterval * 1000);

        while(recordIterator.hasNext()){
            try{
                Document doc = recordIterator.next();

                NodeToRecordTransformer transformer = new NodeToRecordTransformer(baseWikiUri, mappingsOAIUri, oaiPrefix);

                scala.xml.Node element = scala.xml.XML.loadString(XMLUtil.toString(doc));
                org.dbpedia.extraction.sources.Source wikiPageSource = LiveExtractionXMLSource.fromXML((scala.xml.Elem)element);

                //Last modification date of the mapping
                String lastResponseDate = XMLUtil.getPageModificationDate(doc);
                MappingAffectedPagesHelper.GetMappingPages(wikiPageSource, lastResponseDate);

                //System.out.println(lastResponseDate);
//                Traversable<WikiPage> trav = wikiPageSource;
//                Iterable<WikiPage> iter = JavaConversions.asIterable(trav);
//                for(WikiPage CurrentWikiPage :iter){
//                    WikiTitle mappingTitle = WikiTitle.parseEncoded(CurrentWikiPage.toString(), Language.Default);
//                    System.out.println(mappingTitle);
//                }
                

            }
            catch(Exception exp){
                logger.error(ExceptionUtil.toString(exp));

            }
        }

    }
}

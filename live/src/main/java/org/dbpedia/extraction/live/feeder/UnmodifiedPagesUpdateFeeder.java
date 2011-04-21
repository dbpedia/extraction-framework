package org.dbpedia.extraction.live.feeder;

import org.apache.log4j.Logger;
import org.apache.xerces.parsers.DOMParser;
import org.dbpedia.extraction.live.main.Main;
import org.dbpedia.extraction.live.priority.PagePriority;
import org.dbpedia.extraction.live.util.LastResponseDateManager;
import org.dbpedia.extraction.live.util.XMLUtil;
import org.dbpedia.extraction.live.util.iterators.OAIUnmodifiedRecordIterator;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: 4/20/11
 * Time: 9:31 PM
 * This class is responsible for getting the pages that were not changed for long time, and place them into the common queue for processing.
 * The pages that were not modified for long period should also be reprocessed, as there may be change in an Extractor that affects those pages.
 * So they should be reprocessed.
 */
public class UnmodifiedPagesUpdateFeeder extends Thread{

    private static Logger logger = Logger.getLogger(UnmodifiedPagesUpdateFeeder.class);

//    String oaiUri = "http://en.wikipedia.org/wiki/Special:OAIRepository";
    String oaiUri = "http://live.dbpedia.org/syncwiki/Special:OAIRepository";
    Calendar calendar = new GregorianCalendar();


    String endDate = "2011-04-01T15:00:00Z";
    String startDate = "2011-03-01T15:00:00Z";

    int pollInterval = 30;
    int sleepInterval = 1;
    public static String lastResponseDateFile = "./lastResponseDate.dat";

    int articleDelay = 0;
    boolean articleRenewal = false;

//    String baseWikiUri = "http://en.wikipedia.org/wiki/";
//    String oaiPrefix = "oai:en.wikipedia.org:enwiki:";
    String baseWikiUri = "http://live.dbpedia.org/syncwiki/";
    String oaiPrefix = "oai:live.dbpedia.org:dbpediawiki:";

    //This parser is used to parse the XML text generated from the page, in order to get the PageID
    DOMParser parser = new DOMParser();

//    public LiveUpdateFeeder(){
//        super("Live extraction feeder");
//
//        start();
//
//    }

    public UnmodifiedPagesUpdateFeeder(String name, int priority){

        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        this.setPriority(priority);
        this.setName(name);
        calendar.set(2011, 04, 01, 22, 0, 0);

        String proposedStartDate = LastResponseDateManager.getLastResponseDate(lastResponseDateFile);
        //Here we should do the opposite of normal iterator, so the endDate will be the lastResponseDate
        if(proposedStartDate != null)
           endDate = proposedStartDate;

        Date dtEndDate;

        try{

            dtEndDate = dateFormatter.parse(endDate);
        }
        catch(Exception exp){
            logger.warn("End date cannot be parsed, considering end date equal to one month ago");
            dtEndDate = new Date();
        }

        //The end date should be reduced by one month, and start date will be one month more prior to last processing date
        calendar.setTime(dtEndDate);
        calendar.add(Calendar.MONTH, -1);

        //The end date should be one month prior to the date of last processing date
        endDate = dateFormatter.format(calendar.getTime());

        //The start date will one more month prior to the last processing date
        calendar.add(Calendar.MONTH, -1);

        Date dtStartDate =  calendar.getTime();

        //Format start date with the format suitable for Harvester
        startDate = dateFormatter.format(dtStartDate);


        start();
    }

    public UnmodifiedPagesUpdateFeeder(String name){
        this(name, Thread.MIN_PRIORITY);
    }

    public UnmodifiedPagesUpdateFeeder(){
        this("UnmodifiedPagesUpdateFeeder", Thread.MIN_PRIORITY);
    }


    public void run() {

        //oaiUri, startDate, pollInterval * 1000, sleepInterval * 1000);
        Iterator<Document> myTestIterator = new OAIUnmodifiedRecordIterator(
                oaiUri, startDate, endDate);

        while(myTestIterator.hasNext()){
            Document doc = myTestIterator.next();
            NodeList nodes = doc.getElementsByTagName("identifier");

            //The document contains 50 pages at once, so we should iterate through them in sequence
            for(int i=0; i < nodes.getLength(); i++){
                String strFullPageIdentifier = nodes.item(i).getChildNodes().item(0).getNodeValue();
                String startDate = XMLUtil.getPageModificationDate(doc);

                int colonPos = strFullPageIdentifier.lastIndexOf(":");
                String strPageID = strFullPageIdentifier.substring(colonPos+1);

                long pageID = new Long(strPageID);

                String lastResponseDate = XMLUtil.getPageModificationDate(doc);

                Main.pageQueue.add(new PagePriority(pageID, true, lastResponseDate));

                //We should check first if  pageID exists, as if it does not exist then it will be added, if it exists before either with same or higher
                //priority then it will not be added
                if(!Main.existingPagesTree.containsKey(pageID))
                    Main.existingPagesTree.put(pageID, false);//Also insert it into the TreeMap, so it will not be double-processed

            }

        }

        //After placing all page IDs to the queue, this thread should sleep for a month, before it's up again to get ne pages
        try{
            TimeUnit.DAYS.sleep(30);
        }
        catch (InterruptedException exp){
            logger.warn("Thread of UnmodifiedPagesUpdateFeeder cannot be stopped for one month");
        }
    }

}

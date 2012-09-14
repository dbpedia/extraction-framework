package org.dbpedia.extraction.live.feeder;


import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.main.Main;
import org.dbpedia.extraction.live.priority.PagePriority;
import org.dbpedia.extraction.live.priority.Priority;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.dbpedia.extraction.live.util.Files;
import org.dbpedia.extraction.live.util.OAIUtil;
import org.dbpedia.extraction.live.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.Iterator;


/**
 * Created with IntelliJ IDEA.
 * User: Dimitris Kontokostas
 * Date: 9/6/12
 * Time: 1:07 PM
 * This is a general purpose OAI Feeder. It can be used as is for simple operations (live feeder, unmodified pages feeder)
 * or subclass and override "handleFeedItem" function
 */
public class OAIFeeder extends Thread {
    protected static Logger logger;
    protected String feederName;
    protected int threadPriority;
    protected Priority queuePriority;

    protected String oaiUri;
    protected String oaiPrefix;
    protected String baseWikiUri;

    protected long pollInterval;              // in miliseconds
    protected long sleepInterval;             // in miliseconds

    protected String defaultStartDateTime;    //"2011-04-01T15:00:00Z";
    protected long relativeEndFromNow;        // in miliseconds

    protected File lastResponseFile;
    protected String latestResponseDate;

    protected Iterator<Document> oaiRecordIterator;
    private volatile boolean keepRunning = true;

    public OAIFeeder(String feederName, int threadPriority, Priority queuePriority,
                     String oaiUri, String oaiPrefix, String baseWikiUri,
                     long pollInterval, long sleepInterval, String defaultStartDateTime, long relativeEndFromNow,
                     String folderBasePath) {

        this.feederName = feederName;
        logger = Logger.getLogger(feederName);
        this.threadPriority = threadPriority;
        this.setPriority(threadPriority);
        this.queuePriority = queuePriority;

        this.oaiUri = oaiUri;
        this.oaiPrefix = oaiPrefix;
        this.baseWikiUri = baseWikiUri;

        this.pollInterval = pollInterval;
        this.sleepInterval = sleepInterval;

        this.defaultStartDateTime = defaultStartDateTime;   //"2011-04-01T15:00:00Z";
        this.relativeEndFromNow = relativeEndFromNow;

        lastResponseFile = new File(folderBasePath + feederName + ".dat");

        getLastResponseDate();
        oaiRecordIterator = OAIUtil.createEndlessRecordIterator(oaiUri, latestResponseDate, relativeEndFromNow, pollInterval, sleepInterval);
    }

    protected String getLastResponseDate() {
        latestResponseDate = defaultStartDateTime;
        try {
            if (!lastResponseFile.exists()) {
                //lastResponseFile.mkdirs();
                lastResponseFile.createNewFile();
            } else {
                latestResponseDate = Files.readFile(lastResponseFile).trim();
                if (latestResponseDate == "")
                    latestResponseDate = defaultStartDateTime;
            }
        } catch (Exception exp) {
            logger.error(ExceptionUtil.toString(exp));
        }
        logger.warn("Resuming from date: " + latestResponseDate);
        return latestResponseDate;
    }

    protected synchronized void setLastResponseDate(String responseDate) {
        Files.createFile(lastResponseFile, responseDate);
    }

    public void startFeeder() {
        // it can start only once
        if (keepRunning == true) {
            start();
        }
    }

    public void stopFeeder() {
        keepRunning = false;
        setLastResponseDate(latestResponseDate);
    }

    protected String getPageModificationDate(Document doc) {
        return XMLUtil.getPageModificationDate(doc);
    }

    protected long getPageID(Document doc) {

        NodeList nodes = doc.getElementsByTagName("identifier");
        String strFullPageIdentifier = nodes.item(0).getChildNodes().item(0).getNodeValue();
        int colonPos = strFullPageIdentifier.lastIndexOf(":");
        String strPageID = strFullPageIdentifier.substring(colonPos + 1);

        return new Long(strPageID);
    }

    protected void addPageIDtoQueue(long pageID, String modificationDate) {

        Main.pageQueue.add(new PagePriority(pageID, queuePriority, modificationDate));

        // We should check first if  pageID exists, as if it does not exist then it will be added, if it exists before either with same or higher
        // priority then it will not be added
        if (!Main.existingPagesTree.containsKey(pageID))
            Main.existingPagesTree.put(pageID, false);//Also insert it into the TreeMap, so it will not be double-processed
    }

    public void run() {
        long count = 0;
        while (keepRunning && oaiRecordIterator.hasNext()) {
            try {
                handleFeedItem(oaiRecordIterator.next());

                // Do not write in every iteration, too much I/O...
                // Write every XXX for backup in case of unhandled exit
                if (++count == 100) {
                    setLastResponseDate(latestResponseDate);
                    count = 0;
                }
            } catch (Exception exp) {
                logger.error(ExceptionUtil.toString(exp));
            }

        }
        stopFeeder();
    }

    /* This function should be overwritten by sub classes */
    protected void handleFeedItem(Document doc) {

        long pageID = getPageID(doc);
        String tmpDate = getPageModificationDate(doc);
        if (tmpDate != null || tmpDate != "")
            latestResponseDate = tmpDate;

        addPageIDtoQueue(pageID, latestResponseDate);
    }
}

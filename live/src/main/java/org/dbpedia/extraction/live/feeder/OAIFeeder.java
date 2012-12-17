package org.dbpedia.extraction.live.feeder;


import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.queue.LiveQueue;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.dbpedia.extraction.live.util.Files;
import org.dbpedia.extraction.live.util.OAIUtil;

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
    protected LiveQueuePriority queuePriority;

    protected String oaiUri;
    protected String oaiPrefix;
    protected String baseWikiUri;

    protected long pollInterval;              // in miliseconds
    protected long sleepInterval;             // in miliseconds

    protected String defaultStartDateTime;    //"2011-04-01T15:00:00Z";
    protected long relativeEndFromNow;        // in miliseconds

    protected File lastResponseFile;
    protected String latestResponseDate;

    protected Iterator<LiveQueueItem> oaiRecordIterator;
    private volatile boolean keepRunning = true;

    public OAIFeeder(String feederName, int threadPriority, LiveQueuePriority queuePriority,
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
        oaiRecordIterator = OAIUtil.createEndlessFeederItemIterator(oaiUri, latestResponseDate, relativeEndFromNow, pollInterval, sleepInterval);
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

    protected void addPageIDtoQueue(LiveQueueItem item) {

        LiveQueue.add(item);
        latestResponseDate = item.getModificationDate();
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
    protected void handleFeedItem(LiveQueueItem item) {

        //if (!item.isDeleted())      {
            addPageIDtoQueue(item);
        //} else {
            // TODO page deleted case
        //}
    }
}

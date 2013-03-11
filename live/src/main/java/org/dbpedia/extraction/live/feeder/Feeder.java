package org.dbpedia.extraction.live.feeder;

import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.queue.LiveQueue;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.dbpedia.extraction.live.util.Files;

import java.io.File;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: Dimitris Kontokostas
 * Date: 2/27/13
 * Time: 12:58 PM
 * This is an abstract class for feeders.
 */
public abstract class Feeder extends Thread {

    protected static Logger logger;
    protected String feederName;
    protected LiveQueuePriority queuePriority;

    protected String defaultStartTime;    //"2011-04-01T15:00:00Z";
    protected File latestProcessDateFile;
    protected String latestProcessDate;

    private volatile boolean keepRunning = true;

    public Feeder(String feederName, LiveQueuePriority queuePriority, String defaultStartTime, String folderBasePath) {
        this.feederName = feederName;
        this.setName("Feeder_"+feederName);
        logger = Logger.getLogger(feederName);
        this.queuePriority = queuePriority;

        this.defaultStartTime = defaultStartTime;   //"2011-04-01T15:00:00Z";
        latestProcessDateFile = new File(folderBasePath + feederName + ".dat");
        getLatestProcessedDate();
    }

    public LiveQueuePriority getQueuePriority(){
        return queuePriority;
    }

    /*
    * Starts the feeder (it can only start once
    * */
    public void startFeeder() {
        if (keepRunning == true) {
            start();
        }
    }

    /*
    * Stops the feeder from running gracefully
    * */
    public void stopFeeder(String date) {
        keepRunning = false;
        setLatestProcessedDate(date);
    }

    /*
    * Reads the latest process date from the file location. Reverts to default on error
    * */
    public String getLatestProcessedDate() {
        latestProcessDate = defaultStartTime;
        try {
            if (!latestProcessDateFile.exists()) {
                //latestProcessDateFile.mkdirs();
                setLatestProcessedDate(defaultStartTime);
            } else {
                latestProcessDate = (Files.readFile(latestProcessDateFile)).trim();
            }
        } catch (Exception exp) {
            logger.error(ExceptionUtil.toString(exp));
        }
        if (latestProcessDate.isEmpty()) {
            latestProcessDate = defaultStartTime;
            setLatestProcessedDate(latestProcessDate);
        }
        logger.warn("Resuming from date: " + latestProcessDate);
        return latestProcessDate;
    }

    /*
    * Updates the latest process date to file
    * */
    public synchronized void setLatestProcessedDate(String date) {
        if (date == null || date.equals(""))
            date = latestProcessDate;

        Files.createFile(latestProcessDateFile, date);
    }

    protected abstract List<LiveQueueItem> getNextItems();

    public void run() {
        while (keepRunning) {
            try {
                for (LiveQueueItem item : getNextItems()) {
                    handleFeedItem(item);
                }
            } catch (Exception exp) {
                logger.error(ExceptionUtil.toString(exp));
            }
        }
    }

    /* This function should be overwritten by sub classes */
    protected void handleFeedItem(LiveQueueItem item) {
        addPageIDtoQueue(item);
    }

    protected void addPageIDtoQueue(LiveQueueItem item) {
        item.setStatQueueAdd(-1);
        item.setPriority(this.queuePriority);
        LiveQueue.add(item);
        latestProcessDate = item.getModificationDate();
    }
}

package org.dbpedia.extraction.live.feeder;

import org.dbpedia.extraction.live.main.Main;
import org.dbpedia.extraction.live.queue.LiveQueue;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.dbpedia.extraction.live.util.DateUtil;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.dbpedia.extraction.live.util.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.time.ZonedDateTime;
import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: Dimitris Kontokostas
 * Date: 2/27/13
 * Time: 12:58 PM
 * This is an abstract class for feeders.
 */
public abstract class Feeder extends Thread {

    protected static Logger logger;
    protected final String feederName;
    protected final LiveQueuePriority queuePriority;

    protected final File latestProcessDateFile;
    protected String latestProcessDate;

    private volatile boolean keepRunning = true;

    public Feeder(String feederName, LiveQueuePriority queuePriority, String defaultStartTime, String folderBasePath) {
        this.feederName = feederName;
        this.setName("Feeder_" + feederName);
        logger = LoggerFactory.getLogger(feederName);
        this.queuePriority = queuePriority;
        latestProcessDateFile = new File(folderBasePath + this.feederName + ".dat");


        /**
         * HANDLING OF STARTING TIME
         */

        // always prioritize file
        String dateFromFile = readLatestProcessDateFile();
        if (validateLatestProcessDate(dateFromFile)) {
            latestProcessDate = dateFromFile;
            logger.info("Prioritizing .dat file " + latestProcessDateFile.getName() + " using " + dateFromFile);
            // then try alternatives from config file
        } else if (validateLatestProcessDate(defaultStartTime)) {
            latestProcessDate = defaultStartTime;
            writeLatestProcessDateFileOrFail(defaultStartTime);
            logger.info(".dat file not found or incorrect value, created file: " + latestProcessDateFile + " with parameter uploaded_dump_date " + defaultStartTime);
        } else {
            logger.error("Neither found " + latestProcessDateFile + "nor correct option 'uploaded_dump_date' (" + defaultStartTime + ")\n" +
                    "Good Bye");
            Main.stopLive();
            System.exit(-1);//checked
        }

        logger.info("Resuming from date: " + latestProcessDate);
    }


    protected abstract void initFeeder();

    /*
     * Starts the feeder (it can only start once
     * */
    public void startFeeder() {
        if (keepRunning) {
            initFeeder();
            start();
            logger.info(feederName + " started");
        }
    }

    /*
     * Stops the feeder from running gracefully
     * */
    public void stopFeeder(String date) {
        keepRunning = false;
        writeLatestProcessDateFileAndLogOnFail(date);
        logger.info("Stopped " + feederName + " and wrote '" + date + "' to " + latestProcessDateFile);
    }


    public void run() {
        while (keepRunning) {
            try {
                for (LiveQueueItem item : getNextItems()) {
                    handleFeedItem(item);
                }
            } catch (java.lang.OutOfMemoryError exp) {
                logger.error(ExceptionUtil.toString(exp), exp);
                throw new RuntimeException("OutOfMemory Error", exp);
            } catch (Exception exp) {
                logger.error(ExceptionUtil.toString(exp), exp);
                // On error re-initiate feeder
                initFeeder();
            }

        }
    }

    protected abstract Collection<LiveQueueItem> getNextItems();


    /* This function should be overwritten by sub classes */
    protected void handleFeedItem(LiveQueueItem item) {
        item.setStatQueueAdd(-1);
        item.setPriority(this.queuePriority);
        LiveQueue.add(item);
        //setLatestProcessDate(item.getModificationDate());
    }



    // throws runtime exception
    public void writeLatestProcessDateFileOrFail(String latestProcessDate) {
        try {
            FileWriter fw = new FileWriter(latestProcessDateFile);
            fw.write(latestProcessDate);
            fw.flush();
            fw.close();
        } catch (Exception e) {
            logger.error("Could not write file " + latestProcessDateFile + " on Feeder construction", e);
            throw new RuntimeException(e);
        }
    }

    //just logs the error, but continues
    public synchronized void writeLatestProcessDateFileAndLogOnFail(String latestProcessDate) {
        try (FileOutputStream fos = new FileOutputStream(latestProcessDateFile)) {
            fos.write(latestProcessDate.getBytes());
            fos.flush();
            fos.close();
        } catch (Exception e) {
            logger.error("Could not write file " + latestProcessDateFile + " on Feeder construction", e);
        }
    }

    public String readLatestProcessDateFile() {
        try {
            return Files.readFile(latestProcessDateFile).trim();
        } catch (Exception exp) {
            logger.error(ExceptionUtil.toString(exp), exp);
        }
        return "";
    }

    /*
     * Reads the latest process date from the file location. Reverts to default on error
     * */
   /* @Deprecated
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
            logger.error(ExceptionUtil.toString(exp), exp);
        }
        if (latestProcessDate.isEmpty()) {
            latestProcessDate = defaultStartTime;
            setLatestProcessedDate(latestProcessDate);
        }
        logger.warn("Resuming from date: " + latestProcessDate);
        return latestProcessDate;
    }*/


    /*
     * Updates the latest process date to file
     * */
    /*
    public synchronized void setLatestProcessedDate(String date) {
        if (date == null || date.equals("")) {
            date = latestProcessDate;
        }

        Files.createFile(latestProcessDateFile, date);
        logger.info("Date: " + latestProcessDate + " written into " + latestProcessDateFile);
    }
*/

    /**
     * GETTER
     * SETTER
     */


    public static boolean validateLatestProcessDate(String latestProcessDate) {
        try {
            ZonedDateTime.parse(latestProcessDate);
            return true;
        } catch (Exception e) {
            logger.warn("Failed to parse latestProcessDate: |" + latestProcessDate + "|");
            return false;
        }

    }

    public synchronized String getLatestProcessDate() {
        return latestProcessDate;
    }

    public void setLatestProcessDate(String latestProcessDate) {
        this.latestProcessDate = latestProcessDate;
    }

    public LiveQueuePriority getQueuePriority() {
        return queuePriority;
    }
}

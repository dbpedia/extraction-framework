package org.dbpedia.extraction.live.feeder;

import org.dbpedia.extraction.live.config.LiveOptions;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;


/**
 * This Feeder initializes the consumption of the Wikimedia EventStreams API
 * See more at the documentation of the EventStreamsHelper class.
 *
 * @author Lena Schindler, November 2018
 */

public class EventStreamsFeeder extends Feeder {

    protected static Logger logger = LoggerFactory.getLogger("EventStreamsFeeder");
    private Long sleepTime = Long.parseLong(LiveOptions.options.get("feeder.eventstreams.sleepTime"));
    private static ArrayList<LiveQueueItem> queueItemBuffer = new ArrayList<>();
    private final long invocationTime;
    private static long readItemsCount = 0;


    public EventStreamsFeeder(String feederName,
                              LiveQueuePriority queuePriority,
                              String defaultStartTime,
                              String folderBasePath) {
        super(feederName, queuePriority, defaultStartTime, folderBasePath);
        invocationTime = ZonedDateTime.parse(defaultStartTime).toInstant().toEpochMilli();
        logger.info("Comparing\n" +
                "current:    "+System.currentTimeMillis()+"\n" +
                "feed in ms: "+invocationTime+"\n" +
                "feed:       "+defaultStartTime);
    }


    @Override
    protected void initFeeder() {
        EventStreamsHelper helper = new EventStreamsHelper(latestProcessDate);
        helper.eventStreamsClient();
    }

    @Override
    protected Collection<LiveQueueItem> getNextItems() {
        Collection<LiveQueueItem> returnQueueItems = new ArrayList();
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            logger.error("Error when handing over items to liveQueue", e);
        }
        // get first element if any and use as last processeddates
        if (!queueItemBuffer.isEmpty()) {
            int size = queueItemBuffer.size();
            LiveQueueItem firstItem = queueItemBuffer.get(0);
            LiveQueueItem lastItem = queueItemBuffer.get(size-1);
            String firstItemTime = queueItemBuffer.get(0).getModificationDate();

            //start with one second, because of division by zero
            long secondsRunning = ((System.currentTimeMillis() - invocationTime) / 1000) + 1;
            readItemsCount += size;
            logger.info(
                    "\n" + firstItemTime + ", writing " + size + " to queue, feed stats: "
                    + (readItemsCount / secondsRunning) + " per second, " + (readItemsCount) / ((float) secondsRunning / 3600) + " per hour\n" +
                            firstItemTime+"\n"+lastItem+"\n");

            returnQueueItems = exportQueueItemBuffer();

            // set last processed date
            setLatestProcessDate(firstItemTime);
            writeLatestProcessDateFileAndLog(firstItemTime);
        }
        return returnQueueItems;
    }

    public synchronized static Collection<LiveQueueItem> exportQueueItemBuffer() {
        Collection<LiveQueueItem> returnQueueItems = new ArrayList();

        // shoving to queue
        //returnQueueItems.addAll(queueItemBuffer);
        //queueItemBuffer.clear();
        returnQueueItems = queueItemBuffer;
        queueItemBuffer = new ArrayList<>();
        return returnQueueItems;
    }

    public synchronized static void addQueueItemToBuffer(LiveQueueItem item) {
        if (item.getItemName() != "") {
            queueItemBuffer.add(item);
        }
    }


}

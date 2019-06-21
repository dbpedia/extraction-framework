package org.dbpedia.extraction.live.feeder;

import org.dbpedia.extraction.live.config.LiveOptions;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final long invocationTime = System.currentTimeMillis();
    private static long readItemsCount = 0;


    public EventStreamsFeeder(String feederName,
                              LiveQueuePriority queuePriority,
                              String defaultStartTime,
                              String folderBasePath) {
        super(feederName, queuePriority, defaultStartTime, folderBasePath);
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
        synchronized (queueItemBuffer) {
            // get first element if any and use as last processeddates
            if (!queueItemBuffer.isEmpty()) {
                String firstItemTime = queueItemBuffer.get(0).getModificationDate();
                //start with one second, because of division by zero
                long secondsRunning = ((System.currentTimeMillis() - invocationTime) / 1000)+1;
                readItemsCount += queueItemBuffer.size();
                returnQueueItems.addAll(queueItemBuffer);
                queueItemBuffer.clear();
                logger.info("Writing " + queueItemBuffer.size() + " to queue, feed stats: "
                        + (readItemsCount / secondsRunning) + " per second, " + (readItemsCount) / (secondsRunning / 3600) + " per hour");

                // set last processed date
                setLatestProcessDate(firstItemTime);
                writeLatestProcessDateFileAndLog(firstItemTime);
            }
        }
        return returnQueueItems;
    }

    public static void addQueueItemToBuffer(LiveQueueItem item) {
        if (item.getItemName() != "") {
            synchronized (queueItemBuffer) {
                logger.info(item.getModificationDate());
                queueItemBuffer.add(item);
            }
        }
    }


}

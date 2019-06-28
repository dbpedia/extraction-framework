package org.dbpedia.extraction.live.feeder;

import org.dbpedia.extraction.live.config.LiveOptions;
import org.dbpedia.extraction.live.queue.LiveQueue;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.dbpedia.extraction.live.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.reflect.generics.tree.Tree;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;


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
    private static long lastItemsCount = 0;


    public EventStreamsFeeder(String feederName,
                              LiveQueuePriority queuePriority,
                              String defaultStartTime,
                              String folderBasePath) {
        super(feederName, queuePriority, defaultStartTime, folderBasePath);

        //latestProcessDate is set in super
        invocationTime = ZonedDateTime.parse(latestProcessDate).toInstant().toEpochMilli();
        logger.info("Comparing\n" +
                "current time:   " + System.currentTimeMillis() + "\n" +
                "current time:   " + DateUtil.transformToUTC(System.currentTimeMillis()) + "\n" +
                "feed time (ms): " + invocationTime + "\n" +
                "feed time:      " + latestProcessDate);
    }


    @Override
    protected void initFeeder() {
        EventStreamsHelper helper = new EventStreamsHelper(this.getLatestProcessDate());
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
            int bufferSize = queueItemBuffer.size();
            long queueSize = LiveQueue.getQueueSize();

            LiveQueueItem firstItem = queueItemBuffer.get(0);
            LiveQueueItem lastItem = queueItemBuffer.get(bufferSize - 1);
            String firstItemTime = firstItem.getModificationDate();
            String lastItemTime = lastItem.getModificationDate();

            // doing it
            returnQueueItems = exportQueueItemBuffer();


            //start with one second, because of division by zero
            long secondsRunning = ((ZonedDateTime.parse(lastItemTime).toInstant().toEpochMilli() - invocationTime) / 1000) + 1;
            long catchupseconds = (System.currentTimeMillis() - ZonedDateTime.parse(lastItemTime).toInstant().toEpochMilli()) / 1000;

            readItemsCount += bufferSize;
            logger.info("Stream at " + lastItemTime + " (T-" + (catchupseconds / 3600) + "h)"
                    + " writing " + bufferSize + " to queue (" + queueSize + " items), feed avg.: "
                    + (readItemsCount / secondsRunning) + " per second, "
                    + (readItemsCount) / ((float) secondsRunning / 3600) + " per hour, "
                    + (readItemsCount) / ((float) secondsRunning / 86400) + " per day");

            //tracing
            logger.trace("\n" + firstItem + "\n" + lastItem + "\n");

            // set last processed date every 10 minutes
            long itemsPer3Minutes = ((readItemsCount / secondsRunning) * 180);
            if ((readItemsCount - lastItemsCount) > itemsPer3Minutes) {
                lastItemsCount = readItemsCount;
                setLatestProcessDate(LiveQueue.getPriorityDate(this.getQueuePriority()));
                writeLatestProcessDateFileAndLogOnFail(getLatestProcessDate());
                logger.info("~3 min stream time passed, " + getLatestProcessDate() + " > " + latestProcessDateFile);

            }
        }
        return returnQueueItems;
    }

    public synchronized static Collection<LiveQueueItem> exportQueueItemBuffer() {
        //shoving to queue
        //returnQueueItems.addAll(queueItemBuffer);
        //queueItemBuffer.clear();
        Collection<LiveQueueItem> returnQueueItems = queueItemBuffer;
        queueItemBuffer = new ArrayList<>();
        return returnQueueItems;
    }

    public synchronized static void addQueueItemToBuffer(LiveQueueItem item) {
        if (item.getItemName() != "") {
            queueItemBuffer.add(item);
        } else {
            logger.warn("skipping item, itemName not set:" + item);
        }
    }


}

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
    private static Collection<LiveQueueItem> queueItemCollection;

    public EventStreamsFeeder(String feederName,
                              LiveQueuePriority queuePriority,
                              String defaultStartTime,
                              String folderBasePath) {
        super(feederName, queuePriority, defaultStartTime, folderBasePath);
        queueItemCollection = new ArrayList<>();
    }


    @Override
    protected void initFeeder() {
        EventStreamsHelper helper = new EventStreamsHelper();
        helper.eventStreamsClient();
    }

    @Override
    protected Collection<LiveQueueItem> getNextItems(){
        Collection <LiveQueueItem> returnQueueItemCollection;
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e){
            logger.error("Error when handing over items to liveQueue" + e.getMessage());
        }
        synchronized (this){
            returnQueueItemCollection = queueItemCollection;
            queueItemCollection = new ArrayList<>();
        }
        return returnQueueItemCollection;
    }

    public static synchronized void addQueueItemCollection(LiveQueueItem item){
        if (item.getItemName()!= ""){
            queueItemCollection.add(item);
        }
    }
}

package org.dbpedia.extraction.live.feeder;

import java.util.ArrayList;
import java.util.Collection;
import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.dbpedia.utils.sse.EventStreamsHelper;
import org.slf4j.Logger;


/**
 * This Feeder initializes the consumption of the Wikimedia EventStreams API
 * See more at the documentation of the org.dbpedia.utils.sse.EventStreamsHelper class.
 *
 * @author Lena Schindler, November 2018
 */

public class EventStreamsFeeder extends Feeder {

    protected static Logger logger;
    private static Collection<LiveQueueItem> queueItemCollection;
    private ArrayList<Integer> allowedNamespaces;
    private String language;
    private ArrayList<String> streams;
    private String baseURL;

    public EventStreamsFeeder(String feederName,
                              LiveQueuePriority queuePriority,
                              String defaultStartTime, String folderBasePath) {
        super(feederName, queuePriority, defaultStartTime, folderBasePath);
        queueItemCollection = new ArrayList<>();
        allowedNamespaces = new ArrayList<>();
        streams = new ArrayList<>();
        for (String namespace : LiveOptions.options.get("feeder.eventstreams.allowedNamespaces").split("\\s*,\\s*")) {
            allowedNamespaces.add(Integer.parseInt(namespace));
        }
        baseURL = LiveOptions.options.get("eventstreams.baseURL");
        for (String stream: LiveOptions.options.get("eventstreams.streams").split("\\s*,\\s*")){
            streams.add(stream);
        }

        language = LiveOptions.options.get("language");
    }


    @Override
    protected void initFeeder() {
        EventStreamsHelper helper = new EventStreamsHelper(language, allowedNamespaces, streams);
        helper.eventStreamsClient();
    }

    @Override
    protected Collection<LiveQueueItem> getNextItems(){
        Collection <LiveQueueItem> returnQueueItemCollection;
        try {
            Thread.sleep(300000);
            System.out.println("now");
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

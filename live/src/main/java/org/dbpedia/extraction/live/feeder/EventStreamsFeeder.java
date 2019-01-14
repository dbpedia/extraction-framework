package org.dbpedia.extraction.live.feeder;

import java.util.ArrayList;
import java.util.Collection;
import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.dbpedia.utils.sse.EventStreamsHelper;


/**
 * This Feeder initializes the consumption of the Wikimedia EventStreams API
 * See more at the documentation of the org.dbpedia.utils.sse.EventStreamsHelper class.
 *
 * @author Lena Schindler, November 2018
 */

public class EventStreamsFeeder extends Feeder {

    private static Collection<LiveQueueItem> queueItemCollection;
    private ArrayList<Integer> allowedNamespaces;
    private String language;

    public EventStreamsFeeder(String feederName,
                              LiveQueuePriority queuePriority,
                              String defaultStartTime, String folderBasePath) {
        super(feederName, queuePriority, defaultStartTime, folderBasePath);
        queueItemCollection = new ArrayList<>();
        allowedNamespaces = new ArrayList<>();
        for (String namespace : LiveOptions.options.get("feeder.eventstreams.allowedNamespaces").split("\\s*,\\s*")) {
            allowedNamespaces.add(Integer.parseInt(namespace));
        }
        language = LiveOptions.options.get("language");
    }


    @Override
    protected void initFeeder() {
        EventStreamsHelper helper = new EventStreamsHelper(language, allowedNamespaces);
        helper.eventStreamsClient();
    }

    @Override
    protected Collection<LiveQueueItem> getNextItems() {
        Collection <LiveQueueItem> returnQueueItemCollection;
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

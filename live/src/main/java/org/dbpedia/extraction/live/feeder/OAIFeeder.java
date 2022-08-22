package org.dbpedia.extraction.live.feeder;


import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.dbpedia.extraction.live.util.OAIUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * User: Dimitris Kontokostas
 * Date: 9/6/12
 * Time: 1:07 PM
 * This is a general purpose OAI Feeder. It can be used as is for simple operations (live feeder, unmodified pages feeder)
 * or subclass and override "handleFeedItem" function
 */
public class OAIFeeder extends Feeder {

    protected final String oaiUri;
    protected final String oaiPrefix;
    protected final String baseWikiUri;

    protected final long pollInterval;              // in miliseconds
    protected final long sleepInterval;             // in miliseconds

    protected Iterator<LiveQueueItem> oaiRecordIterator;

    public OAIFeeder(String feederName, LiveQueuePriority queuePriority,
                     String oaiUri, String oaiPrefix, String baseWikiUri,
                     long pollInterval, long sleepInterval, String defaultStartTime,
                     String folderBasePath) {
        super(feederName,queuePriority,defaultStartTime,folderBasePath);

        this.oaiUri = oaiUri;
        this.oaiPrefix = oaiPrefix;
        this.baseWikiUri = baseWikiUri;

        this.pollInterval = pollInterval;
        this.sleepInterval = sleepInterval;
    }


    @Override
    protected void initFeeder() {
        oaiRecordIterator = OAIUtil.createEndlessFeederItemIterator(oaiUri, latestProcessDate, pollInterval, sleepInterval);
    }

    @Override
    protected Collection<LiveQueueItem> getNextItems() {
        List<LiveQueueItem> i = new ArrayList<>();
        i.add(oaiRecordIterator.next());
        return i;
    }
}

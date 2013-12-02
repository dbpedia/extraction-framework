package org.dbpedia.extraction.live.feeder;


import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.queue.LiveQueue;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.dbpedia.extraction.live.util.Files;
import org.dbpedia.extraction.live.util.OAIUtil;
import scala.actors.threadpool.Arrays;

import java.io.File;
import java.util.ArrayList;
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

    protected String oaiUri;
    protected String oaiPrefix;
    protected String baseWikiUri;

    protected long pollInterval;              // in miliseconds
    protected long sleepInterval;             // in miliseconds

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
    protected List<LiveQueueItem> getNextItems() {
        List<LiveQueueItem> i = new ArrayList<LiveQueueItem>();
        i.add(oaiRecordIterator.next());
        return i;
    }
}

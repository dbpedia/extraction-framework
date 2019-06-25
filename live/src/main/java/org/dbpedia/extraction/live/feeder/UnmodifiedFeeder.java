package org.dbpedia.extraction.live.feeder;

import org.dbpedia.extraction.live.queue.LiveQueue;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.dbpedia.extraction.live.storage.JDBCUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * This class feeds the unmodifed pages (updated before minDaysAgo) to the Live Extractor
 * to keep the Queue size low this exteractor sleeps until the size is below a threshold
 * and then adds chunk items. To keep the system running at all times, chunk, threshold and
 * sleepTime should be related in the following type:
 *           sleepTime = threshold * (avg item extract time), and
 *           chunk a lot bigger than threshold (to minimize duplicate adding, e.g. 5000)
 *
 *           As a magic number, we estimate 10 million records with uniform partition
 *           If no entities to update are found, the feeder will sleep:
 *           1h * (Chunksize / Modified estimate per hour)
 *           (10,000,000/(minDaysAgo * 24h))
 *
 */
public class UnmodifiedFeeder extends Feeder {
    private int minDaysAgo = 0;
    private long chunk = 0;
    private long threshold = 0;
    private long sleepTime = 0;
    private long estimatedRecords = 10*1000*1000L;
    private long firstTimeSleep =3*60*1000L;

    public UnmodifiedFeeder(String feederName, LiveQueuePriority queuePriority,
                            int minDaysAgo, int chunk, int threshold, long sleepTime,
                            String defaultStartTime,String folderBasePath) {

        super(feederName, queuePriority, defaultStartTime, folderBasePath);
        this.minDaysAgo = minDaysAgo;
        this.chunk = chunk;
        this.threshold = threshold;
        this.sleepTime = sleepTime;
    }

    @Override
    protected void initFeeder() {
        //Nothing to init here
    }

    @Override
    public void startFeeder() {
        // Sleep to allow other feeders fill the queue

        super.startFeeder();
    }

    @Override
    protected Collection<LiveQueueItem> getNextItems() {

        try {
            //set to 3 minutes
            Thread.sleep(firstTimeSleep);
            firstTimeSleep=0;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        while (LiveQueue.getQueueSize() > threshold) {
            try {
                int m = (int) (LiveQueue.getQueueSize() / threshold);
                Thread.sleep(m * sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        }
        Set<LiveQueueItem> items = JDBCUtil.getCacheUnmodified(minDaysAgo, chunk);
        if (items != null && items.size() == 0) {
            try {
                long s =  60*60*1000 /  (chunk/  (estimatedRecords / (minDaysAgo*24)));
                logger.info("No unmodified found, sleeping "+(s/1000)+" seconds.");
                Thread.sleep(s);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        }
        logger.info( "Writing " + items.size() + " to queue (" + LiveQueue.getQueueSize() + " items) ");
        return items;
    }
}

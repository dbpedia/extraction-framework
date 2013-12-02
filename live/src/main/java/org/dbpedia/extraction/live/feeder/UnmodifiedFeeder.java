package org.dbpedia.extraction.live.feeder;

import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.queue.LiveQueue;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.dbpedia.extraction.live.storage.JDBCUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class feeds the unmodifed pages (updated before minDaysAgo) to the Live Extractor
 * to keep the Queue size low this exteractor sleeps until the size is below a threshold
 * and then adds chunk items. To keep the system running at all times, chunk, threshold and
 * sleepTime should be related in the following type:
 *           sleepTime = threshold * (avg item extract time), and
 *           chunk a lot bigger than threshold (to minimize duplicate adding)
 */
public class UnmodifiedFeeder extends Feeder {
    private int minDaysAgo = 0;
    private long chunk = 0;
    private long threshold = 0;
    private long sleepTime = 0;

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
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        super.startFeeder();
    }

    @Override
    protected List<LiveQueueItem> getNextItems() {
        while (LiveQueue.getQueueSize() > threshold) {
            try {
                int m = (int) (LiveQueue.getQueueSize() / threshold);
                Thread.sleep(m * sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        }
        List<LiveQueueItem> items = JDBCUtil.getCacheUnmodified(minDaysAgo, chunk);
        if (items.size() == 0) {
            try {

                Thread.sleep(5 * sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return Collections.emptyList();
            }
        }
        return items;
    }
}

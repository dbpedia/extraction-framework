package org.dbpedia.extraction.live.queue;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Dimitris Kontokostas
 * @since 13/7/2016 2:39 μμ
 */
public class LiveQueueTest {

    @Test
    public void queueTest() throws InterruptedException {
        LiveQueueItem q1 = new LiveQueueItem(1, "item 1", "2016-07-13T10:29:43Z", false, "");
        q1.setPriority(LiveQueuePriority.LivePriority);
        LiveQueueItem q1a = new LiveQueueItem(2, "item 1a", "2016-07-13T10:29:50Z", false, "");
        q1a.setPriority(LiveQueuePriority.LivePriority);

        LiveQueueItem q2 = new LiveQueueItem(3, "item 3", "2016-07-13T10:29:55Z", false, "");
        q2.setPriority(LiveQueuePriority.UnmodifiedPagePriority);
        LiveQueueItem q2a = new LiveQueueItem(4, "item 3a", "2016-07-13T10:29:56Z", false, "");
        q2a.setPriority(LiveQueuePriority.UnmodifiedPagePriority);

        LiveQueue.add(q2);
        LiveQueue.add(q2a);
        LiveQueue.add(q1);
        LiveQueue.add(q1a);


        Assert.assertTrue(LiveQueue.take() == q1a) ;
        Assert.assertTrue(LiveQueue.take() == q1) ;
        Assert.assertTrue(LiveQueue.take() == q2) ;
        Assert.assertTrue(LiveQueue.take() == q2a) ;


    }

}
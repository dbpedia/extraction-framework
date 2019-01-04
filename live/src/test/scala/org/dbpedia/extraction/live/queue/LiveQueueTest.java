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
        LiveQueueItem qL1 = new LiveQueueItem(1, "item L1", "2016-07-13T10:29:43Z", false, "");
        qL1.setPriority(LiveQueuePriority.LivePriority);
        LiveQueueItem qL2 = new LiveQueueItem(2, "item L2", "2016-07-13T10:29:50Z", false, "");
        qL2.setPriority(LiveQueuePriority.LivePriority);

        LiveQueueItem qU1 = new LiveQueueItem(3, "item U1", "2016-07-13T10:29:55Z", false, "");
        qU1.setPriority(LiveQueuePriority.UnmodifiedPagePriority);
        LiveQueueItem qU2 = new LiveQueueItem(4, "item U2", "2016-07-13T10:29:56Z", false, "");
        qU2.setPriority(LiveQueuePriority.UnmodifiedPagePriority);

        LiveQueue.add(qU1);
        LiveQueue.add(qU2);
        LiveQueue.add(qL1);
        LiveQueue.add(qL2);


        Assert.assertTrue(LiveQueue.take() == qL1) ;
        Assert.assertTrue(LiveQueue.take() == qL2) ;

        Assert.assertTrue(LiveQueue.take() == qU2) ;
        Assert.assertTrue(LiveQueue.take() == qU1) ;



    }

}
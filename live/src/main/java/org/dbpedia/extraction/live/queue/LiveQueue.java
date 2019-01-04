package org.dbpedia.extraction.live.queue;

import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * Created with IntelliJ IDEA.
 * User: Dimitris Kontokostas
 * Date: 12/14/12
 * Time: 12:22 PM
 * This is the main queue of the Live Framework.It keeps a Priority Blocking Queue for the actual queue
 * and a unique hashmap to keep track of duplicates
 * TODO: The unique hashmap is not 100% thread safe, however, inserting simultaneously the same item (should be) very rare
 * TODO: If we syncronize add() and take() we will get in a deadlock so we should re-implement the PriorityBlockingQueue
 */
public class LiveQueue {
    private static Logger logger;

    private static final PriorityBlockingQueue<LiveQueueItem> queue = new PriorityBlockingQueue<LiveQueueItem>(1000);
    // this is a Map<long,int>, long is the pageID and int the number of same items in the queue (see add())
    private static final Set<String> uniqueSet = new HashSet(1000);

    // Keeps track of the size of each priority
    private static final HashMap<LiveQueuePriority,Long> counts = new HashMap<LiveQueuePriority, Long>(10);

    // Keeps track of the max modification date per priority
    private static final HashMap<LiveQueuePriority,String> modificationDate = new HashMap<LiveQueuePriority, String>(5);

    private LiveQueue() {
    }

    public static void add(LiveQueueItem item) {
        // Simplified a lot to lower the complexity
        if (!uniqueSet.contains(item.getItemName()) ) {
            uniqueSet.add(item.getItemName());
            counts.put(item.getPriority(), getPrioritySize(item.getPriority()) + 1);
            queue.add(item);
        }
    }

    public static LiveQueueItem take() throws InterruptedException {
        LiveQueueItem item = queue.take();
        uniqueSet.remove(item.getItemName());
        // update counts
        counts.put(item.getPriority(), getPrioritySize(item.getPriority()) - 1);
        modificationDate.put(item.getPriority(), item.getModificationDate());

        return item;
    }

    public static long getQueueSize(){
        return queue.size();
    }

    public static long getPrioritySize(LiveQueuePriority priority){
        Object value = counts.get(priority);
        return (value == null) ? 0 : ((Long) value);
    }

    public static String getPriorityDate(LiveQueuePriority priority){
        for (LiveQueueItem i : queue){
            if (i.getPriority() == priority)
                return i.getModificationDate();
        }
        String d = modificationDate.get(priority);
        if (d != null)
            return d;
        return "";
    }

}
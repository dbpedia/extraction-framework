package org.dbpedia.extraction.live.queue;

import org.apache.log4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
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

    private static PriorityBlockingQueue<LiveQueueItem> queue = null;
    // this is a Map<long,int>, long is the pageID and int the number of same items in the queue (see add())
    private static HashMap<Long,Integer> uniqueMap = null;

    // Keeps track of the size of each priority
    private static HashMap<LiveQueuePriority,Long> counts = null;

    // Keeps track of the max modification date per priority
    private static HashMap<LiveQueuePriority,String> modificationDate = null;

    private LiveQueue() {
    }

    public static void add(LiveQueueItem item) {
        Object value = getUniqueMap().get(item.getItemID());
        int finalValue = 1; // default value in it does not exists

        if (value != null) {
            // Existing item, assign to highest priority
            // NOTE: mappings priority also need to update the mappings/ontology so keep both
            Iterator<LiveQueueItem> iterator;
            iterator = getQueue().iterator();

            while (iterator.hasNext()) {
                LiveQueueItem e = iterator.next();
                if (e.getItemID() == item.getItemID()) {

                    LiveQueuePriority existingPriority = e.getPriority();
                    LiveQueuePriority newPriority = item.getPriority();

                    if (newPriority.compareTo(existingPriority) > 0) {
                        // check only for higher priority
                        // keep both if old in mappingsPriority
                        if (existingPriority.equals(LiveQueuePriority.MappingPriority)) {
                            // keep both
                            finalValue++;
                        } else {
                            // remove existing
                            iterator.remove();
                            getCounts().put(item.getPriority(), getPrioritySize(e.getPriority()) - 1);
                        }
                    } else {
                        // if new priority is lower or the same do nothing
                        return;
                    }
                }
            }
        }
        getUniqueMap().put(item.getItemID(), finalValue);
        getCounts().put(item.getPriority(), getPrioritySize(item.getPriority()) + 1);
        getQueue().add(item);
    }

    public static LiveQueueItem take() throws InterruptedException {

        LiveQueueItem item = getQueue().take();
        int value = (Integer) getUniqueMap().remove(item.getItemID());
        if (value != 1) {  // not single item
            getUniqueMap().put(item.getItemID(), value - 1);
        }
        // update counts
        getCounts().put(item.getPriority(), getPrioritySize(item.getPriority()) -1);
        getModDates().put(item.getPriority(),item.getModificationDate());
        return item;
    }

    public static long getQueueSize(){
        return getQueue().size();
    }

    public static long getPrioritySize(LiveQueuePriority priority){
        Object value = getCounts().get(priority);
        return (value == null) ? 0 : ((Long) value);
    }

    public static String getPriorityDate(LiveQueuePriority priority){
        for (LiveQueueItem i : queue){
            if (i.getPriority() == priority)
                return i.getModificationDate();
        }
        String d = getModDates().get(priority);
        if (d != null)
            return d;
        return "";
    }

    private static PriorityBlockingQueue<LiveQueueItem> getQueue() {
        if (queue == null) {
            synchronized (LiveQueue.class) {
                if (queue == null) {
                    queue = new PriorityBlockingQueue<LiveQueueItem>(1000);
                }
            }
        }
        return queue;
    }

    private static HashMap<Long,Integer> getUniqueMap() {
        if (uniqueMap == null) {
            synchronized (LiveQueue.class) {
                if (uniqueMap == null) {
                    uniqueMap = new HashMap(1000);
                }
            }
        }
        return uniqueMap;
    }

    private static HashMap<LiveQueuePriority,Long> getCounts() {
        if (counts == null) {
            synchronized (LiveQueue.class) {
                if (counts == null) {
                    counts = new HashMap<LiveQueuePriority, Long>(10);
                }
            }
        }
        return counts;
    }

    private static HashMap<LiveQueuePriority,String> getModDates() {
        if (modificationDate == null) {
            synchronized (LiveQueue.class) {
                if (modificationDate == null) {
                    modificationDate = new HashMap<LiveQueuePriority, String>(5);
                }
            }
        }
        return modificationDate;
    }
}
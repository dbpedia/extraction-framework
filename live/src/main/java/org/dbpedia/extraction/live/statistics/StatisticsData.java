package org.dbpedia.extraction.live.statistics;

import org.apache.commons.lang3.mutable.MutableLong;
import org.dbpedia.extraction.live.util.DateUtil;

import java.util.Calendar;
import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created with IntelliJ IDEA.
 * User: Dimitris Kontokostas
 * Date: 8/2/12
 * Time: 5:51 PM
 * This class holds and processes statistics
 */

public class StatisticsData {
    private static final long MINUPDATEINTERVAL = 1000; //in milliseconds
    private static long entityAll = 0;
    private static long triplesAll = 0;
    private static long lastUpdate;
    private static StatisticsResult result;
    private static long startTime = 0;

    /*Store the results of each hour of runtime
     *This method of storing the hour results may not work as intended if the update interval is large.
     *It's best to keep it around a few seconds
     */
    private static Stack<MutableLong> entityHours = new Stack<>(); //Max elements: 24 (for the hours in a day)
    private static Stack<MutableLong> triplesHours = new Stack<>();
    private static long newValueTimestamp = 0; //saves the timestamp of the last insertion in the stack

    // keep a list with triples and timestamps
    private static ConcurrentLinkedDeque<TripleItem> statisticsTriplesQueue = new ConcurrentLinkedDeque<TripleItem>();
    // keep a list with just timestamps to keep track of page change number
    private static ConcurrentLinkedDeque<Long> statisticsTimestampQueue = new ConcurrentLinkedDeque<Long>();

    protected StatisticsData() {
    }

    public static void addItem(int numTriples, long pageTimestamp) {
        try {
            statisticsTriplesQueue.addFirst(new TripleItem(numTriples, pageTimestamp));
            statisticsTimestampQueue.addFirst(pageTimestamp);
        } catch (NullPointerException e) {
            // TODO take furter action? not important...
        }
    }

    public static synchronized String generateStatistics() {
        long now = System.currentTimeMillis();

        //Check if hour needs changing
        if(now - newValueTimestamp > DateUtil.getDuration1HourMillis()){
            entityHours.push(new MutableLong(0)); //add item for new hour
            triplesHours.push(new MutableLong(0));
            newValueTimestamp = now;
            if(entityHours.size() > 24) {
                entityAll += entityHours.get(0).longValue();
                triplesAll += triplesHours.get(0).longValue();
                entityHours.remove(0); //remove the 25th hour because we only want the last 24
                triplesHours.remove(0);
            }
        }

        // compute entity variables
        int entity1m = 0, entity5m = 0, entity1h = 0, entity1d = 0;

        Iterator<Long> timeIter = statisticsTimestampQueue.iterator();
        while (timeIter.hasNext()) {
            long timestamp = (long) timeIter.next();
            long d = now - timestamp;

            if (d < DateUtil.getDuration1HourMillis()) {
                entity1h++;
                if (d < 5*DateUtil.getDuration1MinMillis()) {
                    entity5m++;
                    if (d < DateUtil.getDuration1MinMillis()) {
                        entity1m++;
                    }
                }
            }else {
                entityHours.peek().increment();
                timeIter.remove(); // remove from list if older than an hour
            }
        }
        for(MutableLong val: entityHours)
            entity1d += val.longValue();
        entity1d += entity1h;

        // compute triples variables
        int triples1m = 0, triples5m = 0, triples1h = 0, triples1d = 0;

        Iterator<TripleItem> triplesIter = statisticsTriplesQueue.iterator();
        while (triplesIter.hasNext()) {
            TripleItem item = (TripleItem) triplesIter.next();
            long timestamp = item.getTimestamp();
            int val = item.getNumOfTriples();
            long d = now - timestamp;

            if (d < DateUtil.getDuration1HourMillis()) {
                triples1h+=val;
                if (d < 5*DateUtil.getDuration1MinMillis()) {
                    triples5m+=val;
                    if (d < DateUtil.getDuration1MinMillis()) {
                        triples1m+=val;
                    }
                }
            }else {
                triplesHours.peek().add(val);
                triplesIter.remove(); // remove from list if older than an hour
            }
        }
        for(MutableLong val: entityHours)
            triples1d += val.longValue();
        triples1d += triples1h;

        result = new StatisticsResult(entity1m, entity5m, entity1h, entity1d, entityAll + entity1d);
        result.setTriples(triples1m, triples5m, triples1h, triples1d, triplesAll + triples1d);
        return result.toString();
    }
}
package org.dbpedia.extraction.live.statistics;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created with IntelliJ IDEA.
 * User: Dimitris Kontokostas
 * Date: 8/2/12
 * Time: 5:51 PM
 * This class holds and processes statistics
 */
public class StatisticsData {
    // stats* variables hold the number of updated pages
    private static long stats1m = 0;
    private static long stats5m = 0;
    private static long stats1h = 0;
    private static long stats1d = 0;
    private static long statsAll = 0;

    // keep a list with a detailed latetest page changes
    private static ConcurrentLinkedDeque<StatisticsItem> statisticsDetailedQueue = new ConcurrentLinkedDeque<StatisticsItem>();

    // keep a list with just timestamps to keep track of page change number
    private static ConcurrentLinkedDeque<Long> statisticsTimestampQueue = new ConcurrentLinkedDeque<Long>();

    protected StatisticsData() {
    }

    public static void setStats1m(long value) {
        stats1m = value;
    }

    public static void setStats5m(long value) {
        stats5m = value;
    }

    public static void setStats1h(long value) {
        stats1h = value;
    }

    public static void setStats1d(long value) {
        stats1d = value;
    }

    public static void setStatsAll(long value) {
        statsAll = value;
    }

    public static void addItem(String pageName, String pageDBpediaURI, String pageWikipediaURI, int pageID, long pageTimestamp) {
        try {
            statisticsDetailedQueue.addFirst(new StatisticsItem(pageName, pageDBpediaURI, pageWikipediaURI, pageID, pageTimestamp));
            statisticsTimestampQueue.addFirst(pageTimestamp);
        } catch (NullPointerException e) {
            // TODO take furter action? not important...
        }
    }

    public static synchronized String generateStatistics(int noOfDetailedIntances) {

        long now = System.currentTimeMillis();
        long duration24Hour = 24 * 60 * 60 * 1000; // One day
        long duration01Hour = 60 * 60 * 1000; // One hour
        long duration05Min = 5 * 60 * 1000; // Five Minutes
        long duration01Min = 60 * 1000; // One Minute

        // remove old statistics
        while (!statisticsTimestampQueue.isEmpty()) {
            if (now - statisticsTimestampQueue.peekLast() > duration24Hour) {
                statisticsTimestampQueue.pollLast(); // remove from list if older than a day
                statsAll++;
            } else
                break;
        }

        while (statisticsDetailedQueue.size() > noOfDetailedIntances) {
            statisticsDetailedQueue.pollLast();
        }

        // update stats variables
        stats1m = 0;
        stats5m = 0;
        stats1h = 0;
        stats1d = 0;
        Iterator<Long> timeIter = statisticsTimestampQueue.iterator();
        while (timeIter.hasNext()) {

            long d = now - (long) timeIter.next();

            if (d < duration01Hour) {
                stats1h++;
                if (d < duration05Min) {
                    stats5m++;
                    if (d < duration01Min) {
                        stats1m++;
                    }
                }
            } else {

                break;
            }
        }
        stats1d = statisticsTimestampQueue.size();

        String detailedInstances = "";
        Iterator<StatisticsItem> detIter = statisticsDetailedQueue.iterator();
        while (detIter.hasNext()) {
            detailedInstances += detIter.next().toString() + "\r\n";
        }

        // generate file contents
        String retValue = "";
        retValue += stats1m + "\r\n";
        retValue += stats5m + "\r\n";
        retValue += stats1h + "\r\n";
        retValue += stats1d + "\r\n";
        retValue += statsAll + stats1d + "\r\n";
        retValue += detailedInstances;

        return retValue;
    }
}
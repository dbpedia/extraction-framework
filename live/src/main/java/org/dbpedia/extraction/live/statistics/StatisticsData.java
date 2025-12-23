package org.dbpedia.extraction.live.statistics;

import org.dbpedia.extraction.live.util.DateUtil;
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

    public static synchronized void setAllStats(long s1m, long s5m, long s1h, long s1d, long sall) {
        stats1m = s1m;
        stats5m = s5m;
        stats1h = s1h;
        stats1d = s1d;
        statsAll = sall;
    }

    public static synchronized void setStats1m(long value) {
        stats1m = value;
    }

    public static synchronized void setStats5m(long value) {
        stats5m = value;
    }

    public static synchronized void setStats1h(long value) {
        stats1h = value;
    }

    public static synchronized void setStats1d(long value) {
        stats1d = value;
    }

    public static synchronized void setStatsAll(long value) {
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

        // remove old statistics
        while (!statisticsTimestampQueue.isEmpty()) {
            if (now - statisticsTimestampQueue.peekLast() > DateUtil.getDuration1DayMillis()) {
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

            if (d < DateUtil.getDuration1HourMillis()) {
                stats1h++;
                if (d < 5*DateUtil.getDuration1MinMillis()) {
                    stats5m++;
                    if (d < DateUtil.getDuration1MinMillis()) {
                        stats1m++;
                    }
                }
            } else {

                break;
            }
        }
        stats1d = statisticsTimestampQueue.size();

        // generate json contents
        StringBuffer sb = new StringBuffer("");

        sb.append("{");
        sb.append("\"upd1m\": \"" + stats1m + "\",\n");
        sb.append("\"upd5m\": \"" + stats5m + "\",\n");
        sb.append("\"upd1h\": \"" + stats1h + "\",\n");
        sb.append("\"upd1d\": \"" + stats1d + "\",\n");
        sb.append("\"updat\": \"" + statsAll + stats1d + "\",\n");
        sb.append("\"timestamp\": \"" + System.currentTimeMillis() + "\",\n");
        sb.append("\"latest\": [");


        Iterator<StatisticsItem> detIter = statisticsDetailedQueue.iterator();
        while (detIter.hasNext()) {
            StatisticsItem item = detIter.next();
            sb.append("\n{");
            sb.append("\"title\":\"" + item.getPageTitle() + "\",");
            sb.append("\"dbpediaURI\": \"" + item.getDBpediaURI() + "\",");
            sb.append("\"wikipediaURI\": \"" + item.getWikipediaURI() + "\",");
            sb.append("\"timestamp\": \"" + item.getTimestamp() + "\",");
            sb.append("\"delta\": \"" + item.getHasDelta() + "\"");
            sb.append("}");
            if (detIter.hasNext())
                sb.append(",");
        }
        sb.append("]}");

        return sb.toString();
    }
}

/*

 {
     "upd-1m": "99",
     "upd-5m": "478",
     "upd-1h": "3452",
     "upd-1d": "3452",
     "upd-at": "3452",
     "latest": [
         {
             "title": "Yuko Matsumiya",
             "dbpedia-uri": "http://wiki=nl,locale=nl.wikipedia.org/wiki/Yuko_Matsumiya",
             "wikipedia-uri": "http://wiki=nl,locale=nl.wikipedia.org/wiki/Yuko_Matsumiya",
             "timestamp": "1350637099091",
             "delta": "false"
         },
         {
             "title": "Yuko Matsumiya",
             "dbpedia-uri": "http://wiki=nl,locale=nl.wikipedia.org/wiki/Yuko_Matsumiya",
             "wikipedia-uri": "http://wiki=nl,locale=nl.wikipedia.org/wiki/Yuko_Matsumiya",
             "timestamp": "1350637099091",
             "delta": "false"
         },
         {
             "title": "Yuko Matsumiya",
             "dbpedia-uri": "http://wiki=nl,locale=nl.wikipedia.org/wiki/Yuko_Matsumiya",
             "wikipedia-uri": "http://wiki=nl,locale=nl.wikipedia.org/wiki/Yuko_Matsumiya",
             "timestamp": "1350637099091",
             "delta": "false"
         }
     ]
 }


* */
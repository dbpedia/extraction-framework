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
    private static long lastUpdate;
    private static StatisticsResult result;
    private static long startTime = 0;

    /*Store the results of each hour of runtime
     *This method of storing the hour results may not work as intended if the update interval is large.
     *It's best to keep it around a few seconds
     */
    private static Stack<MutableLong> entityHours = new Stack<>(); //Max elements: 24 (for the hours in a day)
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
            newValueTimestamp = now;
            if(entityHours.size() > 24) {
                entityAll += entityHours.get(0).longValue();
                entityHours.remove(0); //remove the 25th hour because we only want the last 24
            }
        }

        // update stats variables
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
        int ordem = 1;
        for(MutableLong val: entityHours){
            entity1d += val.longValue();
            System.out.println("Ordem: " + ordem + "; Valor: " + val.longValue());
            ordem++;
        }
        entity1d += entity1h;

        result = new StatisticsResult(entity1m, entity5m, entity1h, entity1d, entityAll + entity1d);
        return result.toString();

        // generate json contents
        /*StringBuffer sb = new StringBuffer("");

        sb.append("{");
        sb.append("\"upd1m\": \"" + stats1m + "\",\n");
        sb.append("\"upd5m\": \"" + stats5m + "\",\n");
        sb.append("\"upd1h\": \"" + stats1h + "\",\n");
        sb.append("\"upd1d\": \"" + stats1d + "\",\n");
        sb.append("\"updat\": \"" + statsAll + stats1d + "\",\n");
        sb.append("\"timestamp\": \"" + System.currentTimeMillis() + "\",\n");
        sb.append("\"latest\": [");*/


        /*Iterator<StatisticsItem> detIter = statisticsDetailedQueue.iterator();
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
        sb.append("]}");*/

        //return sb.toString();
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
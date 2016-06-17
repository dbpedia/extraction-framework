package org.dbpedia.extraction.live.statistics;

import org.dbpedia.extraction.live.main.Main;
import org.dbpedia.extraction.live.queue.LiveQueue;

import java.text.DecimalFormat;

/**
 * Created by Andre on 07/07/2015.
 */
public class StatisticsResult {
    private int entity1m;
    private int entity5m;
    private int entity1h;
    private long entity1d;
    private long entityAll;

    private int triples1m;
    private int triples5m;
    private int triples1h;
    private long triples1d;
    private long triplesAll;

    private double averageTriples;
    private String timePassed;
    private String extracted = "";
    private String queued = "";
    private long itemsQueue;

    public StatisticsResult(){}

    public StatisticsResult(int m1, int m5, int h1, long d1, long all){
        entity1m = m1;
        entity5m = m5;
        entity1h = h1;
        entity1d = d1;
        entityAll = all;
    }

    /*Returns a JSON formatted string*/
    @Override
    public String toString() {
        return "{" +
                "\"entity1m\":" + entity1m +
                ",\"entity5m\":" + entity5m +
                ",\"entity1h\":" + entity1h +
                ",\"entity1d\":" + entity1d +
                ",\"entityAll\":" + entityAll +
                ",\"triples1m\":" + triples1m +
                ",\"triples5m\":" + triples5m +
                ",\"triples1h\":" + triples1h +
                ",\"triples1d\":" + triples1d +
                ",\"triplesAll\":" + triplesAll +
                ",\"avrgTriples\":\"" + new DecimalFormat("#.##").format(averageTriples) +"\"" +
                ",\"timePassed\":\"" + timePassed +"\"" +
                ",\"itemsQueued\":" + itemsQueue +
                ",\"extractedTitles\":" + extracted +
                ",\"queued\":" + queued +
                ",\"state\":\"" + Main.state +"\"" +
                '}';
    }

    public void setTriples(int tm1, int tm5, int th1, long td1, long tAll){
        triples1m = tm1;
        triples5m = tm5;
        triples1h = th1;
        triples1d = td1;
        triplesAll = tAll;
    }

    public void finish(long timestamp, String titles){
        averageTriples = triplesAll / (entityAll * 1.0);
        long millis = System.currentTimeMillis() - timestamp;
        long second = (millis / 1000) % 60;
        long minute = (millis / (1000 * 60)) % 60;
        long hour = (millis / (1000 * 60 * 60)) % 24;
        long day = millis / (1000 * 60 * 60 * 24);

        String format = "";
        if(day < 10) format += "%2d days ";
        else format += "%02d days ";
        if(hour < 10) format += "%2d hours ";
        else format += "%02d hours ";
        if(minute < 10) format += "%2d mins ";
        else format += "%02d mins ";
        if(second < 10) format += "%2d secs";
        else format += "%02d secs";
        timePassed = String.format(format, day, hour, minute, second);
        extracted = titles;
        setQueued();
        itemsQueue = LiveQueue.getQueueSize();
    }

    private void setQueued(){
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for(String s: LiveQueue.getNextQueuedItems()) {
            if(s == null || s.equals("")) break;
            if(!first) sb.append(",");
            sb.append(s);
            first = false;
        }
        sb.append("]");
        queued = sb.toString();
    }

    public int getEntity1m() {
        return entity1m;
    }

    public int getEntity5m() {
        return entity5m;
    }

    public int getEntity1h() {
        return entity1h;
    }

    public long getEntity1d() {
        return entity1d;
    }

    public long getEntityAll() {
        return entityAll;
    }

}

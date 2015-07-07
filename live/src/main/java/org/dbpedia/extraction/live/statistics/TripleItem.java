package org.dbpedia.extraction.live.statistics;

/**
 * Created by Andre Pereira on 07/07/2015.
 */
public class TripleItem {
    private int numOfTriples;
    private long timestamp;

    public TripleItem(int num, int time){
        numOfTriples = num;
        timestamp = time;
    }

    public int getNumOfTriples() {
        return numOfTriples;
    }

    public long getTimestamp() {
        return timestamp;
    }
}

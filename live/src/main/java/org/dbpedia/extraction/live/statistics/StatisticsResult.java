package org.dbpedia.extraction.live.statistics;

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

    public StatisticsResult(){}

    public StatisticsResult(int m1, int m5, int h1, long d1, long all){
        entity1m = m1;
        entity5m = m5;
        entity1h = h1;
        entity1d = d1;
        entityAll = all;
    }

    @Override
    public String toString() {
        return "StatisticsResult{" +
                "\nentity1m=" + entity1m +
                ", \nentity5m=" + entity5m +
                ", \nentity1h=" + entity1h +
                ", \nentity1d=" + entity1d +
                ", \nentityAll=" + entityAll +
                ", \ntriples1m=" + triples1m +
                ", \ntriples5m=" + triples5m +
                ", \ntriples1h=" + triples1h +
                ", \ntriples1d=" + triples1d +
                ", \ntriplesAll=" + triplesAll +
                '}';
    }

    public void setTriples(int tm1, int tm5, int th1, long td1, long tAll){
        triples1m = tm1;
        triples5m = tm5;
        triples1h = th1;
        triples1d = td1;
        triplesAll = tAll;
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

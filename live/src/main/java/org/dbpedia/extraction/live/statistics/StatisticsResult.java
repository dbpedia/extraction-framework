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

    public StatisticsResult(int m1, int m5, int h1, long d1, long all){
        entity1m = m1;
        entity5m = m5;
        entity1h = h1;
        entity1d = d1;
        entityAll = all;
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

    @Override
    public String toString() {
        return "StatisticsResult{\n" +
                "entity1m=" + entity1m +
                ",\n entity5m=" + entity5m +
                ",\n entity1h=" + entity1h +
                ",\n entity1d=" + entity1d +
                ",\n entityAll=" + entityAll +
                '}';
    }
}

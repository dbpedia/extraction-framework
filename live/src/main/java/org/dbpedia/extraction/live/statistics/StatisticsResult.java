package org.dbpedia.extraction.live.statistics;

/**
 * Created by Andre on 07/07/2015.
 */
public class StatisticsResult {
    private int instance1m;
    private int instance5m;
    private int instance1h;
    private long instance1d;
    private long instanceAll;

    public StatisticsResult(int m1, int m5, int h1, long d1, long all){
        instance1m = m1;
        instance5m = m5;
        instance1h = h1;
        instance1d = d1;
        instanceAll = all;
    }

    public int getInstance1m() {
        return instance1m;
    }

    public int getInstance5m() {
        return instance5m;
    }

    public int getInstance1h() {
        return instance1h;
    }

    public long getInstance1d() {
        return instance1d;
    }

    public long getInstanceAll() {
        return instanceAll;
    }

    @Override
    public String toString() {
        return "StatisticsResult{\n" +
                "instance1m=" + instance1m +
                ",\n instance5m=" + instance5m +
                ",\n instance1h=" + instance1h +
                ",\n instance1d=" + instance1d +
                ",\n instanceAll=" + instanceAll +
                '}';
    }
}

package org.dbpedia.extraction.live.statistics;

/**
 * Created with IntelliJ IDEA.
 * User: Dimitris Kontokostas
 * Date: 8/1/12
 * Time: 5:20 PM
 * This is a temporary solution to stay compatible with the existing code. This class holds
 * data to calculate statistics on live extraction
 */
public class StatisticsItem implements Comparable<StatisticsItem> {
    private String pageName = "";
    private String pageDBpedia = "";
    private String pageWikipedia = "";
    private int pageID = 0;
    private long pageTimestamp = 0;
    private boolean pageHasDelta = false;

    public StatisticsItem(String name, String dbpediaURI, String wikipediaURI, int id, long timestamp, boolean delta) {
        pageName = name;
        pageDBpedia = dbpediaURI;
        pageWikipedia = wikipediaURI;
        pageID = id;
        pageTimestamp = timestamp;
        pageHasDelta = delta;
    }

    public StatisticsItem(String name, String dbpediaURI, String wikipediaURI, int id, long timestamp) {
        this(name, dbpediaURI, wikipediaURI, id, timestamp, false);
    }

    public StatisticsItem() {
        this("", "", "", 0, System.currentTimeMillis(), false);
    }


    public boolean getHasDelta() {
        return pageHasDelta;
    }

    public void setHasDelta(boolean hasDelta) {
        pageHasDelta = hasDelta;
    }

    public long getTimestamp() {
        return pageTimestamp;
    }

    public boolean isOlderThanMillis(long refTime, long duration) {
        return (refTime - pageTimestamp) > duration;
    }

    public String getPageTitle() {
        return pageName;
    }

    public String getDBpediaURI() {
        return pageDBpedia;
    }

    public String getWikipediaURI() {
        return pageWikipedia;
    }

    public int getPageID() {
        return pageID;
    }

    public int compareTo(StatisticsItem item) {
        if (item.getTimestamp() == this.getTimestamp())
            return 0;
        return (item.getTimestamp() > this.getTimestamp()) ? 1 : -1;
    }

    @Override
    public boolean equals(Object item) {
        try {
            //StatisticsItem passedInstance = (StatisticsItem) item;
            return this.getPageID() == ((StatisticsItem) item).getPageID();
        } catch (Exception exp) {
            return false;
        }
    }

    public String toString() {
        return pageName + "\t" + pageDBpedia + "\t" + pageWikipedia + "\t" + pageTimestamp + "\t" + pageHasDelta;
    }
}

package org.dbpedia.extraction.live.statistics;

/**
 * Created by Andre on 21/07/2015.
 */
public class ExtractedItem {
    public String title, wikiURI, dbpediaURI;

    public ExtractedItem(String title, String wikiURI) {
        this.title = title;
        this.wikiURI = wikiURI;
        int index = wikiURI.indexOf("/wiki/");
        this.dbpediaURI = "http://live.dbpedia.org/resource/" + wikiURI.substring(index + 6);
    }

    @Override
    public String toString() {
        return "{" +
                "\"title\":\"" + title + '"' +
                ", \"wikiURI\":\"" + wikiURI + '"' +
                ", \"dbpediaURI\":\"" + dbpediaURI + '"' +
                '}';
    }
}

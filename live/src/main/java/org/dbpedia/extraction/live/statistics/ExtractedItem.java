package org.dbpedia.extraction.live.statistics;

/**
 * Created by Andre on 21/07/2015.
 */
public class ExtractedItem {
    public String title, wikiURI, dbpediaURI;

    public ExtractedItem(String title, String wikiURI, String dbpediaURI) {
        this.title = title;
        this.wikiURI = wikiURI;
        this.dbpediaURI = dbpediaURI;
    }

    @Override
    public String toString() {
        return "{" +
                "\"title\":\"" + title + '"' +
                ", \"wikiURI\":\"" + wikiURI + '"' +
                //", \"dbpediaURI\":\"" + dbpediaURI + '"' +
                '}';
    }
}

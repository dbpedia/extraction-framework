package org.dbpedia.extraction.nif;

import java.util.LinkedList;

/**
 * Created by Chile on 10/17/2016.
 */
public class Paragraph {
    private String text = "";
    private int begin = 0;
    private LinkedList<Link> internalLinks = new LinkedList<>();

    public Paragraph(int begin, String text){
        this.begin = begin;
        this.text = text;
    }

    public String getText() {
        return text.trim();
    }

    public int getBegin() {
        return begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getEnd() {
        return begin + this.text.trim().length();
    }

    public void addLink(Link link) {
        internalLinks.add(link);
    }

    public void addText(String text) {
        this.text += text;
    }

    public LinkedList<Link> getLinks() {
        return this.internalLinks;
    }
}

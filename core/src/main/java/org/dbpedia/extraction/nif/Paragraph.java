package org.dbpedia.extraction.nif;

import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by Chile on 10/17/2016.
 */
public class Paragraph {
    private String text = "";
    private int begin = 0;
    private LinkedList<Link> internalLinks = new LinkedList<>();
    private HashMap<Integer,String> tableHtml = new HashMap<>();

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

    public int getBegin(Integer offset) {
        return offset + begin;
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public int getEnd() {
        return begin + this.getLength();
    }

    public int getEnd(Integer offset) {
        return offset + begin + this.getLength();
    }

    public void addLink(Link link) {
        internalLinks.add(link);
    }

    public void addTable(Integer position, String html) {
        tableHtml.put(position, html);
    }

    public void addText(String text) {
        this.text += text;
    }

    public LinkedList<Link> getLinks() {
        return this.internalLinks;
    }

    public int getLength(){
        return getText().trim().length() - StringUtils.countMatches(getText(), "\\");
    }

    public HashMap<Integer, String> getTableHtml() {
        return this.tableHtml;
    }
}

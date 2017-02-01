package org.dbpedia.extraction.nif;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * Created by Chile on 10/17/2016.
 */
public class Paragraph {
    String text = "";
    private String tagName = null;
    private int begin = 0;
    private LinkedList<Link> internalLinks = new LinkedList<>();
    private HashMap<Integer,String> tableHtml = new HashMap<>();

    public Paragraph(int begin, String text, String tag){
        this.begin = begin;
        this.text = text;
        this.tagName = tag;
    }

    public String getText() {
        return StringUtils.strip(text, " ");
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

    /**
     * adds new text snipped, returns the
     * @param text
     * @return
     */
    public int addText(String text) {
        this.text += text;
        return GetEscapedStringLength(text);
    }

    public int finalizeParagraph(){
        if(Paragraph.FollowedByWhiteSpace(this.getText()))
            return 0;
        else
            return -1;
    }

    public String getTagName() {
        return tagName;
    }

    public LinkedList<Link> getLinks() {
        return this.internalLinks;
    }

    public int getLength(){
        return GetEscapedStringLength(getText());
    }

    public HashMap<Integer, String> getTableHtml() {
        return this.tableHtml;
    }

    public boolean hasContent(){
        if(this.getLength() > 0)
            return true;
        if(this.tableHtml.size() > 0)
            return true;
        if(this.getLinks().size() > 0)
            return true;
        return false;
    }

    static ArrayList<Character> whiteSpacePrefixes = new ArrayList<Character>(Arrays.asList('(', '[', '{', ' ', '\n'));
    public static boolean FollowedByWhiteSpace(String text){
        if(text == null || text.length() == 0)
            return false;
        return !whiteSpacePrefixes.contains(text.charAt(text.length()-1));
    }

    public static int GetEscapedStringLength(String text){
        return text.length() - StringUtils.countMatches(text, "\\");
    }
}

package org.dbpedia.extraction.nif;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Created by Chile on 10/17/2016.
 */
public class Paragraph {
    String text = "";
    private String tagName = null;
    private int begin = 0;
    private LinkedList<Link> internalLinks = new LinkedList<>();
    private ArrayList<HtmlString> htmlStrings = new ArrayList<>();

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

    void addLink(Link link) {
        internalLinks.add(link);
    }

    void addStructure(Integer position, String html, String tag, String clazz, String id) {
        HtmlString ht = new HtmlString(html, tag, position);
        ht.setOuterClass(clazz);
        ht.setOuterId(id);
        htmlStrings.add(ht);
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

    public String getTagName() {
        return tagName;
    }

    public LinkedList<Link> getLinks() {
        return this.internalLinks;
    }

    public int getLength(){
        return GetEscapedStringLength(getText());
    }

    public ArrayList<HtmlString> getHtmlStrings() {
        return this.htmlStrings;
    }

    public boolean hasContent(){
        if(this.getLength() > 0)
            return true;
        if(this.htmlStrings.size() > 0)
            return true;
        if(this.getLinks().size() > 0)
            return true;
        return false;
    }

    private static ArrayList<Character> whiteSpacePrefixes = new ArrayList<Character>(Arrays.asList('(', '[', '{', ' ', '\n'));
    public static boolean FollowedByWhiteSpace(String text){
        if(text == null || text.length() == 0)
            return false;
        return !whiteSpacePrefixes.contains(text.charAt(text.length()-1));
    }

    public static int GetEscapedStringLength(String text){
        return text.length() - StringUtils.countMatches(text, "\\");
    }

    public static class HtmlString{
        private String html;
        private String outerTag;
        private String outerClass;
        private String outerId;
        private int offset;

        public HtmlString(String html, String tag, int offset){
            this.html = html;
            this.outerTag = tag;
            this.offset = offset;
        }

        public String getHtml() {
            return html;
        }

        public void setHtml(String html) {
            this.html = html;
        }

        public String getOuterTag() {
            return outerTag;
        }

        public void setOuterTag(String outerTag) {
            this.outerTag = outerTag;
        }

        public String getOuterClass() {
            return outerClass;
        }

        public void setOuterClass(String outerClass) {
            this.outerClass = outerClass;
        }

        public String getOuterId() {
            return outerId;
        }

        public void setOuterId(String outerId) {
            this.outerId = outerId;
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }
    }
}

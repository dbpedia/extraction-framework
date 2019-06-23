package org.dbpedia.extraction.live.queue;

import org.dbpedia.extraction.util.Language;

import java.util.Objects;

/**
 * Created with IntelliJ IDEA.
 * User: Dimitris Kontokostas
 * Date: 11/19/12
 * Time: 6:18 PM
 * Item for update.
 */
public class LiveQueueItem implements Comparable<LiveQueueItem>{
    private Language wikiLanguage;
    private long itemID = 0;
    private LiveQueuePriority itemPriority;
    private String itemName = "";
    private String modificationDate = "";
    private boolean deleted = false;
    private long statQueueAdd = 0;
    private String xml = "";

    public LiveQueueItem(String wikiLanguage, long itemID, String modificationDate){
        this.wikiLanguage = theLanguage(wikiLanguage);
        this.itemID = itemID;
        this.modificationDate = modificationDate;
    }

    public LiveQueueItem(String wikiLanguage, long itemID, String itemName, String modificationDate, boolean deleted, String xml){
        this.wikiLanguage = theLanguage(wikiLanguage);
        this.itemID = itemID;
        this.itemName = itemName;
        this.modificationDate = modificationDate;
        this.deleted = deleted;
        this.xml = xml;
    }

    public  Language getWikiLanguage(){
        return wikiLanguage;
    }

    public long getItemID() {
        return itemID;
    }

    public void setPriority(LiveQueuePriority itemPriority){
        this.itemPriority = itemPriority;
    }

    public LiveQueuePriority getPriority(){
        return itemPriority;
    }

    public String getItemName() {
        return itemName;
    }

    public String getModificationDate() {
        return modificationDate;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setStatQueueAdd(long t){
        statQueueAdd = (t == -1) ? System.currentTimeMillis() : t;
    }

    public long getStatQueueAdd(){
        return statQueueAdd;
    }

    public String getXML(){
        return xml;
    }

    private Language theLanguage(String lang){
        Language theLanguage;
        if(lang.equals("")){
            theLanguage =  Language.apply("en"); //TODO this is working in our specific situation but should be generalized in the long term
        }else{
            theLanguage =  Language.apply(lang);
        }
        return theLanguage;
    }


    @Override
    public int compareTo(LiveQueueItem item) {
        // different priority
        if (this.itemPriority != item.itemPriority)
        	return this.itemPriority.compareTo(item.itemPriority);

        if (this.modificationDate == "" || item.modificationDate == "")
            return 0;
        else {
            // String compare should do for this (and it's thread safe)
            if (this.itemPriority == LiveQueuePriority.UnmodifiedPagePriority)
                // When in unmodified do reverse ordering to prevent older from starvation
                return item.modificationDate.compareTo(this.modificationDate);
            else
                return this.modificationDate.compareTo(item.modificationDate);
        }
    }

    /**
     * TODO it really seems that this is used nowhere,
     * Priority Queue uses compare to
     * Hashset uses ItemName, which is a string
     * @return
     */
    @Deprecated
    @Override
    public int hashCode() {
        return Objects.hash(wikiLanguage, itemID, itemPriority, itemName, modificationDate, deleted, statQueueAdd, xml);
    }

    /**
     * TODO it really seems that this is used nowhere,
     * Priority Queue uses compare to
     * Hashset uses ItemName, which is a string
     * @return
     */
    @Deprecated
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final LiveQueueItem other = (LiveQueueItem) obj;
        return Objects.equals(this.wikiLanguage, other.wikiLanguage)
                && Objects.equals(this.itemID, other.itemID)
                && Objects.equals(this.itemPriority, other.itemPriority)
                && Objects.equals(this.itemName, other.itemName)
                && Objects.equals(this.modificationDate, other.modificationDate)
                && Objects.equals(this.deleted, other.deleted)
                && Objects.equals(this.statQueueAdd, other.statQueueAdd)
                && Objects.equals(this.xml, other.xml);
    }

    @Override
    public String toString() {
        return "LiveQueueItem{\n" +
                "wikiLanguage=" + wikiLanguage +
                "\nitemID=" + itemID +
                "\nitemPriority=" + itemPriority +
                "\nitemName='" + itemName + '\'' +
                "\nmodificationDate='" + modificationDate + '\'' +
                "\ndeleted=" + deleted +
                "\nstatQueueAdd=" + statQueueAdd +
                "\nxml='" + xml + '\'' +
                '}';
    }
}

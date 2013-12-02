package org.dbpedia.extraction.live.queue;

/**
 * Created with IntelliJ IDEA.
 * User: Dimitris Kontokostas
 * Date: 11/19/12
 * Time: 6:18 PM
 * Item for update.
 */
public class LiveQueueItem implements Comparable<LiveQueueItem>{
    private long itemID = 0;
    private LiveQueuePriority itemPriority;
    private String itemName = "";
    private String modificationDate = "";
    private boolean deleted = false;
    private long statQueueAdd = 0;
    private String xml = "";

    public LiveQueueItem(long itemID, String modificationDate){
        this.itemID = itemID;
        this.modificationDate = modificationDate;
    }

    public LiveQueueItem(long itemID, String itemName, String modificationDate, boolean deleted, String xml){
        this.itemID = itemID;
        this.itemName = itemName;
        this.modificationDate = modificationDate;
        this.deleted = deleted;
        this.xml = xml;
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

    @Override
    public int compareTo(LiveQueueItem item) {
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
}

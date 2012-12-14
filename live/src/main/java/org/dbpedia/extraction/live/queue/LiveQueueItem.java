package org.dbpedia.extraction.live.queue;

/**
 * Created with IntelliJ IDEA.
 * User: Dimitris Kontokostas
 * Date: 11/19/12
 * Time: 6:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class LiveQueueItem {
    private long itemID = 0;
    private String itemName = "";
    private String modificationDate = "";
    private boolean deleted = false;
    private String xml = "";

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

    public String getItemName() {
        return itemName;
    }

    public String getModificationDate() {
        return modificationDate;
    }

    public boolean isDeleted() {
        return deleted;
    }
}

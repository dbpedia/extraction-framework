package org.dbpedia.extraction.live.feeder;

/**
 * Created with IntelliJ IDEA.
 * User: Dimitris Kontokostas
 * Date: 11/19/12
 * Time: 6:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class FeederItem {
    private long itemID = 0;
    private String itemName = "";
    private String modificationDate = "";
    private boolean deleted = false;

    public FeederItem(long itemID, String itemName, String modificationDate, boolean deleted){
        this.itemID = itemID;
        this.itemName = itemName;
        this.modificationDate = modificationDate;
        this.deleted = deleted;
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

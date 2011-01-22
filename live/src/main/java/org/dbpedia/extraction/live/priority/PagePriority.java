package org.dbpedia.extraction.live.priority;


import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 28, 2010
 * Time: 5:00:34 PM
 * This class represents the priority of the page, because the page IDs that are extracted through live extraction
 * have higher priority than the page IDs that are extracted through mapping change.
 */
public class PagePriority implements Comparable<PagePriority>{
    public long pageID;
    public Boolean isResultOfMappingChange;
    public String lastResponseDate;

    public PagePriority(long pageid, Boolean fromMapping, String responseDate){
        pageID = pageid;
        isResultOfMappingChange = fromMapping;
        lastResponseDate = responseDate;
    }

    public PagePriority(long pageid, Boolean fromMapping){
        this(pageid, fromMapping, "");
    }

    //We should compare the priorities of the pages by the boolean flag, and if they have the same boolean flag
    //we use the timestamp associated with each one, in order to make sure that the one with the least timestamp
    //will be processed first. as the performnace of
    public int compareTo(PagePriority page){
        if(this.isResultOfMappingChange != page.isResultOfMappingChange)
            return this.isResultOfMappingChange.compareTo(page.isResultOfMappingChange);
        else{
            //Split the timestamp into parts i.e. year , month, day, hour, minute, second
            String [] arrThisPageParts = this.lastResponseDate.split("-|:|T|Z");
            Date thisPageDate = new Date(Integer.parseInt(arrThisPageParts[0])-1900 , Integer.parseInt(arrThisPageParts[1])-1,
                    Integer.parseInt(arrThisPageParts[2]), Integer.parseInt(arrThisPageParts[3]),
                    Integer.parseInt(arrThisPageParts[4]), Integer.parseInt(arrThisPageParts[5]));

            String [] requiredPageParts = page.lastResponseDate.split("-|:|T|Z");
            Date requiredPageDate = new Date(Integer.parseInt(requiredPageParts[0])-1900 , Integer.parseInt(requiredPageParts[1])-1,
                    Integer.parseInt(requiredPageParts[2]), Integer.parseInt(requiredPageParts[3]),
                    Integer.parseInt(requiredPageParts[4]), Integer.parseInt(requiredPageParts[5]));

            //compare the two dates
            return thisPageDate.compareTo(requiredPageDate);
        }

    }


    public String toString(){
        return "Page ID = " + this.pageID + ", it's affected by mapping " + isResultOfMappingChange +
                ", and its timestamp = " + lastResponseDate;  

    }
}

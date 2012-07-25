package org.dbpedia.extraction.live.priority;


import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 28, 2010
 * Time: 5:00:34 PM
 * This class represents the priority of the page, because the page IDs that are extracted through live extraction
 * have higher priority than the page IDs that are extracted through mapping change, and also using Unmodified feeder.
 * Basically we use priority 0 for live, 1 for mapping change, and 2 for unmodified pages
 */
public class PagePriority implements Comparable<PagePriority>{

    private static Logger logger = Logger.getLogger(PagePriority.class);


    public long pageID;
    public Priority pagePriority;
    public String lastResponseDate;
//    private final long timestamp=0;

    // TODO: SimpleDateFormat is not thread-safe
    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public PagePriority(long pageid, Priority priority, String responseDate){
        pageID = pageid;
        pagePriority = priority;
        lastResponseDate = responseDate;
    }

    public PagePriority(long pageid, Priority priority){
        this(pageid, priority, "");
    }

    //We should compare the priorities of the pages by the boolean flag, and if they have the same boolean flag
    //we use the timestamp associated with each one, in order to make sure that the one with the least timestamp
    //will be processed first. as the performnace of
    public int compareTo(PagePriority page){
        if(this.pagePriority != page.pagePriority){
//            return this.pagePriority.compareTo(page.pagePriority);
            return this.pagePriority.compareTo(page.pagePriority);

        }
        else{
            try{

                Date thisPageDate = dateFormatter.parse(this.lastResponseDate);
                Date requiredPageDate = dateFormatter.parse(page.lastResponseDate);

                return thisPageDate.compareTo(requiredPageDate);
            }
            catch (Exception exp){
                logger.error("Failed to compare page priorities due to " + exp.getMessage());
                return 0;
            }
        }

    }


    public String toString(){
        return "Page ID = " + this.pageID + ", its priority = " + pagePriority + ", and its timestamp = " + lastResponseDate;

    }
}

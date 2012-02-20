package org.dbpedia.extraction.live.priority;


import org.apache.log4j.Logger;

import java.text.ParseException;
import java.text.SimpleDateFormat;

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
//    public String lastResponseDate;
    private long lastResponseDate = 0;



    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    public PagePriority(long pageid, Priority priority, long responseDate){
        pageID = pageid;
        pagePriority = priority;
        lastResponseDate = responseDate;
    }

    public PagePriority(long pageid, Priority priority, String responseDate){
        pageID = pageid;
        pagePriority = priority;
        try{
            lastResponseDate = dateFormatter.parse(responseDate).getTime();
        }
        catch (ParseException exp){
            logger.error("Unable to parse the passed date due to " + exp.getMessage());
        }

    }


    public PagePriority(long pageid, Priority priority){
        this(pageid, priority, 0);
    }

    //We should compare the priorities of the pages by the boolean flag, and if they have the same boolean flag
    //we use the timestamp associated with each one, in order to make sure that the one with the least timestamp
    //will be processed first. as the performance of
    public int compareTo(PagePriority page){
        if(this.pagePriority != page.pagePriority){
//            return this.pagePriority.compareTo(page.pagePriority);
            return this.pagePriority.compareTo(page.pagePriority);

        }
        else{
            try{

            //Split the timestamp into parts i.e. year , month, day, hour, minute, second
            /*String [] arrThisPageParts = this.lastResponseDate.split("-|:|T|Z");
            Date thisPageDate = new Date(Integer.parseInt(arrThisPageParts[0])-1900 , Integer.parseInt(arrThisPageParts[1])-1,
                    Integer.parseInt(arrThisPageParts[2]), Integer.parseInt(arrThisPageParts[3]),
                    Integer.parseInt(arrThisPageParts[4]), Integer.parseInt(arrThisPageParts[5]));

            String [] requiredPageParts = page.lastResponseDate.split("-|:|T|Z");
            Date requiredPageDate = new Date(Integer.parseInt(requiredPageParts[0])-1900 , Integer.parseInt(requiredPageParts[1])-1,
                    Integer.parseInt(requiredPageParts[2]), Integer.parseInt(requiredPageParts[3]),
                    Integer.parseInt(requiredPageParts[4]), Integer.parseInt(requiredPageParts[5]));

                int comparison = thisPageDate.compareTo(requiredPageDate);
                long diff = System.nanoTime() - startTime;
                logger.info("COMPARISON PERIOD = " + diff);*/

//                Date thisPageDate = dateFormatter.parse(this.lastResponseDate);

//                Date requiredPageDate = dateFormatter.parse(page.lastResponseDate);

                //compare the two dates

//                int comparison = thisPageDate.compareTo(requiredPageDate);


                return (this.lastResponseDate<page.getLastResponseDate() ? -1 : (this.lastResponseDate==page.getLastResponseDate()? 0 : 1));
//                return page.getTimestamp() > this.timestamp:
            }
            catch (Exception exp){
                logger.error("Failed to compare page priorities due to " + exp.getMessage());
                return 0;
            }
        }

    }

    public long getLastResponseDate() {
        return lastResponseDate;
    }

    public String toString(){
        return "Page ID = " + this.pageID + ", its priority = " + pagePriority + ", and its timestamp = " + lastResponseDate;

    }
}

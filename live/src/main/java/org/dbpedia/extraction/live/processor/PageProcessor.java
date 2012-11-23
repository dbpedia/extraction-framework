package org.dbpedia.extraction.live.processor;

import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.extraction.LiveExtractionManager;
import org.dbpedia.extraction.live.main.Main;
import org.dbpedia.extraction.live.priority.PagePriority;


/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 28, 2010
 * Time: 3:43:57 PM
 * This class dequeues the item at the front of the priority pageQueue and processes it i.e. apply the extraction
 * process on it.
 */
public class PageProcessor extends Thread{

    private static Logger logger = Logger.getLogger(PageProcessor.class);

    public PageProcessor(String name, int priority){
        this.setPriority(priority);
        this.setName(name);
        start();
    }

    public PageProcessor(String name){
        this(name, Thread.NORM_PRIORITY);
    }

    public PageProcessor(){
        this("PageProcessor", Thread.NORM_PRIORITY);
    }

    private void processPage(long pageID){
        try{
            LiveExtractionManager.extractFromPageID(
                    pageID,
                    LiveOptions.options.get("localApiURL"),
                    LiveOptions.options.get("language"));
        }
        catch(Exception exp){
            logger.error("Error in processing page number " + pageID + ", and the reason is " + exp.getMessage(), exp);
        }

    }


    public void run(){
        while(true){
            try{
                // block if empty
                PagePriority requiredPage = Main.pageQueue.take();

                //We should remove it also from existingPagesTree, but if it does not exist, then we should only remove it, without any further step
                if((Main.existingPagesTree != null) && (!Main.existingPagesTree.isEmpty()) && (Main.existingPagesTree.containsKey(requiredPage.pageID))){
                    Main.existingPagesTree.remove(requiredPage.pageID);
                    processPage(requiredPage.pageID);
                }
                logger.info("Page # " + requiredPage + " has been removed and processed");



            }
            catch (Exception exp){
                logger.error("Failed to process page");
            }


        }

    }

}

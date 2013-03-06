package org.dbpedia.extraction.live.processor;

import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.extraction.LiveExtractionManager;
import org.dbpedia.extraction.live.extraction.LiveExtractionConfigLoader;
import org.dbpedia.extraction.live.queue.LiveQueue;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;


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
    private volatile boolean keepRunning = true;

    public PageProcessor(String name){
        this.setName("PageProcessor_" + name);
    }

    public PageProcessor(){
        this("PageProcessor");
    }

    public void startProcessor() {
        if (keepRunning == true) {
            start();
        }
    }

    public void stopProcessor() {
        keepRunning = false;
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
        while(keepRunning){
            try{
                LiveQueueItem page = LiveQueue.take();
                // If a mapping page set extractor to reload mappings and ontology
                if (page.getPriority() == LiveQueuePriority.MappingPriority) {
                    LiveExtractionConfigLoader.reload(page.getStatQueueAdd());
                }
                processPage(page.getItemID());
                //logger.info("Page #" + page.getItemName() + " has been removed and processed");
            }
            catch (Exception exp){
                logger.error("Failed to process page");
            }


        }

    }

}

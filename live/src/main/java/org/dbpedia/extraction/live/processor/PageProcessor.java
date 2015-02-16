package org.dbpedia.extraction.live.processor;

import org.slf4j.Logger;
import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.extraction.LiveExtractionConfigLoader;
import org.dbpedia.extraction.live.queue.LiveQueue;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.dbpedia.extraction.live.storage.JSONCache;
import org.slf4j.LoggerFactory;


/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 28, 2010
 * Time: 3:43:57 PM
 * This class dequeues the item at the front of the priority pageQueue and processes it i.e. apply the extraction
 * process on it.
 */
public class PageProcessor extends Thread{

    private static Logger logger = LoggerFactory.getLogger(PageProcessor.class);
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


    private void processPage(LiveQueueItem item){
        try{
            Boolean extracted = LiveExtractionConfigLoader.extractPage(
                    item,
                    LiveOptions.options.get("localApiURL"),
                    LiveOptions.language);

            if (!extracted)
                JSONCache.setErrorOnCache(item.getItemID(), -1);
        }
        catch(Exception exp){
            logger.error("Error in processing page number " + item.getItemID() + ", and the reason is " + exp.getMessage(), exp);
            JSONCache.setErrorOnCache(item.getItemID(), -2);
        }
    }


    public void run(){
        LiveQueueItem currentPage = new LiveQueueItem(0,"");
        while(keepRunning){
            try{
                LiveQueueItem page = LiveQueue.take();
                currentPage = page;
                // If a mapping page set extractor to reload mappings and ontology
                if (page.getPriority() == LiveQueuePriority.MappingPriority) {
                    LiveExtractionConfigLoader.reload(page.getStatQueueAdd());
                }
                if (page.isDeleted() == true) {
                    JSONCache.deleteCacheItem(page.getItemID(),LiveExtractionConfigLoader.policies());
                    logger.info("Deleted page with ID: " + page.getItemID() + " (" + page.getItemName() + ")");
                }
                else
                    processPage(page);
            }
            catch (Exception exp){
                logger.error("Failed to process page " + currentPage.getItemID() + " reason: " + exp.getMessage(), exp);
            }
        }
    }
}

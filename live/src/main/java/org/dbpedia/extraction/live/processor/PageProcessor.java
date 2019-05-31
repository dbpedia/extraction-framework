package org.dbpedia.extraction.live.processor;

import org.dbpedia.extraction.live.extraction.LiveExtractionController;
import org.dbpedia.extraction.live.queue.LiveQueue;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.dbpedia.extraction.live.storage.JSONCache;
import org.slf4j.Logger;
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
        processPage(item, false);
    }

    private void processPageFromTitle(LiveQueueItem item){
        processPage(item, true);
    }

    private void processPage(LiveQueueItem item, boolean isTitle) {
        try{
            Boolean extracted = false;
            if (isTitle) {
                extracted = LiveExtractionController.extractPageFromTitle(
                        item,
                        item.getWikiLanguage().apiUri(),
                        item.getWikiLanguage().wikiCode());
            } else {
                extracted = LiveExtractionController.extractPage(
                        item,
                        item.getWikiLanguage().apiUri(),
                        item.getWikiLanguage().wikiCode()); //TODO pass only item
            }

            if (!extracted)
                JSONCache.setErrorOnCache(item, -1);
        }
        catch(Exception exp){
            logger.error("Error in processing page number " + item.getItemID() + " with title '" + item.getItemName() +  "' and language '"+item.getWikiLanguage() + "', and the reason is " + exp.getMessage(), exp);
            JSONCache.setErrorOnCache(item, -2);
        }
    }


    public void run(){
        LiveQueueItem currentPage = new LiveQueueItem("en", 0,"");
        LiveQueueItem lastPage = null;
        while(keepRunning){
            try{
                LiveQueueItem page = LiveQueue.take();
                if (page.equals(lastPage)) {
                    logger.info("Ignoring duplicate page {} ({}) with priority {}", page.getItemName(), page.getItemID(), page.getPriority());
                    continue;
                }
                lastPage = page;
                currentPage = page;
                // If a mapping page set extractor to reload mappings and ontology
                if (page.getPriority() == LiveQueuePriority.MappingPriority) {
                    LiveExtractionController.reload(page.getStatQueueAdd());
                }
                if (page.isDeleted() == true) {
                    JSONCache.deleteCacheItem(page, LiveExtractionController.policies());
                    logger.info("Deleted page with ID: " + page.getItemID() + " (" + page.getItemName() + ")");
                }
                else {
                    if (!page.getItemName().isEmpty()) {
                        processPageFromTitle(page);
                    } else {
                        processPage(page);
                    }
                }
            }
            catch (Exception exp){
                logger.error("Failed to process page " + currentPage.getItemID() + " reason: " + exp.getMessage(), exp);
                continue;
            }
        }
    }
}

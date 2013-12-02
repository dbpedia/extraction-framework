package org.dbpedia.extraction.live.feeder;

import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.helper.MappingAffectedPagesHelper;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import scala.collection.JavaConversions;

/**
 * Created with IntelliJ IDEA.
 * User: Dimitris Kontokostas
 * Date: 9/10/12
 * Time: 10:08 AM
 * An OAI Feeder for the mappings wiki
 */
public class OAIFeederMappings extends OAIFeeder {

    private String mappingNamespace = "";

    public OAIFeederMappings(String feederName, LiveQueuePriority queuePriority,
                             String oaiUri, String oaiPrefix, String baseWikiUri,
                             long pollInterval, long sleepInterval, String defaultStartTime,
                             String folderBasePath) {

        super(feederName, queuePriority,
                oaiUri, oaiPrefix, baseWikiUri,
                pollInterval, sleepInterval, defaultStartTime,
                folderBasePath);

        String langCode = LiveOptions.options.get("language");
        mappingNamespace = "Mapping " + langCode + ":";

    }

    @Override
    protected void handleFeedItem(LiveQueueItem item) {

        // ignore irrelevant mappings
        if (!item.getItemName().startsWith(mappingNamespace))
            return;

        String title = item.getItemName().substring(item.getItemName().indexOf(":")+1);

        //if (!item.isDeleted()) {
            for (Object newItem: JavaConversions.asJavaIterable(MappingAffectedPagesHelper.GetMappingPages(title))) {
                addPageIDtoQueue(new LiveQueueItem((Long) newItem, item.getModificationDate()));
            }
        //} else {
            // TODO find which template the deleted infobox was referring to
        //}
    }
}

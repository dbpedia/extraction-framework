package org.dbpedia.extraction.live.feeder;

import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.helper.MappingAffectedPagesHelper;
import org.dbpedia.extraction.live.priority.Priority;
import org.dbpedia.extraction.live.util.XMLUtil;
import org.dbpedia.extraction.sources.XMLSource;
import org.w3c.dom.Document;

/**
 * Created with IntelliJ IDEA.
 * User: Dimitris Kontokostas
 * Date: 9/10/12
 * Time: 10:08 AM
 * An OAI Feeder for the mappings wiki
 */
public class OAIFeederMappings extends OAIFeeder {

    private String mappingNamespace = "";

    public OAIFeederMappings(String feederName, int threadPriority, Priority queuePriority,
                             String oaiUri, String oaiPrefix, String baseWikiUri,
                             long pollInterval, long sleepInterval, String defaultStartDateTime, long relativeEndFromNow,
                             String folderBasePath) {

        super(feederName, threadPriority, queuePriority,
                oaiUri, oaiPrefix, baseWikiUri,
                pollInterval, sleepInterval, defaultStartDateTime, relativeEndFromNow,
                folderBasePath);

        String langCode = LiveOptions.options.get("language");
        mappingNamespace = "Mapping " + langCode + ":";

    }

    @Override
    protected void handleFeedItem(FeederItem item) {

        // ignore irrelevant mappings
        if (!item.getItemName().startsWith(mappingNamespace))
            return;

        if (!item.isDeleted()) {
            scala.collection.immutable.List<Object> ids = MappingAffectedPagesHelper.GetMappingPages(item.getItemName());
            scala.collection.Iterator<Object> iter = ids.iterator();
            while (iter.hasNext())
                addPageIDtoQueue((Long) iter.next(), latestResponseDate);
            iter = null;
            ids  = null;
        } else {
            // TODO find which template the deleted infobox was referring to
        }

        latestResponseDate = item.getModificationDate();
    }
}

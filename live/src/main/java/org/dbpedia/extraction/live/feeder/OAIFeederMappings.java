package org.dbpedia.extraction.live.feeder;

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
public class OAIFeederMappings extends OAIFeeder  {

    public OAIFeederMappings(String feederName, int threadPriority, Priority queuePriority,
                     String oaiUri, String oaiPrefix, String baseWikiUri,
                     long pollInterval, long sleepInterval, String defaultStartDateTime, long relativeEndFromNow,
                     String folderBasePath) {

        super(feederName, threadPriority, queuePriority,
             oaiUri, oaiPrefix, baseWikiUri,
             pollInterval, sleepInterval, defaultStartDateTime, relativeEndFromNow,
             folderBasePath);

    }

    @Override
    protected void handleFeedItem(Document doc) {

        scala.xml.Node element = scala.xml.XML.loadString(XMLUtil.toString(doc));
        org.dbpedia.extraction.sources.Source wikiPageSource = XMLSource.fromOAIXML((scala.xml.Elem) element);

        String tmpDate = getPageModificationDate(doc);
        if (tmpDate != null || tmpDate != "")
            latestResponseDate = tmpDate;

        // TODO move this function here
        if (!isPageDeleted(doc))
            MappingAffectedPagesHelper.GetMappingPages(wikiPageSource, latestResponseDate );
        else {
            // TODO find which template the deleted infobox was referring to
        }


    }
}

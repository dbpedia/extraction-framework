package org.dbpedia.extraction.live.util.iterators;

import org.slf4j.Logger;
import org.dbpedia.extraction.live.util.DateUtil;
import org.dbpedia.extraction.live.util.OAIUtil;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import java.util.Collections;
import java.util.Iterator;

/**
 * An iterator which blocks until more data becomes available. poll interval in
 * ms can be set.
 * <p/>
 * This is an interator over iterators. Use a chain iterator to make that look
 * as a single iterator.
 */
public class EndlessOAIMetaIterator
        extends PrefetchIterator<Document> {
    private static Logger logger = LoggerFactory.getLogger(EndlessOAIMetaIterator.class);

    private String oaiBaseUri = null;
    private String startDate = null;
    private String endDate = null;
    private long pollDelay = 0;
    private long resumptionDelay = 0;
    private OAIRecordIterator lastIterator = null;

    public EndlessOAIMetaIterator(String oaiBaseUri, String startDate,
                                  long pollDelay, long resumptionDelay) {
        this.oaiBaseUri = oaiBaseUri;
        this.startDate = startDate;
        this.pollDelay = pollDelay;
        this.resumptionDelay = resumptionDelay;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getendDate() {
        return endDate;
    }

    @Override
    protected Iterator<Document> prefetch() {
        // Whenever this method is called it means that the iterator we
        // returned reached its end.
        // Here we get its lastResponseDate for creating a new iterator

        if (lastIterator != null)
            startDate = lastIterator.getLastResponseDate();

        logger.trace("Using date: " + startDate);

        Iterator<Document> iterator;
        OAIRecordIterator it = new OAIRecordIterator(oaiBaseUri, startDate, null, pollDelay, resumptionDelay);
        iterator = it;


        //if (resumptionDelay > 0)
        //    iterator = new DelayIterator<Document>(iterator, resumptionDelay);

        lastIterator = it;

        return Collections.singletonList(iterator).get(0);
    }
}

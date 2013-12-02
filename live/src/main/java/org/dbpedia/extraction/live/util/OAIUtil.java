package org.dbpedia.extraction.live.util;


import org.apache.commons.collections15.iterators.TransformIterator;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.transformer.NodeToDocumentTransformer;
import org.dbpedia.extraction.live.transformer.NodeToLiveQueueItemTransformer;
import org.dbpedia.extraction.live.util.iterators.*;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpression;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;


public class OAIUtil {
    private static String getStartDate(String date) {
        return date == null ? DateUtil.transformToUTC(System
                .currentTimeMillis()) : date;
    }

    public static Iterator<LiveQueueItem> createEndlessFeederItemIterator(
            String oaiBaseUri, String startDate, long pollDelay, long resumptionDelay) {

        XPathExpression expr = DBPediaXPathUtil.getRecordExpr();

        Iterator<Document> metaIterator = new EndlessOAIMetaIterator(
                        oaiBaseUri, startDate, pollDelay, resumptionDelay);

        Iterator<Node> nodeIterator = new XPathQueryIterator(metaIterator, expr);

        // 'Dirty' because it can contain duplicates.
        Iterator<LiveQueueItem> dirtyRecordIterator = new TransformIterator<Node, LiveQueueItem>(
                nodeIterator, new NodeToLiveQueueItemTransformer());

         // This iterator removed them
        Iterator<LiveQueueItem> recordIterator = new DuplicateFeederItemRemoverIterator(
                dirtyRecordIterator);

        return recordIterator;
    }

    public static Iterator<Document> createEndlessRecordIterator(
            String oaiBaseUri, String startDate, long relativeEndFromNow, long pollDelay, long resumptionDelay) {

        XPathExpression expr = DBPediaXPathUtil.getRecordExpr();

        //Iterator<Document> metaIterator = createEndlessIterator(oaiBaseUri,
        //        startDate, relativeEndFromNow, pollDelay, resumptionDelay);
        Iterator<Document> metaIterator = new EndlessOAIMetaIterator(
                        oaiBaseUri, startDate, pollDelay, resumptionDelay);

        Iterator<Node> nodeIterator = new XPathQueryIterator(metaIterator, expr);

        // 'Dirty' because it can contain duplicates.
        Iterator<Document> dirtyRecordIterator = new TransformIterator<Node, Document>(
                nodeIterator, new NodeToDocumentTransformer());

        // This iterator removed them
        Iterator<Document> recordIterator = new DuplicateOAIRecordRemoverIterator(
                dirtyRecordIterator);

        return recordIterator;
    }
/*
    public static Iterator<Document> createEndlessIterator(String oaiBaseUri, String startDate, long relativeEndFromNow,
                                                           int pollDelay, int resumptionDelay) {
        startDate = getStartDate(startDate);

        // This iterator always fetches fresh data from the oai repo
        // when next() is called
        Iterator<Document> metaIterator = new EndlessOAIMetaIterator(
                oaiBaseUri, startDate, relativeEndFromNow, resumptionDelay);

        // This iterator puts a minimum delay between two next calls
        if (pollDelay > 0)
            metaIterator = new DelayIterator<Iterator<Document>>(metaIterator,
                    pollDelay);

        // This iterator makes the multiple iterators look like a single one
        ChainIterator<Document> chainIterator = new ChainIterator<Document>(
                metaIterator);

        return chainIterator;
    }

    public static Iterator<Document> createIterator(String oaiBaseUri,
                                                    String startDate, int resumptionDelay) {
        startDate = getStartDate(startDate);

        Iterator<Document> iterator = new OAIRecordIterator(oaiBaseUri,
                startDate);

        if (resumptionDelay > 0)
            iterator = new DelayIterator<Document>(iterator, resumptionDelay);

        return iterator;
    }
      */
    public static String getOAIDateFormatString() {
        return "yyyy-mm-dd'T'HH:mm:ss'Z'";
    }

    public static DateFormat getOAIDateFormat() {
        return new SimpleDateFormat(getOAIDateFormatString());
    }
}

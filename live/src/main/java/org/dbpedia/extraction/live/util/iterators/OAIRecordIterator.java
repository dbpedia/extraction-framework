package org.dbpedia.extraction.live.util.iterators;

import ORG.oclc.oai.harvester2.verb.ListRecords;
import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.util.DBPediaXPathUtil;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.util.Collections;
import java.util.Iterator;

/**
 * Iterator for iterating over a oai repository. The end is reached, if no
 * resumption token is found.
 *
 * @author raven
 */
public class OAIRecordIterator
        extends PrefetchIterator<Document> {
    private static Logger logger = Logger
            .getLogger(OAIRecordIterator.class);

    private boolean endReached = false;
    private String resumptionToken = null;
    private String oaiBaseUri;
    private String startDate;
    private String endDate;

    private String lastResponseDate = null;

    private XPathExpression lastResponseDateExpr = null;

    private long pollDelay = 0;
    private long prefetchDelay = 0;
    private boolean firstRun = true;

    public String getLastResponseDate() {
        return lastResponseDate;
    }

    public String getStartDate() {
        return startDate;
    }

    public OAIRecordIterator(String oaiBaseUri, String startDate, String endDate, long pollDelay, long prefetchDelay) {
        this.oaiBaseUri = oaiBaseUri;
        this.startDate = startDate;
        this.endDate = endDate;
        this.lastResponseDate = startDate;
        this.pollDelay = pollDelay;
        this.prefetchDelay = prefetchDelay;
        this.firstRun = true;

        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            lastResponseDateExpr = DBPediaXPathUtil.getOAIResponseDateExpr();
        } catch (Exception e) {
            logger.error(ExceptionUtil.toString(e), e);
        }
    }

    @Override
    public Document next() {
        if (pollDelay > 0) {
            try {
                Thread.sleep(pollDelay, 0);
            } catch (Exception e) {
                logger.warn(ExceptionUtil.toString(e));
            }
        }

        return super.next();
    }

    @Override
    public Iterator<Document> prefetch() {
        try {
            // Do not wait on init
            long curPrefetchDelay = firstRun ? 0 : prefetchDelay;
            firstRun = false;

            if (endReached)
                return null;

            if (curPrefetchDelay > 0) {
                Thread.sleep(curPrefetchDelay, 0);
            }

            ListRecords listRecords = null;

            // Note: We crash if ListRecords fail.
            if (resumptionToken == null) {
                listRecords = new ListRecords(oaiBaseUri, startDate, endDate, null, "mediawiki");
            } else
                listRecords = new ListRecords(oaiBaseUri, resumptionToken);

            logger.debug("Executed: " + listRecords.getRequestURL());

            resumptionToken = listRecords.getResumptionToken();

            if (resumptionToken != null && !resumptionToken.trim().isEmpty())
                logger.debug("Got resumptionToken: '" + resumptionToken + "'");
            else
                endReached = true;

            Document document = listRecords.getDocument();

            lastResponseDate = (String) lastResponseDateExpr.evaluate(document, XPathConstants.STRING);

            return Collections.singleton(document).iterator();
        } catch (Exception e) {
            logger.warn(ExceptionUtil.toString(e));
        }

        return null;
    }
}

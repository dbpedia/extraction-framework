package org.dbpedia.extraction.live.util.iterators;

import ORG.oclc.oai.harvester2.verb.ListRecords;
import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.w3c.dom.Document;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.util.Collections;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: 4/10/11
 * Time: 8:42 PM
 * This class resembles class OAIRecordIterator, but it returns the records that were not modified along time ago.
 * This requirement is needed to ensure that the pages that were not modified along time ago are also reprocessed, as if there is code change like
 * a change in an extractor those pages should be reprocessed to reflect that change
 */
public class OAIUnmodifiedRecordIterator extends PrefetchIterator<Document>{
    private static Logger logger					= Logger.getLogger(OAIRecordIterator.class);

	private boolean			endReached				= false;
	private String			resumptionToken			= null;
	private String			oaiBaseUri;
	private String			startDate;
	private String          untilDate;

	private String			lastResponseDate		= null;

	private XPathExpression lastResponseDateExpr	= null;

	public String getLastResponseDate()
	{
		return lastResponseDate;
	}

	public String getStartDate()
	{
		return startDate;
	}

	public OAIUnmodifiedRecordIterator(String oaiBaseUri, String startDate, String untilDate)
	{
		this.oaiBaseUri = oaiBaseUri;
		this.startDate = startDate;
        this.untilDate = untilDate;
		this.lastResponseDate = startDate;

		try {
			XPath xpath = XPathFactory.newInstance().newXPath();
			lastResponseDateExpr = xpath
					.compile("//*[local-name()='responseDate']/text()");
		}
		catch (Exception e) {
			logger.error(ExceptionUtil.toString(e));
		}
	}

	@Override
	public Iterator<Document> prefetch()
	{
		if (endReached)
			return null;

		try {
			ListRecords listRecords = null;

			// Note: We crash if ListRecords fail.
			if (resumptionToken == null) {
				listRecords = new ListRecords(oaiBaseUri, startDate, untilDate,
						null, "mediawiki");
			}
			else
				listRecords = new ListRecords(oaiBaseUri, resumptionToken);

			logger.debug("Executed: " + listRecords.getRequestURL());

			resumptionToken = listRecords.getResumptionToken();

			if (resumptionToken != null && !resumptionToken.trim().isEmpty())
				logger.debug("Got resumptionToken: '" + resumptionToken + "'");
			else
				endReached = true;

			Document document = listRecords.getDocument();

			lastResponseDate = (String) lastResponseDateExpr.evaluate(document,
					XPathConstants.STRING);

			return Collections.singleton(document).iterator();
		}
		catch (Exception e) {
			logger.warn(ExceptionUtil.toString(e));
		}

		return null;
	}
}

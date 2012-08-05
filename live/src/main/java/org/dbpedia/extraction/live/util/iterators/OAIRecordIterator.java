package org.dbpedia.extraction.live.util.iterators;

import java.util.Collections;
import java.util.Iterator;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;


import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.w3c.dom.Document;


import ORG.oclc.oai.harvester2.verb.ListRecords;

/**
 * Iterator for iterating over a oai repository. The end is reached, if no
 * resumption token is found.
 * 
 * @author raven
 * 
 */
public class OAIRecordIterator
	extends PrefetchIterator<Document>
{
	private static Logger	logger					= Logger
															.getLogger(OAIRecordIterator.class);

	private boolean			endReached				= false;
	private String			resumptionToken			= null;
	private String			oaiBaseUri;
	private String			startDate;

	private String			lastResponseDate		= null;

	private XPathExpression	lastResponseDateExpr	= null;

	public String getLastResponseDate()
	{
		return lastResponseDate;
	}

	public String getStartDate()
	{
		return startDate;
	}

	public OAIRecordIterator(String oaiBaseUri, String startDate)
	{
		this.oaiBaseUri = oaiBaseUri;
		this.startDate = startDate;
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
				listRecords = new ListRecords(oaiBaseUri, startDate, null,
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

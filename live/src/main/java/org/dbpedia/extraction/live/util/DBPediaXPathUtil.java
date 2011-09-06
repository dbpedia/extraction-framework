package org.dbpedia.extraction.live.util;

import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;

public class DBPediaXPathUtil
{
	private static Logger			logger	= Logger
													.getLogger(DBPediaXPathUtil.class);

	private static Map<String, XPathExpression> map =
		new HashMap<String, XPathExpression>();
	/*
	private static XPathExpression	oaiIdentifierExpr;
	private static XPathExpression	timestampExpr;

	private static XPathExpression	recordExpr;
	private static XPathExpression	deletedRecordExpr;

	private static XPathExpression	identifierExpr;
	private static XPathExpression	datestampExpr;
	 */
	
	
	private static XPathExpression compile(String expression)
	{
		try {
			XPath xPath = XPathFactory.newInstance().newXPath();
			return xPath.compile(expression);
		}
		catch (Exception e) {
			logger.error(ExceptionUtil.toString(e));
			throw new RuntimeException(e);
		}
	}

	private static XPathExpression get(String id, String query)
	{
		XPathExpression expr = map.get(id);
		if(expr == null) {
			expr = compile(query);
			map.put(id, expr);
		}
		
		return expr;
	}
	public static XPathExpression getPageIdExpr()
	{
		return get("pageId", "//*[local-name()='page']/*[local-name()='id']/text()");
	}
	
	public static XPathExpression getOAIIdentifierExpr()
	{
		return get("oaiIdentifier", "//*[local-name()='header']/*[local-name()='identifier']/text()");
	}

	public static XPathExpression getTimestampExpr()
	{
		return get("timestamp", "//*[local-name()='header']/*[local-name()='datestamp']/text()");
	}

	public static XPathExpression getRecordExpr()
	{
		return get("record", "//*[local-name()='record']");
	}

	public static XPathExpression getIsRecordDeletedExpr()
	{
		return get("recordDeleted", "//*[local-name()='header']/@status");
	}
	
	public static XPathExpression getIdentifierExpr()
	{
		return get("identifier", "//*[local-name()='identifier']");
	}

	public static XPathExpression getDatestampExpr()
	{
		return get("datestamp", "//*[local-name()='datestamp']");
	}

	public static XPathExpression getContributorNameExpr()
	{
		return get("contributorName", "//*[local-name()='contributor']/*[local-name()='username']");
	}
	
	public static XPathExpression getContributorIdExpr()
	{
		return get("contributorId", "//*[local-name()='contributor']/*[local-name()='id']");
	}
	
	public static XPathExpression getContributorIpExpr()
	{
		return get("contributorIp", "//*[local-name()='contributor']/*[local-name()='ip']");
	}
	
	public static XPathExpression getTextExpr()
	{
		return get("text", "//*[local-name()='text']/text()");
	}

	public static XPathExpression getRevisionExpr()
	{
		return get("revision", "//*[local-name()='revision']/*[local-name()='id']");
	}

	public static XPathExpression getLanguageExpr()
	{
		return get("lang", "//@*[local-name()= 'lang']");
	}

	public static XPathExpression getTitleExpr()
	{
		return get("title", "//*[local-name()='title']");
	}
}

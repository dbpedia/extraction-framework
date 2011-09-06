package org.dbpedia.extraction.live.util;


import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XPathUtil
{
	private static Logger	logger	= Logger.getLogger(XPathUtil.class);
	private static XPath	xPath	= null;

	private static XPath getXPath()
	{
		if (xPath == null)
			xPath = XPathFactory.newInstance().newXPath();
		
		return xPath;
	}
	
	public static XPathExpression compile(String expression)
		throws Exception
	{
		try {
			return getXPath().compile(expression);
		}
		catch (Exception e) {
			logger.error(ExceptionUtil.toString(e));
			throw new RuntimeException(e);
		}
	}

	/*
	public static String evalToString(Node node, XPathExpression expr)
		throws XPathExpressionException
	{
		return (String)expr.evaluate(node);
	}*/

	public static String evalToString(Node node, XPathExpression expr)
	{
		try {
			return (String)expr.evaluate(node);
		}
		catch (Exception e) {
			logger.error(ExceptionUtil.toString(e));
		}
		
		return null;
	}
	
	public static String evalToString(Node node, String query)
		throws XPathExpressionException
	{
        return (String)getXPath().evaluate(query, node, XPathConstants.STRING);
	}

	/**
	 * This version doesn't throw an exception on error
	 * 
	 * @param node
	 * @param query
	 * @param dummy
	 * @return
	 */
	public static String evalToString(Node node, String query, boolean dummy)
	{
		try {
			return evalToString(node, query);
		}
		catch(Exception e) {
			logger.error(ExceptionUtil.toString(e));
		}
        
		return null;
	}
	
	public static NodeList evalToNodes(Node node, String query)
	{
		try {
			return (NodeList)getXPath().evaluate(query, node, XPathConstants.NODESET);
		}
		catch(Exception e) {
			logger.error(ExceptionUtil.toString(e));
		}
		
		return null;
	}
}

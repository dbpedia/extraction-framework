package org.dbpedia.extraction.live.util.iterators;

import java.util.Iterator;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;


import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.dbpedia.extraction.live.util.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;




/**
 * An iterator for iterating over nodes which match a certain xpath expression.
 * 
 * @author raven
 * 
 */
public class XPathQueryIterator
	extends PrefetchIterator<Node>
{
	private static Logger		logger	= Logger
												.getLogger(XPathQueryIterator.class);

	private XPathExpression		xPathExpression;

	private Iterator<Document>	itDocument;

	public XPathQueryIterator(Iterator<Document> itDocument,
			XPathExpression xPathExpression)
	{
		this.itDocument = itDocument;
		this.xPathExpression = xPathExpression;
	}

	@Override
	protected NodeListIterator prefetch()
	{
		while (itDocument.hasNext()) {
			Document document = itDocument.next();

			// WhatLinksHereArticleLoader.printDom(document);
			NodeList nodeList = null;
			try {

				nodeList = (NodeList) xPathExpression.evaluate(document,
						XPathConstants.NODESET);

				logger.debug(nodeList.getLength() + " nodes matched");
			}
			catch (Exception e) {
				logger.warn(ExceptionUtil.toString(e));
			}

			if (nodeList == null || nodeList.getLength() == 0) {
				logger.warn("No nodes matched in a document");
				String data = XMLUtil.toString(document);
				logger.trace("Document was:\n" + data);

				continue;
			}

			return new NodeListIterator(nodeList);
		}

		return null;
	}

}

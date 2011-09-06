package org.dbpedia.extraction.live.transformer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;



/**
 * An iterator which takes an iterator of nodes and creates converts them to
 * documents.
 * 
 */
public class NodeToDocumentTransformer
	implements Transformer<Node, Document>
{
	private static Logger	logger	= Logger
											.getLogger(NodeToDocumentTransformer.class);

	public Document transform(Node node)
	{
		Document document = null;
		try {

			if (node == null)
				return null;

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			document = db.newDocument();

			Node clone = document.importNode(node, true);
			document.appendChild(clone);

			/*
			 * System.out.println("****"); System.out.println("****");
			 * WhatLinksHereArticleLoader.printDom(document);
			 * System.out.println("####"); System.out.println("####");
			 */
		}
		catch (ParserConfigurationException e) {
			logger.warn(ExceptionUtil.toString(e));
		}

		return document;
	}
}

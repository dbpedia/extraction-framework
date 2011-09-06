package org.dbpedia.extraction.live.transformer;


import java.util.Iterator;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;


import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.dbpedia.extraction.live.util.XMLUtil;
import org.dbpedia.extraction.live.util.XPathUtil;
import org.dbpedia.extraction.live.util.iterators.NodeListIterator;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;




public class XPathTransformer
	implements Transformer<Node, Iterator<Node>>
{
	private Logger logger = Logger.getLogger(XPathTransformer.class);
	
	private XPathExpression		xPathExpression;

	public XPathTransformer(String expr)
		throws Exception
	{
		this.xPathExpression = XPathUtil.compile(expr);
	}

	public XPathTransformer(XPathExpression xPathExpression)
	{
		this.xPathExpression = xPathExpression;
	}

	@Override
	public NodeListIterator transform(Node node)
	{
		NodeList result = null;
		try {

			result = (NodeList) xPathExpression.evaluate(node,
					XPathConstants.NODESET);

			logger.debug(result.getLength() + " nodes matched");
		}
		catch (Exception e) {
			logger.warn(ExceptionUtil.toString(e));
		}

		if (result == null || result.getLength() == 0) {
			logger.warn("No nodes matched in a document");
			String data = XMLUtil.toString(node);
			logger.trace("Document was:\n" + data);

			return null;
		}
		
		return new NodeListIterator(result);
	}
}

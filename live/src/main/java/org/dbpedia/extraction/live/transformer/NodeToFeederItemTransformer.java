package org.dbpedia.extraction.live.transformer;

import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.feeder.FeederItem;
import org.dbpedia.extraction.live.util.DBPediaXPathUtil;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.dbpedia.extraction.live.util.XMLUtil;
import org.dbpedia.extraction.live.util.XPathUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created with IntelliJ IDEA.
 * User: Dimitris Kontokostas
 * Date: 11/19/12
 * Time: 8:38 PM
 * An iterator which takes an iterator of nodes and creates converts them to a FeederItem.
 */
public class NodeToFeederItemTransformer implements Transformer<Node, FeederItem>
{
	private static Logger logger = Logger.getLogger(NodeToFeederItemTransformer.class);

	public FeederItem transform(Node node)
	{
		try {
			if (node == null)
				return null;

            long nodeItemID = Long.parseLong(XPathUtil.evalToString(node, DBPediaXPathUtil.getPageIdExpr()));
            String nodeItemName =XPathUtil.evalToString(node, DBPediaXPathUtil.getTitleExpr())                      ;
            String nodeModificationDate = XPathUtil.evalToString(node, DBPediaXPathUtil.getTimestampExpr());
            boolean nodeDeleted = XPathUtil.evalToString(node, DBPediaXPathUtil.getOAIIsRecordDeletedExpr()).equals("deleted");

            return new FeederItem(nodeItemID, nodeItemName, nodeModificationDate, nodeDeleted);
		}
		catch (Exception e) {
			logger.warn(ExceptionUtil.toString(e));
		}

		return null;
	}
}

package org.dbpedia.extraction.live.transformer;


import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.record.IRecord;
import org.dbpedia.extraction.live.util.DBPediaXMLUtil;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.w3c.dom.Document;



public class NodeToRecordTransformer
	implements Transformer<Document, IRecord>
{
	private Logger logger = Logger.getLogger(NodeToRecordTransformer.class);
	private String oaiUri;
	private String baseWikiUri;
	private String oaiPrefix;
	
	public NodeToRecordTransformer(String baseWikiUri, String oaiUri, String oaiPrefix)
	{
		this.baseWikiUri = baseWikiUri;
		this.oaiUri = oaiUri;
		this.oaiPrefix = oaiPrefix;
	}
	
	@Override
	public IRecord transform(Document node)
	{
		try {
			return DBPediaXMLUtil.processOAIRecord(node, baseWikiUri, oaiUri, oaiPrefix);
		}
		catch (Exception e) {
			logger.error(ExceptionUtil.toString(e));
		}
		return null;
	}
}

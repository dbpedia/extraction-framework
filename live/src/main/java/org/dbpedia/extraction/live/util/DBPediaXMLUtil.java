package org.dbpedia.extraction.live.util;

import org.dbpedia.extraction.live.record.*;
import org.w3c.dom.Node;

import javax.xml.xpath.XPathExpressionException;
import java.net.URI;
import java.net.URLEncoder;



public class DBPediaXMLUtil
{
	public static Record exportToRecord(Node node, String baseWikiUri, String oaiUri,
			String oaiPrefix)
		throws Exception
	{
		String datestamp = XPathUtil.evalToString(node, DBPediaXPathUtil.getOAIDatestampExpr());
		//System.out.println("DATESTAMP = " + datestamp);
		
		
		//System.out.println(XMLUtil.toString(node));

		String language = XPathUtil.evalToString(node, DBPediaXPathUtil.getLanguageExpr());
		String t = XPathUtil.evalToString(node, DBPediaXPathUtil.getTitleExpr());

		// http://en.wikipedia.org/wiki/Special:OAIRepository
		//MediawikiTitle title = MediawikiHelper.parseTitle(domainUri
		//		+ "/wiki/Special:OAIRepository", t);
		MediawikiTitle title = new MediawikiTitle("NOT SET", "NOT SET", 0, "NOT SET");//MediawikiHelper.parseTitle(oaiUri, t);

		String oaiId = oaiPrefix
				+ XPathUtil.evalToString(node, DBPediaXPathUtil.getPageIdExpr());
		//String wikipediaUri = domainUri + "/wiki/" + URLEncoder.encode(title.getFullTitle(), "UTF-8");
		String wikipediaUri = baseWikiUri + URLEncoder.encode(title.getFullTitle(), "UTF-8");
		String revision = XPathUtil.evalToString(node, DBPediaXPathUtil.getRevisionExpr());
		String username = XPathUtil.evalToString(node, DBPediaXPathUtil.getContributorNameExpr());
		String ip = XPathUtil.evalToString(node, DBPediaXPathUtil.getContributorIpExpr());
		String userId = XPathUtil.evalToString(node, DBPediaXPathUtil.getContributorIdExpr());

		String text = XPathUtil.evalToString(node, DBPediaXPathUtil.getTextExpr());

		RecordMetadata metadata = new RecordMetadata(language, title, oaiId,
				URI.create(wikipediaUri), revision, username, ip, userId);

		RecordContent content = new RecordContent(text, revision, XMLUtil.toString(node));

		return new Record(metadata, content);
	}
	

	
	public static IRecord processOAIRecord(Node node, String baseWikiUri, String oaiUri,
			String oaiPrefix)
		throws Exception
	{
		if(isRecordDeleted(node)) {
			String datestamp = XPathUtil.evalToString(node,DBPediaXPathUtil.getOAIDatestampExpr());
			String oaiId = XPathUtil.evalToString(node, DBPediaXPathUtil.getOAIIdentifierExpr());
			
			DeletionRecord result = new DeletionRecord(oaiId, datestamp);
			return result;
		}
		else
			return exportToRecord(node, baseWikiUri, oaiUri, oaiPrefix);
	}
	
	private static boolean isRecordDeleted(Node node)
		throws XPathExpressionException
	{
		String value = XPathUtil.evalToString(node, DBPediaXPathUtil.getOAIIsRecordDeletedExpr());
		
		return "deleted".equalsIgnoreCase(value);
	}
}

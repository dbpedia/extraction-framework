package org.dbpedia.extraction.live.util.iterators;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.util.DBPediaXPathUtil;
import org.dbpedia.extraction.live.util.XPathUtil;
import org.w3c.dom.Document;



/**
 * Skips successive (concerning timestamp) duplicate identifiers. In this
 * example the second '123' item will be skipped. Fri Jan 02 14:50:11 CET 2009:
 * oai:en.wikipedia.org:enwiki:123 Fri Jan 02 14:50:11 CET 2009:
 * oai:en.wikipedia.org:enwiki:456 Fri Jan 02 14:50:11 CET 2009:
 * oai:en.wikipedia.org:enwiki:123
 * 
 * @author raven
 * 
 */
public class DuplicateOAIRecordRemoverIterator
	extends PrefetchIterator<Document>
{
	private Logger				logger				= Logger
															.getLogger(DuplicateOAIRecordRemoverIterator.class);

	private Iterator<Document>	iterator;

	private Date				currentTimestamp	= new Date(0L);
	private Set<String>			currentIdentifiers	= new HashSet<String>();

	private DateFormat			dateFormat			= new SimpleDateFormat(
															"yyyy-mm-dd'T'HH:mm:ss'Z'");

	public DuplicateOAIRecordRemoverIterator(Iterator<Document> iterator)
	{
		this.iterator = iterator;
	}

	private String getIdentifier(Document document)
	{
		return XPathUtil.evalToString(document, DBPediaXPathUtil
				.getOAIIdentifierExpr());
	}

	private Date getTimestamp(Document document)
		throws Exception
	{
		String str = XPathUtil.evalToString(document, DBPediaXPathUtil
				.getTimestampExpr());

		return dateFormat.parse(str);
	}

	@Override
	protected Iterator<Document> prefetch()
		throws Exception
	{
		while (iterator.hasNext()) {
			Document document = iterator.next();

			String identifier = getIdentifier(document);
			Date timestamp = getTimestamp(document);

			if (timestamp.after(currentTimestamp)) {
				currentTimestamp = timestamp;
				currentIdentifiers.clear();
			}

			if (currentIdentifiers.contains(identifier)) {
				logger.debug("Skipping duplicate: " + timestamp + ": "
						+ identifier);
				continue;
			}

			// logger.info("Accepted: " + timestamp + ": " + identifier);
			// logger.info("Identifiers in set: " + currentIdentifiers.size());
			currentIdentifiers.add(identifier);

			return Collections.singleton(document).iterator();
		}

		return null;
	}

}

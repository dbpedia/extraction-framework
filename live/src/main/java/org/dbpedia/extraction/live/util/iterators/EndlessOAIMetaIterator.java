package org.dbpedia.extraction.live.util.iterators;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

/**
 * An iterator which blocks until more data becomes available. poll interval in
 * ms can be set.
 * 
 * This is an interator over iterators. Use a chain iterator to make that look
 * as a single iterator.
 * 
 */
public class EndlessOAIMetaIterator
	extends PrefetchIterator<Iterator<Document>>
{
	private static Logger		logger			= Logger
														.getLogger(EndlessOAIMetaIterator.class);

	private String				oaiBaseUri;
	private String				startDate;
	private String				nextDate;
	private int					resumptionDelay;
	private File                file;
	private OAIRecordIterator	lastIterator	= null;

	public EndlessOAIMetaIterator(String oaiBaseUri, String startDate,
			int resumptionDelay)
	{
		this.oaiBaseUri = oaiBaseUri;
		this.startDate = startDate;
		//this.nextDate = startDate;
        
		this.resumptionDelay = resumptionDelay;
		this.file = file;
	}

	public String getStartDate()
	{
		return startDate;
	}

	public String getNextDate()
	{
		return nextDate;
	}

	@Override
	protected Iterator<Iterator<Document>> prefetch()
	{
		// Whenever this method is called it means that the iterator we
		// returned reached its end.
		// Here we get its lastResponseDate for creating a new iterator
		if (lastIterator != null)
			nextDate = lastIterator.getLastResponseDate();
		else
			nextDate = startDate;

		logger.trace("Using date: " + nextDate);

		Iterator<Document> iterator;
		OAIRecordIterator it = new OAIRecordIterator(oaiBaseUri, nextDate);
		iterator = it;
		
//		if(file != null)
//			iterator = new SaveResponseTimeIterator(it, file);
		
		
		if (resumptionDelay > 0)
			iterator = new DelayIterator<Document>(iterator, resumptionDelay);

		lastIterator = it;
		// System.out.println("Using last response date: " + nextDate);

		return Collections.singleton(iterator).iterator();
	}
}

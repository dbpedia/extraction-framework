package org.dbpedia.extraction.live.util.iterators;

import java.util.Iterator;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;


/**
 * An abstract base class for iterating over containers of unknown size. This
 * works by prefetching junks of the container: Whenever the iterator reaches
 * the end of a chunk, the method "myPrefetch" is called.
 * 
 * 
 * Note that once the iterator is finished (myPrefetch returned null),
 * myPrefetch will never be called again. This means, that if myPrefetch is
 * called, the iterator hasn't reached its end yet.
 * 
 * 
 * @author raven_arkadon
 * @param <T>
 */
public abstract class SinglePrefetchIterator<T>
	implements Iterator<T>
{
	private static Logger logger = Logger.getLogger(SinglePrefetchIterator.class);
	private T	    current		= null;
	private boolean finished	= false;

	private boolean advance     = true;
	
	protected abstract T prefetch()
		throws Exception;

	protected SinglePrefetchIterator()
	{
	}

	protected T finish()
	{
		this.finished = true;

		return null;
	}
	
	private void _prefetch()
	{		
		try {
			current = prefetch();
		}
		catch(Exception e) {
			current = null;		
			logger.error(ExceptionUtils.getFullStackTrace(e));
		}
	}

	@Override
	public boolean hasNext()
	{
		if(advance) {
			_prefetch();
			advance = false;
		}

		return finished == false;
	}

	@Override
	public T next()
	{
		if(finished) {
			throw new IndexOutOfBoundsException();
		}
		
		if(advance)
			_prefetch();
		
		advance = true;
		return current;
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException("Not supported.");
	}
}

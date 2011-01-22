package org.dbpedia.extraction.live.util.iterators;

import java.util.Iterator;


import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.util.ExceptionUtil;



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
public abstract class PrefetchIterator<T>
	implements Iterator<T>
{
	private static Logger logger = Logger.getLogger(PrefetchIterator.class);
	private Iterator<T>	current		= null;
	private boolean		finished	= false;

	abstract protected Iterator<T> prefetch()
		throws Exception;

	protected PrefetchIterator()
	{
	}

	private void preparePrefetch()
	{
		if (finished)
			return;
		current = null;
		try {
			current = prefetch();
		}
		catch(Exception e) {
			logger.error(ExceptionUtil.toString(e));
		}
		if (current == null)
			finished = true;
	}

	private Iterator<T> getCurrent()
	{
		if (current == null || !current.hasNext())
			preparePrefetch();

		return current;
	}

	@Override
	public boolean hasNext()
	{
		return getCurrent() != null;
	}

	@Override
	public T next()
	{
		return getCurrent().next();
	}

	@Override
	public void remove()
	{
		throw new UnsupportedOperationException("Not supported.");
	}
}

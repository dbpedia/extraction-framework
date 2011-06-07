package org.dbpedia.extraction.live.util.iterators;

import java.util.Iterator;


import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.util.ExceptionUtil;



/**
 * This iterator waits a certain amount of time between 2 next calls.
 * 
 * @author raven
 * 
 * @param <T>
 */
public class DelayIterator<T>
	implements Iterator<T>
{
	private static Logger	logger		= Logger.getLogger(DelayIterator.class);
	private Iterator<T>		iterator;

	private int				delay;
	private boolean			firstRun	= true;
	
	public DelayIterator(Iterator<T> iterator, int delay)
	{
		this.iterator = iterator;
		this.delay = delay;
	}

	@Override
	public boolean hasNext()
	{
		return iterator.hasNext();
	}

	@Override
	public T next()
	{
		if (!firstRun) {
			try {
				logger.info("Waiting " + delay + "ms");
				Thread.sleep(delay, 0);
			}
			catch (Exception e) {
				logger.warn(ExceptionUtil.toString(e));
			}
		}
		firstRun = false;

		T result = iterator.next();

		return result;
	}

	@Override
	public void remove()
	{
		iterator.remove();
	}
}

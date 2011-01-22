package org.dbpedia.test;

import org.apache.commons.lang.exception.ExceptionUtils
import org.apache.log4j.Logger


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
abstract class PrefetchIterator[T]
	extends Iterator[T]
{
	private val logger = Logger.getLogger("PrefetchIterator");
	private var	current : Iterator[T] = null;
	private var finished : Boolean = false;

	protected def prefetch() : Iterator[T];

	/*
	protected def PrefetchIterator()
	{
	}
	*/

	private def preparePrefetch() : Unit =  {
		if (finished) {
			return;
		}
		
		current = null;	
		try {
			// Prefetch may return empty iterators - skip them.
			do {
				current = prefetch();
			} while(current != null && !current.hasNext); 
		}
		catch {
			case e => logger.error(ExceptionUtils.getFullStackTrace(e));
		}
		if (current == null)
			finished = true;
	}

	private def getCurrent() : Iterator[T] = {
		if (current == null || !current.hasNext)
			preparePrefetch();

		return current;
	}

	override def hasNext() : Boolean = {
		return getCurrent() != null;
	}

	override def next() : T = {
		return getCurrent().next();
	}

	/*
	def remove() : Unit = {
		throw new UnsupportedOperationException("Not supported.");
	}
	*/
}

package org.dbpedia.test;

import org.apache.log4j.Logger
import org.apache.commons.lang.exception.ExceptionUtils;


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
abstract class SinglePrefetchIterator[T >: Null <: AnyRef]
	extends Iterator[T]
{
	private val logger : Logger = Logger.getLogger("SinglePrefetchIterator");
	private var current : T = null;
	private var finished : Boolean = false;

	private var advance : Boolean = true;
	
	protected def prefetch() : T;
		//throws Exception;

	/*
	protected SinglePrefetchIterator()
	{
	}
	*/

	protected def finish() : T = {
		this.finished = true;

		return null;
	}
	
	private def _prefetch() : Unit = {
		try {
			current = prefetch();
		}
		catch {
			case e => {
				current = null;		
				logger.error(ExceptionUtils.getFullStackTrace(e));
			}
		}
	}

	
	override def hasNext() : Boolean = {
		if(advance) {
			_prefetch();
			advance = false;
		}

		return finished == false;
	}

	
	override def next() : T =  {
		if(finished) {
			throw new IndexOutOfBoundsException();
		}
		
		if(advance)
			_prefetch();
		
		advance = true;
		return current;
	}

	/*
	override def remove() : Unit = {
		throw new UnsupportedOperationException("Not supported.");
	}
	*/
}

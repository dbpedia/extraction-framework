package org.dbpedia.extraction.live.util.iterators;

import java.util.Collection;
import java.util.Iterator;

/**
 * This class is subject to removal.
 * Reason: A similar class seems to be present in commons.collections
 *         verify though.
 * 
 * @author raven
 *
 * @param <T>
 */
public class ChainIterator<T>
	extends PrefetchIterator<T>
{
	private Iterator<? extends Iterator<T>>	metaIterator;

	public ChainIterator(Iterator<? extends Iterator<T>> metaIterator)
	{
		this.metaIterator = metaIterator;
	}

	public ChainIterator(Collection<? extends Iterator<T>> metaContainer)
	{
		this.metaIterator = metaContainer.iterator();
	}

	@Override
	protected Iterator<T> prefetch()
	{
		if (!metaIterator.hasNext())
			return null;

		return metaIterator.next();
	}
}
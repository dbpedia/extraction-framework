package org.dbpedia.extraction.live.transformer;

import java.util.Iterator;

import org.apache.commons.collections15.Transformer;

public class IterableToIteratorTransformer<I>
	implements Transformer<Iterable<I>, Iterator<I>>
{
	@Override
	public Iterator<I> transform(Iterable<I> it)
	{
		return it.iterator();
	}
}

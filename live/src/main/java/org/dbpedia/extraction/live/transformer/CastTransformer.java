package org.dbpedia.extraction.live.transformer;

import org.apache.commons.collections15.Transformer;

public class CastTransformer<I, O>
	implements Transformer<I, O>
{

	@SuppressWarnings("unchecked")
	@Override
	public O transform(I item)
	{
		return (O)item;
	}
}

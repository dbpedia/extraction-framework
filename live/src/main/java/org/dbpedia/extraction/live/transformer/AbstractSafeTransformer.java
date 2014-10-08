package org.dbpedia.extraction.live.transformer;


import org.apache.commons.collections15.Transformer;
import org.slf4j.Logger;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.slf4j.LoggerFactory;


public abstract class AbstractSafeTransformer<I, O>
	implements Transformer<I, O>
{
	private final static Logger logger = LoggerFactory.getLogger(AbstractSafeTransformer.class);
	
	protected abstract O _transform(I arg)
		throws Exception;

	@Override
	public O transform(I arg)
	{
		try {
			return _transform(arg);
		}
		catch(Throwable t) {
			logger.error(ExceptionUtil.toString(t));
		}
		
		return null;
	}

}

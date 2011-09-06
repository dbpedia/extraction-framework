package org.dbpedia.extraction.live.transformer;


import org.apache.commons.collections15.Transformer;
import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.util.ExceptionUtil;



public abstract class AbstractSafeTransformer<I, O>
	implements Transformer<I, O>
{
	private final static Logger logger = Logger.getLogger(AbstractSafeTransformer.class); 
	
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

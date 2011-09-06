package org.dbpedia.extraction.live.util.collections;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

public class MultiMap<TKey, TValue>
	extends HashMap<TKey, Collection<TValue>>
	implements IMultiMap<TKey, TValue>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void put(TKey key, TValue value)
	{
		Collection<TValue> values = this.get(key);
		
		if(values == null) {
			values = //new ArrayList<TValue>();
				new HashSet<TValue>();
			this.put(key, values);
		}
		
		if(value != null)
			values.add(value);
	}
	
	public Collection<TValue> safeGet(Object key)
	{
		Collection<TValue> result = this.get(key);
		if(result == null)
			result = Collections.emptySet();
		
		return result;
	}
}

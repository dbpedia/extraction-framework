package org.dbpedia.extraction.live.record;

import java.util.HashSet;
import java.util.Set;


public class ObjectContainer<TBase>
{
	private Set<TBase> objects =
		new HashSet<TBase>();

	public void add(TBase o)
	{
		objects.add(o);
	}
	
	public void remvoe(TBase o)
	{
		objects.remove(o);
	}
	
	/**
	 * Find a representation whose class is a sub class of the given one 
	 * 
	 * @param <T>
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends TBase> Set<T> get(Class<T> clazz)
	{
		Set<T> result = new HashSet<T>();
		for(Object item : objects)
			if(item.getClass().isAssignableFrom(clazz))
				result.add((T)item);

		return result;
	}
	
	/**
	 * Returns only the first matching representation
	 * 
	 * FIXME Throw an exception if there are multiple matches
	 * 
	 * @param <T>
	 * @param clazz
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends TBase> T getSingle(Class<T> clazz)
	{
		for(Object item : objects)
			//if(item.getClass().isAssignableFrom(clazz))
			if(clazz.isAssignableFrom(item.getClass()))
				return (T)item;
		
		return null;
	}
}


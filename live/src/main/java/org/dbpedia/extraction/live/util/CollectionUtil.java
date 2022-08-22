package org.dbpedia.extraction.live.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.dbpedia.extraction.live.util.collections.SetDiff;





public class CollectionUtil
{
	public static <T> Set<T> set(T ... items)
	{
		return new HashSet<T>(Arrays.asList(items));		
	}

	public static <T> ArrayList<T> newList(T ... items)
	{
		return new ArrayList<T>(Arrays.asList(items));
	}
	
	/**
	 * Returns the size of the specified collection or 0 on null.
	 * @param <T>
	 * @param collection
	 * @return
	 */
	public static <T> int size(Collection<T> collection)
	{
		return collection == null ? 0 : collection.size();
	}
	
	public static <T> SetDiff<T> diff(Collection<? extends T> old, Collection<? extends T> n)
	{
		// All new triples which were not present in old
		Set<T> added = new HashSet<T>(n);
		added.removeAll(old);
		
		// All old triples which are not present in new
		Set<T> removed = new HashSet<T>(old);
		removed.removeAll(n);
		
		// Triples which are common in both
		Set<T> retained = new HashSet<T>(old);
		retained.retainAll(n);
		
		return new SetDiff<T>(added, removed, retained);
	}
}

package org.dbpedia.extraction.live.util.collections;

import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;




/**
 * T is the Timestamp object (e.g. Date, Long, etc).
 * Multiple keys may have the same timestamp.
 * 
 * 
 * 
 * 
 * @author raven
 * JFreeChart
 * @param <K>
 * @param <V>
 * @param <T>
 */
public class TimeStampMap<K, V, T, D>
	extends HashMap<K, V>
{
	private static final long	serialVersionUID	= -4277098373746171836L;

	private TimeStampSet<K, T, D> set;
	
	public TimeStampMap(TimeStampSet<K, T, D> set)
	{
		this.set = set;
		
	}
	
	@Override
	public V put(K key, V value)
	{
		set.add(key);
		
		return super.put(key, value);
	}
	
	@Override
	public V remove(Object key)
	{
		set.remove(key);
		
		return super.remove(key);
	}
	
	public Map<K, V> setCurrentTime(T time)
	{
		Set<K> keys = set.setCurrentTime(time);
		
		if(keys.isEmpty())
			return Collections.emptyMap();
		
		Map<K, V> result = new HashMap<K, V>();
		for(K key : keys) {
			result.put(key, this.get(key));
			
			this.remove(key);
		}
		
		return result;
	}

	public static <K, V> TimeStampMap<K, V, Date, Long> create(Class<K> keyClazz,
			Class<V> valueClazz, long maxDistance, boolean inclusive, boolean allowRenewal)
	{
		TimeStampSet<K, Date, Long> set = TimeStampSet.createSet(keyClazz, maxDistance, inclusive, allowRenewal);
		
		return new TimeStampMap<K, V, Date, Long>(set);
	}
}

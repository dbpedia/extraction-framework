package org.dbpedia.extraction.live.util.collections;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * T is the Timestamp object (e.g. Date, Long, etc). Multiple keys may have the
 * same timestamp.
 * 
 * 
 * 
 * 
 * @author raven JFreeChart
 * @param <K>
 * @param <V>
 * @param <T>
 */
public class TimeStampSet<K, T, D>
	implements Set<K>
// extends HashMap<K, V>
{
	private static final long	serialVersionUID	= -4277098373746171836L;

	private Map<K, T>			keyToTime			= new HashMap<K, T>();
	private TreeMap<T, Set<K>>	sortedTimes			= new TreeMap<T, Set<K>>();

	private T					currentTime;									// This
	// must
	// always
	// be
	// the
	// same
	// as
	// sortedTimes.lastEntry

	private IDistanceFunc<T, D>	funcDistance;
	private D					maxDistance;
	private Comparator<D>		compDistance;
	private boolean				inclusive;

	// Controls wheter the timestamp of a key which already exists should
	// be updated (true) or not (false).
	private boolean				allowRenewal;

	public T getKeyTime(K key)
	{
		return keyToTime.get(key);
	}

	/**
	 * inclusive: wheter max distance is inclusive or exclusive. e.g. if
	 * maxDistance is 10, and a distance is also 10, then inclusive: the item is
	 * retained (10 is included) exclusive: the item is removed
	 * 
	 * @param funcDistance
	 * @param maxDistance
	 * @param compDistance
	 * @param inclusive
	 */
	public TimeStampSet(IDistanceFunc<T, D> funcDistance, D maxDistance,
			Comparator<D> compDistance, boolean inclusive, boolean allowRenewal)
	{
		this.funcDistance = funcDistance;
		this.maxDistance = maxDistance;
		this.compDistance = compDistance;
		this.inclusive = inclusive;
		this.allowRenewal = allowRenewal;
	}

	public TimeStampSet(D maxDistance)
	{
		setMaxDistance(maxDistance);
	}

	/**
	 * Checks wheter a given time is outdated.
	 * 
	 * Example: If current time is 10, maxDistance is 5, then everthing smaller
	 * than 5 is outdated.
	 * 
	 * @return
	 */
	private boolean isOutdated(T t)
	{
		// Distance between e.g. 0 and 10
		D distance = funcDistance.distance(t, currentTime);

		// e.g. compare 10 to 5
		int delta = compDistance.compare(distance, maxDistance);

		if (inclusive)
			return delta > 0;
		else
			return delta >= 0;
	}

	private Set<K> removeOutdated()
	{
		Set<K> result = new HashSet<K>();
		while (!sortedTimes.isEmpty() && isOutdated(sortedTimes.firstKey()))
			result.addAll(removeOldest());

		return result;
	}

	/**
	 * Sets a new maximum distance.
	 * 
	 * Removes all entries whose distance is greater than the new one.
	 * 
	 * @param newMaxDistance
	 */
	public void setMaxDistance(D newMaxDistance)
	{
		maxDistance = newMaxDistance;

		removeOutdated();
	}

	private Set<K> removeOldest()
	{
		// we need to remove the oldest item
		Map.Entry<T, Set<K>> removeItem = sortedTimes.pollFirstEntry();

		//System.out.println("    Removing time: " + removeItem.getKey());

		// remove by time (cheaper as a possible equals on the key)
		for (K removeKey : removeItem.getValue()) {
			keyToTime.remove(removeKey);
			// this.remove(removeKey);
		}

		return removeItem.getValue();
	}

	private T initKeyTime(K key)
	{
		keyToTime.put(key, currentTime);

		Set<K> keys = sortedTimes.get(currentTime);
		if (keys == null) {
			keys = new HashSet<K>();
			sortedTimes.put(currentTime, keys);
		}

		keys.add(key);

		return currentTime;
	}

	private void updateKeyTime(K key, T oldTime)
	{
		Set<K> keys = sortedTimes.get(oldTime);
		if (!keys.contains(key))
			throw new RuntimeException("Shouldn't happen");

		// If the keyset for a timestamp only contains the key we are updating
		// remove the whole timestamp, otherwise just remove the key
		if (sortedTimes.size() == 1)
			sortedTimes.remove(oldTime);
		else
			keys.remove(key);

		initKeyTime(key);
	}

	/**
	 * Sets the current time. Could returns the elements which become outdated.
	 * 
	 * @param time
	 * @return
	 */
	public Set<K> setCurrentTime(T time)
	{
		// TODO The new time must be greater-quals the current time
		this.currentTime = time;

		return removeOutdated();
	}

	/**
	 * Put operations are done for the current time
	 * 
	 */
	@Override
	public boolean add(K key)
	{
		T keyTime = keyToTime.get(key);

		if (keyTime == null)
			initKeyTime(key);
		else if (allowRenewal)
			updateKeyTime(key, keyTime);

		return keyTime == null;
	}

	@Override
	public boolean addAll(Collection<? extends K> arg0)
	{
		boolean result = false;
		for (K item : arg0)
			result |= this.add(item);

		// TODO Auto-generated method stub
		return result;
	}

	@Override
	public void clear()
	{
		keyToTime.clear();
		sortedTimes.clear();
	}

	@Override
	public boolean contains(Object arg0)
	{
		return keyToTime.containsKey(arg0);
	}

	@Override
	public boolean containsAll(Collection<?> arg0)
	{
		boolean result = true;
		for (Object item : arg0)
			result &= this.contains(item);

		return result;
	}

	@Override
	public boolean isEmpty()
	{
		return keyToTime.isEmpty();
	}

	/**
	 * FIXME Make sure that the iterator's remove method cannot be invoked
	 * directly
	 * 
	 */
	@Override
	public Iterator<K> iterator()
	{
		return keyToTime.keySet().iterator();
	}

	@Override
	public boolean remove(Object arg0)
	{
		T time = keyToTime.get(arg0);
		if (time == null)
			return false;

		Set<K> keys = sortedTimes.get(time);
		if (keys == null)
			return false;

		keyToTime.remove(arg0);

		if (keys.remove(arg0)) {
			if (keys.isEmpty())
				sortedTimes.remove(keys);

			return true;
		}

		return false;
	}

	@Override
	public boolean removeAll(Collection<?> arg0)
	{
		throw new RuntimeException("Not implemented yet.");
	}

	@Override
	public boolean retainAll(Collection<?> arg0)
	{
		throw new RuntimeException("Not implemented yet.");
	}

	@Override
	public int size()
	{
		return keyToTime.size();
	}

	@Override
	public Object[] toArray()
	{
		return keyToTime.keySet().toArray();
	}

	@Override
	public <X> X[] toArray(X[] arg0)
	{
		return keyToTime.keySet().toArray(arg0);
	}

	@Override
	public int hashCode()
	{
		return keyToTime.keySet().hashCode();
	}

	@Override
	public boolean equals(Object o)
	{
		return keyToTime.keySet().equals(o);
	}

	@Override
	public String toString()
	{
		return keyToTime.keySet().toString();
	}

	/**
	 * Convenience function for Date
	 * 
	 * @param maxDistance
	 *            time window in ms
	 * @param allowRenewal
	 * @return
	 */
	public static <K> TimeStampSet<K, Date, Long> createSet(Class<K> clazz,
			long maxDistance, boolean inclusive, boolean allowRenewal)
	{
		return new TimeStampSet<K, Date, Long>(new IDistanceFunc<Date, Long>() {
			@Override
			public Long distance(Date a, Date b)
			{
				return b.getTime() - a.getTime();
			}
		}, maxDistance, new Comparator<Long>() {
			@Override
			public int compare(Long a, Long b)
			{
				return a.compareTo(b);
			}
		}, inclusive, allowRenewal);
	}

	/*
	 * @SuppressWarnings("unchecked") public V get(Object key) { return _get((K)
	 * key); }
	 * 
	 * private V _get(K key) { /* Integer keyTime = keyToTime.get(key);
	 * 
	 * if (keyTime == null) { ++missCount; return null; }
	 * 
	 * ++hitCount; updateTime(key, keyTime); / return super.get(key); }
	 */

	/*
	 * public int getHitCount() { return hitCount; }
	 * 
	 * public int getMissCount() { return missCount; }
	 * 
	 * public void resetStats() { hitCount = 0; missCount = 0; }
	 */
}

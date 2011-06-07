package org.dbpedia.extraction.live.util.collections;

import java.util.Map;

/**
 *
 * @author raven_arkadon
 */
public interface IOneToOneMap<TKey, TValue>
	extends Map<TKey, TValue>
{
    //public boolean put(TKey key, TValue value);
    //TValue getValue(Object key);
    TKey   getKey(Object value);
    
    //boolean containsKey(Object key);
    //boolean containsValue(Object value);
    boolean contains(Object key, Object value);
    //boolean remove(Object key, Object value);
    //TValue  removeKey(Object key);
    TKey    removeValue(Object key);
    //void    clear();
    
    //int size();
    
    //Set<Map.Entry<TKey, TValue>> entrySet();
}

package org.dbpedia.extraction.live.util.collections;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author raven_arkadon
 */
public class OneToOneMap<TKey, TValue>
	extends HashMap<TKey, TValue>
	implements IOneToOneMap<TKey, TValue>
{
    private Map<TValue, TKey> value_2_key = new HashMap<TValue, TKey>();

    public TValue put(TKey key, TValue value)
    {
    	if(value_2_key.containsKey(value))
    		throw new RuntimeException("Multiple keys for the same value");

        super.put(key, value);
        value_2_key.put(value, key);

        return value;
    }

    public TKey getKey(Object value)
    {
        return value_2_key.get(value);
    }
    /*
    public TValue getValue(Object key)
    {
        return this.get(key);
    }
    */
    
    /*

    public boolean containsValue(Object value)
    {
        return value_2_key.containsKey(value);
    }
     */

    public boolean contains(Object key, Object value)
    {
        return value == this.get(key);
    }

    /*
    public TValue removeKey(Object key)
    {
        TValue v = this.remove(key);
        value_2_key.remove(v);
        
        return v;
    }
     */
    public TKey removeValue(Object key)
    {
        TKey k = value_2_key.remove(key);
        this.remove(k);
 
        return k;
    }

    public void clear()
    {
    	super.clear();
        value_2_key.clear();
    }

    /*
    public Set<Entry<TKey, TValue>> entrySet()
    {
        return this.entrySet();
    }
    */
}

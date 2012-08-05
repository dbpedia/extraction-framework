package org.dbpedia.extraction.live.publisher;

/**
 * Created by IntelliJ IDEA.
 * Date: Oct 30, 2010
 * Time: 5:51:33 PM
 */

import org.apache.commons.collections15.MultiMap;
import org.apache.commons.collections15.multimap.MultiHashMap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;

public class URIUtil
{
	public static void download(URL url, File file)
		throws IOException
	{
		InputStream in = url.openStream();
		FileOutputStream out = new FileOutputStream(file);

		StreamUtil.copyThenClose(in, out);
	}

    
	/**
	 * Just a wrapper for URLEncoder.encode so we don't have to care about
	 * exception handling.
	 *
	 * @param uri
	 * @return
	 */
	public static String encodeUTF8(String uri)
	{
		try {
			return URLEncoder.encode(uri, "UTF-8");
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String decodeUTF8(String uri)
	{
		try {
			return URLDecoder.decode(uri, "UTF-8");
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}


	public static String myEncode(String str)
	{
		String result = str.replaceAll("\\s+", "_");

		try {
			result = URLEncoder.encode(result, "UTF-8");
		} catch(Exception e) {
			throw new RuntimeException(e);
		}

		return result;
	}

	public static MultiMap<String, String> getQueryMap(String query)
	{
	    MultiMap<String, String> result = new MultiHashMap<String, String>();
	    if(query == null)
	    	return result;

	    String[] params = query.split("&");
	    for (String param : params)
	    {
	    	String[] kv = param.split("=", 2);
	        String key = kv[0];
	        String value = kv.length == 2 ? kv[1] : null;

	        result.put(key, value);
	    }
	    return result;
	}

	public static <K, V> Collection<V> safeGet(MultiMap<K, V> map, K key)
	{
		Collection<V> value = map.get(key);
		if(value == null)
			return Collections.emptyList();

		return value;
	}
}

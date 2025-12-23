package org.dbpedia.extraction.live.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;



public class StringUtil
{
	/*
	public static void main(String[] args)
	{
		System.out.println(matchAll("#REDIRECT[[BLAAAH]]", MediawikiHelper.getRedirectPattern()));
	}
	*/
	
	
	public static List<String> matchAll(String text, Pattern pattern)
	{
		Matcher matcher = pattern.matcher(text);
		List<String> result = new ArrayList<String>();

		if(matcher.matches())
		{
			for(int i = 1; i <= matcher.groupCount(); ++i)
			{
				result.add(matcher.group(i));
			}
		}
		
		return result;
	}
	
	/*
	 * returns an empty string if the argument is null
	 */
	public static String noNull(String str)
	{
		return str == null ? "" : str;
	}
	
	public static byte[] zip(String str)
	{
		byte[] result = null;
		try {
			result = _zip(str);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}
	
	private static byte[] _zip(String str)
		throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		GZIPOutputStream zipStream = new GZIPOutputStream(baos);
		OutputStreamWriter writer = new OutputStreamWriter(zipStream);
		writer.write(str);
		
		writer.close();
		
		return baos.toByteArray();		 
	}
	

	public static String unzip(byte[] bytes)
	{
		String result = null;
		try {
			result = _unzip(bytes);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		return result;
	}
	
	public static String _unzip(byte[] bytes)
		throws IOException
	{
		ByteArrayInputStream in = new ByteArrayInputStream(bytes);
		GZIPInputStream zipStream = new GZIPInputStream(in);
		InputStreamReader reader = new InputStreamReader(zipStream);

		BufferedReader br = new BufferedReader(reader);

		String result = "";
		String part = "";
		while((part = br.readLine()) != null)
			result += part;

		//System.out.println("Data = " + result);
		
		return result;
	}
	
	public static String toString(InputStream in)
		throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
	
		String result = "";
		String line;
		while(null != (line = reader.readLine()))
			result += line + "\n";
	
		return result;
	}

	
	public static String toString(Object o)
	{
		return o == null
			? "(null)"
			: o.toString();
	}
	
	public static String implode(String separator, Object ... items)
	{
		return implode(separator, Arrays.asList(items));
	}

	/**
	 * Returns null if the iterator is null.
	 * If an empty string is desired, use StringUtil.trim() 
	 * 
	 * @param separator
	 * @param it
	 * @return
	 */
	public static String implode(String separator, Iterator<? extends Object> it)
	{
		String result = "";
		
		if(it == null)
			return null;
		
		while(it.hasNext()) {
			result += it.next();
			
			if(it.hasNext())
				result += separator;
		}
		
		return result;		
	}
	
	public static String implode(String separator, Iterable<? extends Object> items)
	{
		return implode(separator, items.iterator());
/*
		String result = "";
		
		if(items == null)
			return result;
		
		Iterator<? extends Object> it = items.iterator();
		while(it.hasNext()) {
			result += it.next();
			
			if(it.hasNext())
				result += separator;
		}
		
		return result;
*/
	}

	/**
	 * Cuts a string after nMax bytes - unless the remaining bytes are less
	 * than tolerance.
	 * In case of a cut appends "... (# more bytes)".
	 * (# cannot be less than tolerance)
	 * 
	 * @param str
	 * @param nMax
	 * @param nTolerance
	 * @return
	 */
	public static String cropString(String str, int nMax, int nTolerance)
	{
		String result = str;
		int nGiven = str.length();

		if(nGiven > nMax) {
			int tooMany = nGiven - nMax;
			
			if(tooMany > nTolerance)			
				result = str.substring(0, nMax) +
					"... (" + tooMany + " more bytes)";
		}
		return result;
	}

	public static String ucFirst(String str)
	{
		return str.length() == 0
			? ""
			: str.substring(0,1).toUpperCase() + str.substring(1);
	}

	public static String lcFirst(String str)
	{
		return str.length() == 0
			? ""
			: str.substring(0,1).toLowerCase() + str.substring(1); 
	}

	/**
	 * Trims a string, or returns empty string in case of null.
	 * 
	 */
	public static String trim(String str)
	{
		return str == null
			? ""
			: str.trim();
	}

	// FIXME: Rename this method using refactoring.
	// Semantic of this method is different from what will be expected
	// (e.g. noone would expect a trim here)
	public static String ucFirstOnly(String str)
	{
		String tmp = noNull(str).trim().toLowerCase();
	
		return ucFirst(tmp);
	}
}

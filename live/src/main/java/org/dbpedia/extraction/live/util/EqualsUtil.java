package org.dbpedia.extraction.live.util;

public class EqualsUtil
{
	public static int hashCode(Object o)
	{
		return o == null
			? 1
			: o.hashCode();
	}

	public static boolean equals(Object a, Object b)
	{
		return a != null
			? a.equals(b)
			: a == b;
	}

	
	public static int compareTo(String a, String b)
	{
		if(a == null)
			return b == null ? 0 : -1;
		else
			return b == null ? 1 : a.compareTo(b);
	}
}

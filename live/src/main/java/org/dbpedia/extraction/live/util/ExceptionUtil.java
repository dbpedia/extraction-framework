package org.dbpedia.extraction.live.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionUtil
{
	public static String toString(Throwable e)
	{
		if(e == null)
			return "";

	    StringWriter sw = new StringWriter();
	    PrintWriter pw = new PrintWriter(sw);
	    e.printStackTrace(pw);
	    return sw.toString();
	}
}

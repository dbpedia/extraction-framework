package org.dbpedia.extraction.live.publisher;

/**
 * Created by IntelliJ IDEA.
 * Date: Oct 30, 2010
 * Time: 5:52:44 PM
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class StreamUtil
{
	public static void copy(InputStream in, OutputStream out)
		throws IOException
	{
		int n;
		byte[] buffer = new byte[16 * 1024];
		while((n = in.read(buffer)) != -1) {
			out.write(buffer, 0, n);
		}
	}

	public static void copyThenClose(InputStream in, OutputStream out)
		throws IOException
	{
		try {
			copy(in, out);
			out.flush();
		}
		finally {
			in.close();
			out.close();
		}
	}


	public static String toString(InputStream in)
		throws IOException
	{
		return toString(in, true);
	}

	public static String toString(InputStream in, boolean bClose)
		throws IOException
	{
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));

		String result = "";
		try {
			String line;
			while(null != (line = reader.readLine()))
				result += line + "\n";
		}
		finally {
			if(bClose) {
				reader.close();
				in.close();
			}
		}

		return result;
	}
}

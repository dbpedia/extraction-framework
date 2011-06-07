package org.dbpedia.extraction.live.util.iterators;

import java.io.BufferedReader;
import java.util.Collections;
import java.util.Iterator;

/**
 * This iterator can be given a buffered reader to a dump of wikipedia pages.
 * The iterator will simply return a string for each pair of
 * <page> and </page> tags containing these tags and the content.
 * 
 * @author raven
 *
 */
public class WikiDumpPageIterator
	extends PrefetchIterator<String>
{
	private BufferedReader reader;
	
	public WikiDumpPageIterator(BufferedReader reader)
	{
		this.reader = reader;
	}
	
	@Override
	protected Iterator<String> prefetch()
		throws Exception
	{
		String result = "";
		String line;
		while((line = reader.readLine()) != null)
		{
			if(line.trim().startsWith("<page>")) {				
				result += line + "\n";
				
				while((line = reader.readLine()) != null) {
					if(line.trim().startsWith("</page>")) {
						result += line + "\n";
	
						return Collections.singleton(result).iterator();
					}
					
					result += line + "\n";
				}
			}
		}
		
		return null;
	}
}

package org.dbpedia.extraction.live.record;


import java.lang.reflect.Field;
import java.net.URI;

import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.dbpedia.extraction.live.util.StringUtil;





public class RecordMetadata
{
	private final String language;
	private final MediawikiTitle title;
	private final String oaiId;
	private final URI wikipediaIRI;
	private final String revision;
	
	private final String username;
	private final String ip;
	private final String userId;
	
	public RecordMetadata(
			String language,
			MediawikiTitle title, // title without namespace
			String oaiId,
			URI wikipediaIRI,
			String revision,
			String username,
			String ip,
			String userId)
	{
		this.language = language;
		this.oaiId = oaiId;
		this.wikipediaIRI = wikipediaIRI;
		this.title = title;
		this.revision = revision;
		this.username = username;
		this.ip = ip;
		this.userId = userId;
	}
	
	public String getLanguage() {
		return language;
	}
	
	public MediawikiTitle getTitle() {
		return title;
	}
	
	public String getOaiId() {
		return oaiId;
	}
	
	public URI getWikipediaURI() {
		return wikipediaIRI;
	}

	public String getRevision()
	{
		return revision;
	}

	public String getUsername()
	{
		return username;
	}
	
	public String getIp()
	{
		return ip;
	}
	
	public String getUserId()
	{
		return userId;
	}
	/*
	 * Convenience method which returns the part of the tile
	 * before the first colon in lower case letters
	 * or 'article' if there is no such type.
	 * appened with the part after the first following the colon
	 * So TeMplate:Infobox_Person/Doc becomes template/Doc
	 * The namespace is case insensitive
	 * 
	 */
	/*
	public String getNamespace()
	{			
		String result = "article";
		int colonIndex = title.indexOf(":");
		if(colonIndex != -1)
			result = title.substring(0, colonIndex).toLowerCase(); // case insensitive
		
		int slashIndex = title.indexOf("/", colonIndex); // case sensitive
		if(slashIndex != -1) {
			String part = title.substring(slashIndex + 1);

			if(part.equals("doc"))
				result += "/doc";
		}
		
		return result;
	}
	*/
	
	@Override
	public String toString()
	{
		String result = "";
		Field[] fields = this.getClass().getDeclaredFields();
		
		for(Field field : fields) {
			String value = "error";
			try {
				value = StringUtil.toString(field.get(this));
			}
			catch(Exception e) {
				System.err.println(ExceptionUtil.toString(e));
			}
			
			
			result += field.getName() + ": " + value + "\n";
		}
		
		return result;
	}

	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof RecordMetadata))
			return false;
		
		RecordMetadata other = (RecordMetadata)o;
		return this.getOaiId().equals(other.getOaiId());
	}
	
	@Override
	public int hashCode()
	{
		return 29 * this.getOaiId().hashCode();
	}	
}

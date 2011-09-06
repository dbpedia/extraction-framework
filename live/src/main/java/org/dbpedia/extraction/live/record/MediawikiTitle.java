package org.dbpedia.extraction.live.record;

/*
class MediawikiNamespace
{
	private String name;
	private int id;
}
*/

public class MediawikiTitle
{
	private String fullTitle;
	private String shortTitle;
	private Integer namespaceId;
	private String namespaceName;
	
	/*
	public MediawikiTitle(String title)
	{
		this.originalTitle = title;
	}
	*/

	public MediawikiTitle(
			String fullTitle,
			String shortTitle,
			Integer namespaceId,
			String namespaceName
			)
	{
		this.fullTitle = fullTitle;
		this.shortTitle = shortTitle;
		this.namespaceId = namespaceId;
		this.namespaceName = namespaceName;
	}
	
	public String getFullTitle()
	{
		return fullTitle; 
	}

	public String getShortTitle()
	{
		return shortTitle;
	}

	public Integer getNamespaceId()
	{
		return namespaceId;
	}
	
	public String getNamespaceName()
	{
		return namespaceName;
	}
}

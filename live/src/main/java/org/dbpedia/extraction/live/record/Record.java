package org.dbpedia.extraction.live.record;

/**
 * A records consists of metadata and content.
 * 
 * @author raven_arkadon
 *
 */
public class Record
	implements IRecord
{
	private RecordMetadata metadata;
	private RecordContent  content;
	
	public Record(RecordMetadata metadata)
	{
		this.metadata = metadata;
	}
	
	public Record(RecordMetadata metadata, RecordContent content)
	{
		this.metadata = metadata;
		this.content = content;
	}
	
	public RecordMetadata getMetadata()
	{
		return metadata;
	}
	
	public RecordContent getContent()
	{
		return content;
	}
	
	public void setContent(RecordContent content)
	{
		this.content = content;
	}

	@Override
	public boolean equals(Object o)
	{
		if(!(o instanceof Record))
			return false;
		
		Record other = (Record)o;
		return this.getMetadata().equals(other.getMetadata());
	}
	
	@Override
	public int hashCode()
	{
		return 19 * this.getMetadata().hashCode();
	}
	
	public <T> T accept(IRecordVisitor<T> visitor)
	{
		return visitor.visit(this);
	}

	public String toString()
	{
		return
			"Metadata = " + metadata.toString() +
			"content = " + content.getText();
		/*
		StringBuffer sb = new StringBuffer();
		// length 29 "http://en.wikipedia.org/wiki/".length()
		String pageID = wikipediaIRI.substring(OAIReaderMain.baseWikiIRI.length()); //wikipediaIRI.substring(29);

		sb.append( pageID+"\n");
		/*sb.append("title "+title+"\n");
		sb.append("language "+language+"\n");
		sb.append("oaiIdentifier "+oaiIdentifier+"\n");
		sb.append("revision "+revision+"\n"+"\n");
		* /
		sb.append(this.wikiSource);
		//sb.append(xml);
		return sb.toString();
		*/
	}
}

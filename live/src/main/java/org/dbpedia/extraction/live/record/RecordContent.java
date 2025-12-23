package org.dbpedia.extraction.live.record;




public class RecordContent
{
	private String text = "";
	private String revision = "";
	private String xml = "";

	// A container for objects which should be representations of this content
	// (e.g. an xml representation, or some processed representation)
	private ObjectContainer<Object> representations = new ObjectContainer<Object>();
	
	// The same content can have multiple presentations
	// Actually, this should be added to record itself..
	// (This is similar to the linked data principle, where a resource has
	// a unique id, but there may be multiple representations of tha same resource)
	
	public RecordContent()
	{
	}
	
	public ObjectContainer<Object> getRepresentations()
	{
		return representations;
	}
 
	
	public RecordContent(String text, String revision, String xml)
	{
		this.text = text;
		this.revision = revision;
		this.xml = xml;
	}
	
	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getRevision() {
		return revision;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}
		
	public void setXml(String xml) {
		this.xml = xml;
	}

	public String getXml() {
		return xml;
	}
}

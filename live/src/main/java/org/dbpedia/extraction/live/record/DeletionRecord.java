package org.dbpedia.extraction.live.record;



public class DeletionRecord
	implements IRecord
{
	private String oaiId;
	private String dateStamp;
	
	public DeletionRecord(String oaiId, String dateStamp)
	{
		this.oaiId = oaiId;
		this.dateStamp = dateStamp;
	}
	
	public String getOaiId()
	{
		return oaiId;
	}
	
	public String getDateStamp()
	{
		return dateStamp;
	}
	
	public <T> T accept(IRecordVisitor<T> visitor)
	{
		return visitor.visit(this);
	}	
}

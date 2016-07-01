package org.dbpedia.extraction.live.record;

public interface IRecordVisitor<T>
{
	T visit(Record record);
	T visit(DeletionRecord record);
}

package org.dbpedia.extraction.live.record;

public interface IRecord
{
	<T> T accept(IRecordVisitor<T> visitor);
}

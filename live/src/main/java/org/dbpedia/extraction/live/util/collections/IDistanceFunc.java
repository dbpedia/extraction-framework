package org.dbpedia.extraction.live.util.collections;

public interface IDistanceFunc<T, D>
{
	D distance(T a, T b);
}

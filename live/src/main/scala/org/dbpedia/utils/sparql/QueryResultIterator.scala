package org.dbpedia.test

import org.dbpedia.utils.sparql.IGraphDAO;



import com.hp.hpl.jena.query.QuerySolution;







class QueryResultIterator(val graphDAO : IGraphDAO, val query : String, val limit : Int, var offset : Int)
	extends PrefetchIterator[QuerySolution]
{
	private var isEndReached : Boolean = false;

	def buildQuery() : String = {
		var result = query;

		if(limit != 0)
			result += " LIMIT " + limit;
			
		if(offset != 0)
			result += " OFFSET " + offset;
		
		return result;
	}

	
	override def prefetch() : Iterator[QuerySolution] = {
		
		if(isEndReached)
			return null;
		
		val q = buildQuery();

		val list = graphDAO.executeSelect(q)
    Thread.sleep(1000);

		if(offset == 0)
			offset = limit;
		else
			offset += limit;

		if(list.length < limit)
			isEndReached = true;
		
		return list.iterator;
	}
}

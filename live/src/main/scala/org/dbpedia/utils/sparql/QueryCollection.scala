package org.dbpedia.utils.sparql

import com.hp.hpl.jena.query.QuerySolution
import org.dbpedia.test.QueryResultIterator


/**
 * A collection view on a result set for a Sparql-Query.
 * This collection uses limit and offset to retrieve the Sparql result
 * chunk-wise
 *
 * 
 * @author Claus Stadler
 *
 */
class QueryCollection(val graphDAO : IGraphDAO, val query : String)
	extends Iterable[QuerySolution]
{
	override def iterator() : Iterator[QuerySolution] = {
		return new QueryResultIterator(graphDAO, query, 500, 0);
	}
}


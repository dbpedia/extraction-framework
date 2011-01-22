package org.dbpedia.extraction.live.util.sparql;

import com.hp.hpl.jena.rdf.model.Model;

public interface ISparulExecutor
	extends ISparqlExecutor
{
	void executeUpdate(String query)
		throws Exception;
	
	boolean insert(Model model, String graphName)
		throws Exception;

	boolean remove(Model model, String graphName)
		throws Exception;
}

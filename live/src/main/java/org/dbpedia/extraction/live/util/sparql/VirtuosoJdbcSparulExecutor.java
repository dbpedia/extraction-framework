package org.dbpedia.extraction.live.util.sparql;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

//import org.dbpedia.extraction.live.util.ModelUtil;
import com.hp.hpl.jena.n3.turtle.TurtleReader;
import com.hp.hpl.jena.n3.turtle.parser.TurtleParser;
import org.dbpedia.extraction.live.util.ModelUtil;
import org.dbpedia.extraction.live.util.SQLUtil;
import org.dbpedia.extraction.live.util.iterators.SinglePrefetchIterator;
import org.log4j.Logger;

import virtuoso.jdbc4.VirtuosoExtendedString;
import virtuoso.jdbc4.VirtuosoRdfBox;
import virtuoso.jdbc4.VirtuosoResultSet;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.QuerySolutionMap;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.StmtIterator;

/**
 * 
 * Implemented using:
 * http://docs.openlinksw.com/virtuoso/VirtuosoDriverJDBC.html
 * @author raven
 *
 */
public class VirtuosoJdbcSparulExecutor
	implements ISparulExecutor
{
	private static final Logger logger = Logger.getLogger(VirtuosoJdbcSparulExecutor.class);
	
	private Connection con;
	private String defaultGraphName;
	
	/**
	 * Flags for the virtuoso TTLP function (does insert of triples)
	 * according to http://docs.openlinksw.com/virtuoso/fn_ttlp.html
	 */
	private int ttlpFlags = 255;
	
	// Maximum number of statements to insert/delete
	private int maxChunkSize = 1024;
	
	public VirtuosoJdbcSparulExecutor(String defaultGraphName)
	{
		this.defaultGraphName = defaultGraphName;
	}
	
	public VirtuosoJdbcSparulExecutor(Connection con, String defaultGraphName)
	{
		this.con = con;
		this.defaultGraphName = defaultGraphName;
	}

	
	public void setConnection(Connection connection)
	{
		this.con = connection;
	}
	
	public Connection getConnection()
	{
		return con;
	}
	
	public void setTTLPFlags(int ttlpFlags)
	{
		this.ttlpFlags = ttlpFlags;
	}
	
	public int getTTLPFlags()
	{
		return ttlpFlags;
	}
	
	
	@Override
	public void executeUpdate(String query)
		throws Exception
	{				
		query = processRawQuery(query);
		Statement stmt = con.createStatement();
		stmt.executeUpdate(query);
		//int result = stmt.executeUpdate(query);
		//System.out.println("Result was: " + result);
	}
	
	private static String escapeQuery(String query)
	{
		query = query.replace("\\", "\\\\");
		query = query.replace("'", "\\'");

		return query;
	}

	private String processRawQuery(String query)
	{
		// The order of the following two statements is important :)
		query = escapeQuery(query);

		if(getGraphName() != null)
			query = 
				"define input:default-graph-uri <" + getGraphName() + "> \n" +
				query;
		
		query = "CALL DB.DBA.SPARQL_EVAL('" + query + "', NULL, 0)";

		return query;
	}

	@Override
	public boolean executeAsk(String query)
		throws Exception
	{
		query = processRawQuery(query);
		logger.trace("Sending query: " + query);
	
		Statement stmt = con.createStatement();
		java.sql.ResultSet rs = stmt.executeQuery(query);
		
		boolean result = false;
		
		while(rs.next())
			result = rs.getBoolean(1);
		
		return result;
	}


	@Override
	public List<QuerySolution> executeSelect(String query)
		throws Exception
	{
		query = processRawQuery(query);
		logger.trace("Sending query: " + query);

		List<QuerySolution> result = new ArrayList<QuerySolution>();
	
		Statement stmt = con.createStatement();
		VirtuosoResultSet rs = (VirtuosoResultSet)stmt.executeQuery(query);
		
		ResultSetMetaData meta = rs.getMetaData();
		
		Model model = ModelFactory.createDefaultModel();
		
		while(rs.next()) {
			QuerySolutionMap qs = new QuerySolutionMap();

			for(int i = 1; i <= meta.getColumnCount(); ++i) {
				String columnName = meta.getColumnName(i);
				
				Object o = rs.getObject(i);
				// String representing an IRI
				if(o instanceof VirtuosoExtendedString)
				{
					VirtuosoExtendedString vs = (VirtuosoExtendedString) o;
					if (vs.iriType == VirtuosoExtendedString.IRI)
						qs.add(columnName, model.createResource(vs.str));
					else if (vs.iriType == VirtuosoExtendedString.BNODE)
						qs.add(columnName, model.createResource(new AnonId(vs.str)));
				}
				else if(o instanceof VirtuosoRdfBox) // Typed literal
				{
					VirtuosoRdfBox rb = (VirtuosoRdfBox) o;
					if(rb.getType() == null || rb.getType().isEmpty())
						qs.add(columnName, model.createLiteral(rb.rb_box.toString(), rb.getLang()));
					else
						qs.add(columnName, model.createTypedLiteral(rb.rb_box));
				}
				else if(o == null) {
					qs.add(columnName, null);					
				}
				else { // Untyped literal
					//System.out.println("Got type: " + o.getClass());
					qs.add(columnName, model.createLiteral(o.toString()));
				}
			}

			result.add(qs);
		}

		/*
		System.out.println(result);
		for(QuerySolution qs : result) {
			Iterator<String> names = qs.varNames();
			while(names.hasNext()) {
				String name = names.next();
				Object value = qs.get(name);
				System.out.print(name + ": " + value + "(" + value.getClass().getSimpleName() + ")");
			}
			System.out.println("   ");
			System.out.println();
		}
		*/
		
		return result;
	}

	@Override
	public String getGraphName()
	{
		return defaultGraphName;
	}

	
	@Override
	public boolean insert(Model model, String graphName)
		throws SQLException
	{
		Iterable<Model> chunks = new ModelChunkIterable(model, maxChunkSize);
		for(Model item : chunks) {
			insertBatch(item, graphName);
		}
		
		return true;
	}

	public boolean insertBatch(Model model, String graphName)
		throws SQLException
	{
		if(model.isEmpty())
			return true;
		
		String ntriples = ModelUtil.toString(model, "N-TRIPLE");
		ntriples = ntriples.replace("\\", "\\\\");
		ntriples = ntriples.replace("'", "\\'");

		String query =
		//	"ttlp('" + ntriples + "', '', '" + graphName + "')";
		"DB.DBA.TTLP_MT ('" + ntriples + "', '" + graphName + "', '" + graphName + "', " + ttlpFlags + ")";

		logger.trace("Sending query: " + query);
		Statement stmt = con.createStatement();
		boolean result = stmt.execute(query);
		stmt.close();
		return result;
	}

	
	public boolean removeBatch(Model model, String graphName)
		throws Exception
	{
		if(model.isEmpty())
			return false;

		if(graphName == null)
			graphName = defaultGraphName;

		String ntriples = ModelUtil.toString(model, "N-TRIPLE");
		
		String query =
			"Delete ";
		
		if(graphName != null)
			query += "From <" + graphName + "> ";
		
		query += "{" + ntriples + "}";
		
		logger.trace("Sending query: " + query);
		
		executeUpdate(query);
		
		return true;		
	}
	
	
	@Override
	public boolean remove(Model model, String graphName)
		throws Exception
	{
		Iterable<Model> chunks = new ModelChunkIterable(model, maxChunkSize);
		for(Model item : chunks) {
			removeBatch(item, graphName);
		}
		
		return true;
	}

	/**
	 * FIXME This method does not make use of the defaultGraph
	 */
	@Override
	public Model executeConstruct(String query)
		throws Exception
	{
		query = escapeQuery(query);
		query = "sparql " + query;
		
		logger.trace("Sending query: " + query);
		String str = SQLUtil.execute(con, query, String.class);
        //System.out.println(str);

		InputStream in = new ByteArrayInputStream(str.getBytes());
		
		Model result = ModelFactory.createDefaultModel();
 		result.read(in, null, "N3");
		
		return result;
	}
}


class ModelChunkIterator
	extends SinglePrefetchIterator<Model>
{
	private Model model;
	private int maxChunkSize;

	private StmtIterator it;
	
	public ModelChunkIterator(Model model, int maxChunkSize)
	{
		this.model = model;
		this.maxChunkSize = maxChunkSize;
	}
	
	@Override
	protected Model prefetch()
		throws Exception
	{		
		Model tmp = ModelFactory.createDefaultModel();
		
		if(it == null)
			it = model.listStatements();

		while(it.hasNext()) {
			tmp.add(it.next());
			
			if(tmp.size() > maxChunkSize)
				return tmp;
		}

		if(!tmp.isEmpty())
			return tmp;
		else {
			it.close();
			return finish();
		}
	}
}

class ModelChunkIterable
	implements Iterable<Model>
{
	private Model model;
	private int maxChunkSize;

	public ModelChunkIterable(Model model, int maxChunkSize)
	{
		this.model = model;
		this.maxChunkSize = maxChunkSize;
	}


	@Override
	public Iterator<Model> iterator()
	{
		if(model.size() <= maxChunkSize)
			return Collections.singleton(model).iterator();
		else
			return new ModelChunkIterator(model, maxChunkSize);
	}
}

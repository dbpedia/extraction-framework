package org.dbpedia.extraction.live.core;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.*;
import java.net.URLDecoder;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 1, 2010
 * Time: 5:37:41 PM
 * This class represents the results of the extraction process.
 */

public class ExtractionResult{
    
    private long pageID;
    private String language;
    private String extractorID;
    private ArrayList triples = new ArrayList();
    private ArrayList metadataTriples = new ArrayList();
    private HashMap predicates = new HashMap();

/* @param pageID The page from which this result originated, or null if
            from no specific page */
    public  ExtractionResult(long pageid, String lang, String extractorid)
    {
        this.pageID = pageid;
        this.language = lang;
        this.extractorID = extractorid;
    }

    /* @result The page from which this result originated, or null if
            from no specific page */
    public long getPageID()
    {
        return this.pageID;
    }
    public String getLanguage()
    {
        return this.language;
    }
    public String getExtractorID()
    {
        return this.extractorID;
    }
    public ArrayList getTriples()
    {
        return this.triples;
    }
    public ArrayList getMetadataTriples()
    {
        return this.metadataTriples;
    }

     public ExtractionResult getPredicateTriples()
     {

         ExtractionResult predicateTriples = new ExtractionResult(this.pageID, this.language, this.extractorID);

         Iterator predicatesIterator = this.predicates.entrySet().iterator();
         while(predicatesIterator.hasNext())
         //foreach ( this.predicates as subject => bool )
         {
             String subject = ((Map.Entry)predicatesIterator.next()).getKey().toString();
             
             predicateTriples.addTriple(new URIImpl(subject), new URIImpl(Constants.RDF_TYPE), new URIImpl(Constants.RDF_PROPERTY));
			 predicateTriples.addTriple(new URIImpl(subject), new URIImpl(Constants.RDFS_LABEL),
                            new LiteralImpl(this.getPredicateLabel(subject)));
         }
        return predicateTriples;
    }

    public void addTripleObject(Statement triple)
    {
		this._addToTripleArray(triple);
    }

	public void addTriple(Resource s, URI p, Value o)
    {
		this._addToTripleArray(new RDFTriple(s, p, o));
    }

	private void _addToTripleArray(Statement triple)
    {
        //TODO we should call the validation of RDFTriple
        //TODO We can depend now only on the validation provided by org.openrdf

		//if(triple.validate())
        {
            this.triples.add(triple);
		}
	}

    public void addMetadataTriple(Resource s, URI p, Value o) 
    {
        //this.metadataTriples = new ArrayList();
        this.metadataTriples.add(new RDFTriple(s, p, o));
    }

    public void addPredicate(String  p)
    {
        this.predicates.put(p, true);
    }

    private String getPredicateLabel(String p)
    {
    	p = p.replace(Constants.DB_PROPERTY_NS,"");

        Pattern pattern =  Pattern.compile("/[A-Z]/");
        Matcher matcher = pattern.matcher(p);
        String resultingString = matcher.replaceAll(" \\0"); 

        resultingString = resultingString.toLowerCase();

        //TODO it is better to use decode with the parameter of encoding too
        return URLDecoder.decode(resultingString);
    	//return urldecode( strtolower( preg_replace("/[A-Z]/",' \\0',p)) );
    }

	public void clear()
    {
		triples = new ArrayList();
		metadataTriples = new ArrayList();
		predicates = new HashMap();
	}

}

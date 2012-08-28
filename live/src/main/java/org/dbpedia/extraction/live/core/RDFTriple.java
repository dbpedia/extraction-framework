package org.dbpedia.extraction.live.core;


import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.apache.log4j.Logger;
import org.dbpedia.helper.Triple;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
/*import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;*/


/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 1, 2010
 * Time: 11:21:20 AM
 * This class constructs RDF triples.
 */
public class RDFTriple extends Triple {
    
    //Initializing the Logger
    private static Logger logger = Logger.getLogger(RDFTriple.class);
    private static String pageCacheKey = null;
    private static Resource pageCacheValue = null;

    private String SPARULPattern = null;

    public RDFTriple(Resource subject, Property predicate, RDFNode object)
    {
        super(subject, predicate, object);
    }

   public String toSPARULPattern()
   {
        //init
        if(this.SPARULPattern == null)
        {

            this.SPARULPattern = org.dbpedia.extraction.live.core.Util.convertToSPARULPattern(this.getSubject()) + " " +
                    org.dbpedia.extraction.live.core.Util.convertToSPARULPattern(this.getPredicate()) + " " +
                    org.dbpedia.extraction.live.core.Util.convertToSPARULPattern(this.getObject()) + " . ";
        }
        return this.SPARULPattern;
   }

   public static Resource page(String pageID) {
       String encPageID = "";
       if(!pageID.equals(pageCacheKey)){
           try{
               encPageID = URLEncoder.encode(org.dbpedia.extraction.live.core.Util.wikipediaEncode(pageID), "UTF-8");
               String strSubstring = encPageID.substring(0,1);
               //strSubstring = strSubstring.toUpperCase() + encPageID.substring(1);
               String returnPageID = strSubstring.toUpperCase() + encPageID.substring(1);
               String resourceURI = Constants.DB_RESOURCE_NS + returnPageID;
               Resource uri = ResourceFactory.createResource(resourceURI);
               pageCacheKey = pageID;
               pageCacheValue = uri;
           }
           catch (UnsupportedEncodingException exp){
               logger.error("Unsupported encoding is used in encoding the page title");
           }
       }
       return pageCacheValue;
   }
    
}

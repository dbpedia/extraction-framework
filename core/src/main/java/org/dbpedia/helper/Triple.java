package org.dbpedia.helper;


import org.apache.log4j.Logger;
import org.dbpedia.extraction.util.Language;
import org.dbpedia.extraction.util.WikiUtil;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.rio.ntriples.NTriplesUtil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 1, 2010
 * Time: 11:21:20 AM
 * This class constructs RDF triples.
 */
public class Triple extends StatementImpl{

    //Initializing the Logger
    private static Logger logger = null;
    private static String pageCacheKey = null;
    private static URI pageCacheValue = null;

    static
    {
        try
        {
            logger = Logger.getLogger(Triple.class);
        }
        catch (Exception exp){

        }
    }

    public Triple(Resource subject, URI predicate, Value object)
    {
        super(subject, predicate, object);
    }

    public String getMD5HashCode()
    {
        String hashCode = null;
        try
        {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");

            algorithm.reset();
            algorithm.update(this.toString().getBytes());

            byte messageDigest[] = algorithm.digest();

            hashCode = getHexString(messageDigest);

        }
        catch(NoSuchAlgorithmException nsae){
            logger.error("FAILED to create hash code for " + this.toNTriples());
        }
        catch(Exception exp){
            logger.error(exp.getMessage());
        }
        return hashCode;
    }

public static String getHexString(byte[] b) throws Exception {
  String result = "";
  for (int i=0; i < b.length; i++) {
    result +=
          Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
  }
  return result;
}


    public String toNTriples()
    {
        String strNTriples = NTriplesUtil.toNTriplesString(this.getSubject()) + " " +
            NTriplesUtil.toNTriplesString(this.getPredicate()) + " " +
            NTriplesUtil.toNTriplesString(this.getObject()) + " .\n" ;

		return strNTriples;
    }
    public static URI page(String pageID) {
       if(!pageID.equals(pageCacheKey)){
           String encPageID = WikiUtil.wikiEncode(pageID, Language.Default(), true);
           String strSubstring = encPageID.substring(0,1);
           String returnPageID = strSubstring.toUpperCase() + encPageID.substring(1);
           //TODO make resource domain configurable
           String resourceURI = "http://dbpedia.org/resource/"+ returnPageID;
           URI uri = new URIImpl(resourceURI);
           pageCacheKey = pageID;
           pageCacheValue = uri;
       }
       return pageCacheValue;
   }

}
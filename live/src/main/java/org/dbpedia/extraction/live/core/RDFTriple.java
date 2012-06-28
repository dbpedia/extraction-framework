package org.dbpedia.extraction.live.core;


import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import org.apache.log4j.Logger;
import org.dbpedia.helper.Triple;
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
    private static Logger logger = null;
    private static String pageCacheKey = null;
    private static Resource pageCacheValue = null;

    private String SPARULPattern = null;

    static
    {
        try
        {
            logger = Logger.getLogger(RDFTriple.class.getName());
            //Assert.assertNotNull("Logger cannot be null", logger);
        }
        catch (Exception exp){

        }
    }

    public RDFTriple(Resource subject, Property predicate, RDFNode object)
    {
        super(subject, predicate, object);
    }

//    public String getMD5HashCode()
//    {
//        String hashCode = null;
//        try
//        {
//            MessageDigest algorithm = MessageDigest.getInstance("MD5");
//
//            algorithm.reset();
//            algorithm.update(this.toString().getBytes());
//
//            byte messageDigest[] = algorithm.digest();
//
////           hashCode = messageDigest.toString();
//            hashCode = getHexString(messageDigest);
//
////            try{
////                MessageDigest algorithm = MessageDigest.getInstance("MD5");
////
////            algorithm.reset();
////                String str = "Hello World";
////            algorithm.update(str.getBytes("iso-8859-1"), 0, str.length());
////
////            byte messageDigest[] = algorithm.digest();
////
////           hashCode = getHexString(messageDigest);
////
////            }
////            catch(Exception exp){
////
////            }
//
//
//
//        }
//        catch(NoSuchAlgorithmException nsae){
//            logger.error("FAILED to create hash code for " + this.toNTriples());
//        }
//        catch(Exception exp){
//            logger.error(exp.getMessage());
//        }
//        return hashCode;
//    }
//
//public static String getHexString(byte[] b) throws Exception {
//  String result = "";
//  for (int i=0; i < b.length; i++) {
//    result +=
//          Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
//  }
//  return result;
//}


//    public String toNTriples()
//    {
//        String strNTriples = NTriplesUtil.toNTriplesString(this.getSubject()) + " " +
//            NTriplesUtil.toNTriplesString(this.getPredicate()) + " " +
//            NTriplesUtil.toNTriplesString(this.getObject()) + " .\n" ;
//
//        //Assert.assertTrue("NTriples string cannot be null or empty", (strNTriples != null && strNTriples != ""));
//
//		return strNTriples;
//    }

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

       //Assert.assertTrue("PageID cannot be null or empty", (pageID != null && pageID != ""));
       if(!pageID.equals(pageCacheKey)){
           String encPageID = org.dbpedia.extraction.live.core.Util.wikipediaEncode(pageID);
           String strSubstring = encPageID.substring(0,1);
           //strSubstring = strSubstring.toUpperCase() + encPageID.substring(1);
           String returnPageID = strSubstring.toUpperCase() + encPageID.substring(1);
           String resourceURI = Constants.DB_RESOURCE_NS + returnPageID;
           Resource uri = ResourceFactory.createResource(resourceURI);
           pageCacheKey = pageID;
           pageCacheValue = uri;
       }
       //Assert.assertNotNull("PageID cannot be null", pageCacheValue);
       return pageCacheValue;
   }
    
}

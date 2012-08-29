package org.dbpedia.helper;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.util.Language;
import org.dbpedia.extraction.util.WikiUtil;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;

import static org.dbpedia.extraction.util.RichString.toRichString;

/*import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.rio.ntriples.NTriplesUtil;*/

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Aug 19, 2010
 * Time: 12:39:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class CoreUtil {
    //Initialize the logger
    private static Logger logger = Logger.getLogger(CoreUtil.class);

    public static String convertToSPARULPattern(RDFNode requiredResource)
    {
        String storeSpecific = "VIRTUOSO";
        return convertToSPARULPattern(requiredResource, storeSpecific);
    }

    public  static String convertToSPARULPattern(Object requiredResource){
        String storeSpecific = "VIRTUOSO";
        return convertToSPARULPattern(requiredResource, storeSpecific);
    }

    private static String convertToSPARULPattern(Object requiredResource, String storeSpecific)
    {
        try{
            RDFNode valResource = (RDFNode) requiredResource;
            return convertToSPARULPattern(valResource, storeSpecific);
        }
        catch(Exception exp){
            logger.warn("Invalid resource object is passed", exp);
            return requiredResource.toString();
        }
    }

    public static String convertToSPARULPattern(RDFNode requiredResource, String storeSpecific)
    {
        String strSPARULPattern = "";
        if(requiredResource instanceof Resource){

            try{
                URI uri = new URI(((Resource) requiredResource).getURI());
                strSPARULPattern = "<" + uri + ">";
            }
            catch (Exception exp){
                logger.error("Resource \"" + requiredResource  + "\" cannot be URL encoded.");
            }
        }
        else if(requiredResource instanceof Literal){

            String quotes="";

            if(storeSpecific.toUpperCase().equals("VIRTUOSO"))
                quotes = "\"\"\"";
            else
                quotes = "\"";

            if((((Literal) requiredResource).getDatatype() == null) && (((Literal) requiredResource).getLanguage() == null))
                strSPARULPattern = quotes + escapeString( ((Literal) requiredResource).getValue().toString()) + quotes;
            else if(((Literal) requiredResource).getDatatype() == null)
                strSPARULPattern = quotes + escapeString(((Literal) requiredResource).getValue().toString() ) + quotes +
                        "@" + ((Literal) requiredResource).getLanguage();
            else
                strSPARULPattern = quotes + escapeString(((Literal) requiredResource).getValue().toString()) + quotes + "^^<" +
                        ((Literal) requiredResource).getDatatype().getURI() + ">";

        }
        return strSPARULPattern;
    }

    private static String escapeString(String input){
        StringBuilder outputString = new StringBuilder();
        for (char c :input.toCharArray())
        {
            if (c == '\\' || c == '"')
            {
                outputString.append('\\' + c);
            }
            else if (c == '\n')
            {
                outputString.append("\\n");
            }
            else if (c == '\r')
            {
                outputString.append("\\r");
            }
            else if (c == '\t')
            {
                outputString.append("\\t");
            }
            else if (c >= 32 && c < 127)
            {
                outputString.append(c);
            }
            else
            {
                outputString.append("\\u");

                String hexStr = Integer.toHexString(c).toUpperCase();
                int pad = 4 - hexStr.length();

                while (pad > 0)
                {
                    outputString.append('0');
                    pad -= 1;
                }

                outputString.append(hexStr);
            }
        }
        return outputString.toString();
    }

    /**
     * @param page_title: decoded page title
     * @return encoded page title
     */
    public static String wikipediaEncode(String page_title) {
        return toRichString(WikiUtil.wikiEncode(page_title)).capitalize(Language.apply(LiveOptions.options.get("language")).locale());
    }

    /**
     * Encodes a URI, as in case of DBpedia the page title is the only part that should be
     * @param   uri the URI of the page that should be encoded
     * @return Encoded URI
     */
    public static String encodeURI(String uri){
        int lastSlashIndex = uri.lastIndexOf("/");

        String namespacePart = uri.substring(0, lastSlashIndex);
        String pageTitlePart = uri.substring(lastSlashIndex+1);

        String resultingURI = uri;

        try{
            resultingURI = namespacePart + "/" + URLEncoder.encode(pageTitlePart, "UTF-8");
        }
        catch (UnsupportedEncodingException exp){
            logger.error("Page \"" + pageTitlePart + "\" cannot be encoded");
        }

        return resultingURI;
    }

    /**
     * Writes a triple as NTriples string
     * @param triple    The required triple
     * @return  The NTriples represntation of the passed triple
     */
    public static String writeTripleAsNTriples(Statement triple)
    {

        return convertToSPARULPattern(triple.getSubject()) + " " + convertToSPARULPattern(triple.getPredicate())
                +" " + convertToSPARULPattern(triple.getObject()) + " .";
    }

}

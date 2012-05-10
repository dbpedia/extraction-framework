package org.dbpedia.helper;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.dbpedia.extraction.util.Language;
import org.dbpedia.extraction.util.WikiUtil;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.rio.ntriples.NTriplesUtil;
import static org.dbpedia.extraction.util.RichString.toRichString;

/**
 * TODO: why is this class necessary? Quad.render() does pretty much the same thing...
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Aug 19, 2010
 * Time: 12:39:31 PM
 * To change this template use File | Settings | File Templates.
 */
public class CoreUtil {
    //Initialize the logger
    private static Logger logger = Logger.getLogger(CoreUtil.class.getName());

    public static String convertToSPARULPattern(Value requiredResource)
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
            Value valResource = (Value) requiredResource;
            return convertToSPARULPattern(valResource, storeSpecific);
        }
        catch(Exception exp){
            logger.log(Level.WARNING, "Invalid resource object is passed", exp);
            return requiredResource.toString();
        }
    }

    public static String convertToSPARULPattern(Value requiredResource, String storeSpecific)
    {
        String strSPARULPattern = "";
        if(requiredResource instanceof URI){

            strSPARULPattern = NTriplesUtil.toNTriplesString(requiredResource);

        }
        else if(requiredResource instanceof BNode){

            strSPARULPattern = NTriplesUtil.toNTriplesString(requiredResource);
            strSPARULPattern = strSPARULPattern.replace("%", "_");

        }
        else if(requiredResource instanceof Literal){

            /*if((storeSpecific == null) || (storeSpecific.equals("")))
                storeSpecific = LiveOptions.options.get("Store.SPARULdialect");*/

            String quotes="";

            //TODO this point should be checked i.e. whether we must place 2 double quotes or not

            if(storeSpecific.toUpperCase().equals("VIRTUOSO"))
                quotes = "\"\"\"";
            else
                quotes = "\"";

            if((((Literal) requiredResource).getDatatype() == null) && (((Literal) requiredResource).getLanguage() == null))
                strSPARULPattern = quotes + escapeString( requiredResource.stringValue()) + quotes;
            else if(((Literal) requiredResource).getDatatype() == null)
                strSPARULPattern = quotes + escapeString(requiredResource.stringValue()) + quotes +
                        "@" + ((Literal) requiredResource).getLanguage();
            else
                strSPARULPattern = quotes + escapeString(requiredResource.stringValue()) + quotes + "^^<" +
                        ((Literal) requiredResource).getDatatype() + ">";

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

                //val hexStr = c.toHexString().toUpperCase
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
     * @deprecated please use WikiUtil.wikiEncode!
     * @param page_title: decoded page title
     * @return encoded page title
     */
    @Deprecated
    public static String wikipediaEncode(String page_title) {
        return toRichString(WikiUtil.wikiEncode(page_title)).capitalize(Language.English().locale());
     }
}

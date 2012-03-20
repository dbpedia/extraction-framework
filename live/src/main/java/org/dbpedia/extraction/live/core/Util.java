package org.dbpedia.extraction.live.core;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dbpedia.helper.CoreUtil;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 5, 2010
 * Time: 6:06:54 PM
 * This class contains some utility functions used in live extraction.
 */
public class Util extends CoreUtil{

    //Initializing the Logger
    private static Logger logger = null;
    public static HashMap <String, HashMap<String, String>>MEDIAWIKI_NAMESPACES = null;

    static{
        try
        {
            logger = Logger.getLogger(Util.class.getName());

            //Initialize the MEDIAWIKI_NAMESPACES hashmap
            MEDIAWIKI_NAMESPACES = new HashMap <String, HashMap<String, String>>();

            //Initialize a hashmap for the legal key
            HashMap<String, String> legalHashmap = new HashMap<String, String> ();
            legalHashmap.put(Constants.MW_CATEGORY_NAMESPACE, "");
            legalHashmap.put(Constants.MW_TEMPLATE_NAMESPACE, "");
            legalHashmap.put(Constants.MW_FILE_NAMESPACE, "");
            legalHashmap.put(Constants.MW_FILEALTERNATIVE_NAMESPACE, "");

            MEDIAWIKI_NAMESPACES.put("legal", legalHashmap);
            /////////////////////////////////////////////////

            //Initialize a hashmap for the English language "en" key
            HashMap<String, String> englishHashmap = new HashMap<String, String> ();
            englishHashmap.put(Constants.MW_CATEGORY_NAMESPACE, "Category");
            englishHashmap.put(Constants.MW_TEMPLATE_NAMESPACE, "Template");
            englishHashmap.put(Constants.MW_FILE_NAMESPACE, "File");
            englishHashmap.put(Constants.MW_FILEALTERNATIVE_NAMESPACE, "Image");
            MEDIAWIKI_NAMESPACES.put("en", englishHashmap);
            /////////////////////////////////////////////////

            //Initialize a hashmap for the German language "de" key
            HashMap<String, String> germanHashmap = new HashMap<String, String> ();
            germanHashmap.put(Constants.MW_CATEGORY_NAMESPACE, "Kategorie");
            germanHashmap.put(Constants.MW_TEMPLATE_NAMESPACE, "Template");
            germanHashmap.put(Constants.MW_FILE_NAMESPACE, "Datei");
            germanHashmap.put(Constants.MW_FILEALTERNATIVE_NAMESPACE, "Bild");
            MEDIAWIKI_NAMESPACES.put("de", germanHashmap);
            /////////////////////////////////////////////////

            //Initialize a hashmap for the Korean language "ko" key
            HashMap<String, String> koreanHashmap = new HashMap<String, String> ();
            koreanHashmap.put(Constants.MW_CATEGORY_NAMESPACE, "??");
            koreanHashmap.put(Constants.MW_TEMPLATE_NAMESPACE, "?");
            koreanHashmap.put(Constants.MW_FILE_NAMESPACE, "??");
            koreanHashmap.put(Constants.MW_FILEALTERNATIVE_NAMESPACE, "??");
            MEDIAWIKI_NAMESPACES.put("ko", koreanHashmap);
            /////////////////////////////////////////////////
        }
        catch (Exception exp){

        }
    }

    public static String deck(String in, int space){
        String w = StringUtils.repeat("&nbsp;",space);
        return "<td>" + w + in + w + "</td>";
 	}

    public static String deck(String in){
        return deck(in, 0);
    }

    public static String row(String in, int space){
        String w = StringUtils.repeat("&nbsp;",space);
        return "<td>" + w + in + w + "</td>" + "\n";
 	}

    public static String row(String in){
        return row(in, 0);
    }

//    public static String convertToSPARULPattern(Value requiredResource)
//    {
//        String storeSpecific = "VIRTUOSO";
//        return convertToSPARULPattern(requiredResource, storeSpecific);
//    }
//
//    public  static String convertToSPARULPattern(Object requiredResource){
//        String storeSpecific = "VIRTUOSO";
//        return convertToSPARULPattern(requiredResource, storeSpecific);
//    }
//
//    private static String convertToSPARULPattern(Object requiredResource, String storeSpecific)
//    {
//        try{
//            Value valResource = (Value) requiredResource;
//            return convertToSPARULPattern(valResource, storeSpecific);
//        }
//        catch(Exception exp){
//            logger.error("Invalid resource object is passed");
//            return requiredResource.toString();
//        }
//    }
//
//    public static String convertToSPARULPattern(Value requiredResource, String storeSpecific)
//    {
//        String strSPARULPattern = "";
//        if(requiredResource instanceof URI){
//
//            strSPARULPattern = NTriplesUtil.toNTriplesString(requiredResource);
//
//        }
//        else if(requiredResource instanceof BNode){
//
//            strSPARULPattern = NTriplesUtil.toNTriplesString(requiredResource);
//            strSPARULPattern = strSPARULPattern.replace("%", "_");
//
//        }
//        else if(requiredResource instanceof Literal){
//
//            //If the application is working in multithreading mode, we must attach the thread id to the timer name
//            //to avoid the case that a thread stops the timer of another thread.
//            String timerName = "Converting literal to SPARULPattern" +
//                    (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");
//
//            Timer.start(timerName);
//
//            if((storeSpecific == null) || (storeSpecific.equals("")))
//                storeSpecific = LiveOptions.options.get("Store.SPARULdialect");
//
//            String quotes="";
//
//            //TODO this point should be checked i.e. whether we must place 2 double quotes or not
//
//            if(storeSpecific.toUpperCase().equals("VIRTUOSO"))
//                quotes = "\"\"\"";
//            else
//                quotes = "\"";
//
////            if((((Literal) requiredResource).getDatatype() == null) && (((Literal) requiredResource).getLanguage() == null))
////                strSPARULPattern = quotes + escape( requiredResource.stringValue()) + quotes;
////            else if(((Literal) requiredResource).getDatatype() == null)
////                strSPARULPattern = quotes + escape(requiredResource.stringValue()) + quotes +
////                        "@" + ((Literal) requiredResource).getLanguage();
////            else
////                strSPARULPattern = quotes + escape(requiredResource.stringValue()) + quotes + "^^<" +
////                        ((Literal) requiredResource).getDatatype() + ">";
//
//            if((((Literal) requiredResource).getDatatype() == null) && (((Literal) requiredResource).getLanguage() == null))
//                strSPARULPattern = quotes + escapeString( requiredResource.stringValue()) + quotes;
//            else if(((Literal) requiredResource).getDatatype() == null)
//                strSPARULPattern = quotes + escapeString(requiredResource.stringValue()) + quotes +
//                        "@" + ((Literal) requiredResource).getLanguage();
//            else
//                strSPARULPattern = quotes + escapeString(requiredResource.stringValue()) + quotes + "^^<" +
//                        ((Literal) requiredResource).getDatatype() + ">";
//
//            Timer.stop(timerName);
//        }
//        return strSPARULPattern;
//    }

// Input is an UTF-8 encoded string. Output is the string in N-Triples encoding.
    // Checks for invalid UTF-8 byte sequences and replaces them with \uFFFD (white
    // question mark inside black diamond character)
    //
    // Sources:
    // http://www.w3.org/TR/rdf-testcases/#ntrip_strings
    // http://en.wikipedia.org/wiki/UTF-8
    // http://www.cl.cam.ac.uk/~mgk25/ucs/examples/UTF-8-test.txt
    private static String escape(String str) {
        // Replaces all byte sequences that need escaping. Characters that can
        // remain unencoded in N-Triples are not touched by the regex. The
        // replaced sequences are:
        //
        // 0x00-0x1F   non-printable characters
        // 0x22        double quote (")
        // 0x5C        backslash (\)
        // 0x7F        non-printable character (Control)
        // 0x80-0xBF   unexpected continuation byte,
        // 0xC0-0xFF   first byte of multi-byte character,
        //             followed by one or more continuation byte (0x80-0xBF)
        //
        // The regex accepts multi-byte sequences that don't have the correct
        // number of continuation bytes (0x80-0xBF). This is handled by the
        // callback.


//        return preg_replace_callback(
//                "/[\\x00-\\x1F\\x22\\x5C\\x7F]|[\\x80-\\xBF]|[\\xC0-\\xFF][\\x80-\\xBF]*/",
//                array('RDFliteral', 'escape_callback'),
//                $str);
        StringBuffer resultingString = new StringBuffer();
        Pattern regex = Pattern.compile("/[\\x00-\\x1F\\x22\\x5C\\x7F]|[\\x80-\\xBF]|[\\xC0-\\xFF][\\x80-\\xBF]*/");

        Matcher regexMatcher = regex.matcher(str);

        while (regexMatcher.find()) {
            // You can vary the replacement text for each match on-the-fly
            MatchResult matchResult = regexMatcher.toMatchResult();

            //TODO this was changed from 1 to 0
            String strMatch = matchResult.group(0);
            String replacementString = "";

            //TODO we should convert the first character of the matched string to byte
            String encoded_character = strMatch.toString();

            byte Byte = (byte)encoded_character.charAt(0);
            byte codepoint = 0;

            int Bytes = 0;

            // Single-byte characters (0xxxxxxx, hex 00-7E)
            if (Byte == 0x09) replacementString = "\\t";
            if (Byte == 0x0A) replacementString = "\\n";
            if (Byte == 0x0D) replacementString = "\\r";
            if (Byte == 0x22) replacementString = "\\\"";
            if (Byte == 0x5C) replacementString = "\\\\";
            if (Byte < 0x20 || Byte == 0x7F) {
//                 encode as \ u00XX

                replacementString =  "\\u00" + String.format("%02X", Byte);
            }
            // Multi-byte characters
            if (Byte < 0xC0) {
                // Continuation bytes (0x80-0xBF) are not allowed to appear as first byte
                replacementString = Constants.error_character;
            }
            if (Byte < 0xE0) { // 110xxxxx, hex C0-DF
                Bytes = 2;
                codepoint = (byte)(Byte & 0x1F);
            } else if (Byte < 0xF0) { // 1110xxxx, hex E0-EF
                Bytes = 3;
                codepoint = (byte)(Byte & 0x0F);
            } else if (Byte < 0xF8) { // 11110xxx, hex F0-F7
                Bytes = 4;
                codepoint = (byte)(Byte & 0x07);
            } else if (Byte < 0xFC) { // 111110xx, hex F8-FB
                Bytes = 5;
                codepoint = (byte)(Byte & 0x03);
            } else if (Byte < 0xFE) { // 1111110x, hex FC-FD
                Bytes = 6;
                codepoint = (byte)(Byte & 0x01);
            } else { // 11111110 and 11111111, hex FE-FF, are not allowed
                replacementString = Constants.error_character;
            }

            // Verify correct number of continuation Bytes (0x80 to 0xBF)
            int length = encoded_character.length();
            if (length < Bytes) { // not enough continuation bytes
                replacementString = Constants.error_character;
            }

            String rest = "";
            if (length > Bytes) { // Too many continuation bytes -- show each as one error
                rest = StringUtils.repeat(Constants.error_character, length - Bytes);
            } else {
                rest = "";
            }
            // Calculate Unicode codepoints from the bytes
            for (byte i = 0; i < Bytes; i++) {
                // Loop over the additional bytes (0x80-0xBF, 10xxxxxx)
                // Add their lowest six bits to the end of the codepoint
                Byte = (byte)(encoded_character.charAt(i));
                codepoint = (byte)((codepoint << 6) | (Byte & 0x3F));
            }
            // Check for overlong encoding (character is encoded as more Bytes than
            // necessary, this must be rejected by a safe UTF-8 decoder)
            if ((Bytes == 2 && codepoint <= 0x7F) ||
                (Bytes == 3 && codepoint <= 0x7FF) ||
                (Bytes == 4 && codepoint <= 0xFFFF) ||
                (Bytes == 5 && codepoint <= 0x1FFFFF) ||
                (Bytes == 6 && codepoint <= 0x3FFFFF)) {
                replacementString = Constants.error_character + rest;
            }
            // Check for UTF-16 surrogates, which must not be used in UTF-8
            if (codepoint >= 0xD800 && codepoint <= 0xDFFF) {
                replacementString = Constants.error_character + rest;
            }
            // Misc. illegal code positions
            if (codepoint == 0xFFFE || codepoint == 0xFFFF) {
                replacementString = Constants.error_character + rest;
            }
            if (codepoint <= 0xFFFF) {
//                 0x0100-0xFFFF, encode as \ uXXXX
                replacementString = "\\u" + String.format("%04X", codepoint) + rest;
            }
            if (codepoint <= 0x10FFFF) {
                // 0x10000-0x10FFFF, encode as \UXXXXXXXX
                replacementString = "\\U" + String.format("%08X", codepoint) + rest;
            }
            // Unicode codepoint above 0x10FFFF, no characters have been assigned
            // to those codepoints
            replacementString = Constants.error_character + rest;

            regexMatcher.appendReplacement(resultingString, replacementString);
        }

        regexMatcher.appendTail(resultingString);

        return resultingString.toString();
    }

//    private static String escapeString(String input){
//        StringBuilder outputString = new StringBuilder();
//		for (char c :input.toCharArray())
//		{
//			if (c == '\\' || c == '"')
//			{
//				outputString.append('\\' + c);
//			}
//			else if (c == '\n')
//			{
//				outputString.append("\\n");
//			}
//			else if (c == '\r')
//			{
//				outputString.append("\\r");
//			}
//			else if (c == '\t')
//			{
//				outputString.append("\\t");
//			}
//			else if (c >= 32 && c < 127)
//			{
//				outputString.append(c);
//			}
//			else
//			{
//				outputString.append("\\u");
//
//				//val hexStr = c.toHexString().toUpperCase
//                String hexStr = Integer.toHexString(c).toUpperCase();
//				int pad = 4 - hexStr.length();
//
//				while (pad > 0)
//				{
//					outputString.append('0');
//					pad -= 1;
//				}
//
//				outputString.append(hexStr);
//			}
//		}
//		return outputString.toString();
//	}

    public static boolean isStringNullOrEmpty(String str)
    {
        if((str == null) || (str.equals("")))
            return true;
        return false;

    }

//    public static String wikipediaEncode(String page_title) {
// 	String strEncodedPageTitle = URLEncoder.encode(page_title.trim().replace(" ","_"));
// 	// Decode slash "/", colon ":", as wikimedia does not encode these
// 	strEncodedPageTitle = strEncodedPageTitle.replace("%2F","/");
// 	strEncodedPageTitle =  strEncodedPageTitle.replace("%3A",":");
// 	return strEncodedPageTitle;
// 	}

    public static String getDBpediaCategoryPrefix(String language){
        try
        {
          return Constants.DB_RESOURCE_NS + URLEncoder.encode(_getMediaWikiNamespace(language, Constants.MW_CATEGORY_NAMESPACE), "UTF-8")+':';
        }
        catch (UnsupportedEncodingException e)
        {
          throw new RuntimeException(e);
        }
    }

 	public static String getMediaWikiCategoryNamespace(String language){
 	    return _getMediaWikiNamespace(language, Constants.MW_CATEGORY_NAMESPACE);
 	}

 	public static String getMediaWikiNamespace(String language, String what){
 	    return _getMediaWikiNamespace(language, what);
 	}

    private static String _getMediaWikiNamespace(String language, String what){
        if(MEDIAWIKI_NAMESPACES == null)
        MEDIAWIKI_NAMESPACES = new HashMap<String, HashMap<String, String>>();
        
        if(MEDIAWIKI_NAMESPACES.get("legal").get(what) == null){
            logger.error("no namespace for " + what + " illegal use, does not exist");
        };

        if(MEDIAWIKI_NAMESPACES.get(language) == null){
            logger.warn("namespaces not set in core/language_namespaces for: " + language);
            MEDIAWIKI_NAMESPACES.put(language, new HashMap<String, String>());
        }

        if(MEDIAWIKI_NAMESPACES.get(language).get(what) == null){
            logger.warn("no namespace for " + what + " in language: " + language +
                    " in core/language_namespaces using english instead of: " + language);

            String valueForEnglishLanguage = MEDIAWIKI_NAMESPACES.get("en").get(what);
            MEDIAWIKI_NAMESPACES.get(language).put(what, valueForEnglishLanguage);
        }
        return MEDIAWIKI_NAMESPACES.get(language).get(what);
    } 

}

package org.dbpedia.extraction.live.core;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 5, 2010
 * Time: 6:06:54 PM
 * This class contains some utility functions used in live extraction.
 */
public class Util {

    //Initializing the Logger
    private static Logger logger = null;

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


}

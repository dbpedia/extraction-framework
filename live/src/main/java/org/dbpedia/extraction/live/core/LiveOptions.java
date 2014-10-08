package org.dbpedia.extraction.live.core;

import org.ini4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 6, 2010
 * Time: 5:33:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class LiveOptions {
    //Initializing the Options file
    public static Options options;
    private static Logger logger = LoggerFactory.getLogger(LiveOptions.class);;
    static{
        try{

            File OptionsFile = new File("./live.ini");

            options = new Options(OptionsFile);
        }
        catch (IOException exp){
            logger.error("live.ini file not found", exp);
            System.exit(1);
        }
        catch (Exception exp){
            logger.error(exp.getMessage(), exp);
        }

    }

    public static String language = LiveOptions.options.get("language");
}

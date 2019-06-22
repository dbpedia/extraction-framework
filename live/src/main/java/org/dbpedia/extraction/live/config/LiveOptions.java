package org.dbpedia.extraction.live.config;

import org.dbpedia.extraction.live.config.extractors.LiveExtractorConfigReader;
import org.ini4j.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

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
            System.exit(-1);//checked
        }
        catch (Exception exp){
            logger.error(exp.getMessage(), exp);
        }

    }

    public static List<String> languages = LiveExtractorConfigReader.readLanguages();

}

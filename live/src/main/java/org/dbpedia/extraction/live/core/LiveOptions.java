package org.dbpedia.extraction.live.core;

import org.apache.log4j.Logger;
import org.ini4j.*;
import scala.tools.nsc.io.Directory;

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
    private static Logger logger;
    static{
        try{
            logger = Logger.getLogger(LiveOptions.class);

            //Assert.assertNotNull("Logger cannot be null", logger);

            File OptionsFile = new File("./live/dbpedia_default.ini");

            //Assert.assertTrue("dbpedia_default.ini file does not exist", OptionsFile.exists());

            options = new Options(OptionsFile);
        }
        catch (IOException exp){
            logger.fatal("dbpedia_default.ini file not found");
            System.exit(1);
        }
        catch (Exception exp){
            logger.error(exp.getMessage());
        }

    }
}

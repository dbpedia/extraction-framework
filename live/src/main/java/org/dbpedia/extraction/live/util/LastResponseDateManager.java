package org.dbpedia.extraction.live.util;

import org.apache.log4j.Logger;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

/**
 * Created by IntelliJ IDEA.
 * User: Morsey
 * Date: Jul 28, 2010
 * Time: 6:26:07 PM
 * This class is responsible for reading and writing the response dates to files, in order to enable resume starting
 * from the last working point both for live extraction and for mapping update 
 */
public class LastResponseDateManager {

    private static Logger logger = Logger.getLogger(LastResponseDateManager.class);

    
    public static String getLastResponseDate(String strFileName)
    {
        String strLastResponseDate = null;

        try{
            FileInputStream fsLastResponseDateFile = new FileInputStream(strFileName);

            int ch;
            strLastResponseDate="";
            while( (ch = fsLastResponseDateFile.read()) != -1)
                strLastResponseDate += (char)ch;

        }
        catch(Exception exp){
           logger.error(ExceptionUtil.toString(exp));
        }

        return strLastResponseDate;

    }

    public static void writeLastResponseDate(String strFileName, String strLastResponseDate)
    {
        try{
            FileOutputStream fsLastResponseDateFile = new FileOutputStream(strFileName);
            OutputStreamWriter osWriter = new OutputStreamWriter(fsLastResponseDateFile);
            osWriter.write(strLastResponseDate);
            osWriter.close();
        }
        catch(Exception exp){
           logger.error(ExceptionUtil.toString(exp));
        }
    }
}

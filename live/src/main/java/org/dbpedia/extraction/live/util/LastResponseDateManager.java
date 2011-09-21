package org.dbpedia.extraction.live.util;

import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.core.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

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

    public static String getNow() {
        return UTCHelper.transformToUTC(new Date());
    }
    
    public static String getLastResponseDate(String strFileName)
    {
        // TODO Actually, we should get a file-object here in the first place
        File file = new File(strFileName);
        if(!file.exists()) {
            return null;
        }


        String strLastResponseDate = null;
        FileInputStream fsLastResponseDateFile = null;

        try{
            fsLastResponseDateFile = new FileInputStream(strFileName);

            int ch;
            strLastResponseDate="";
            while( (ch = fsLastResponseDateFile.read()) != -1)
                strLastResponseDate += (char)ch;


        }
        catch(Exception exp){
           logger.error(ExceptionUtil.toString(exp));
        }
        finally {
            try{
                if(fsLastResponseDateFile != null)
                    fsLastResponseDateFile.close();

            }
            catch (Exception exp){
                logger.error("File " + strFileName + " cannot be closed due to " + exp.getMessage());
            }

        }

        return strLastResponseDate;

    }

    public static void writeLastResponseDate(String strFileName, String strLastResponseDate)
    {
        FileOutputStream fsLastResponseDateFile = null;
        OutputStreamWriter osWriter = null;

        try{
            fsLastResponseDateFile = new FileOutputStream(strFileName);
            osWriter = new OutputStreamWriter(fsLastResponseDateFile);
            osWriter.write(strLastResponseDate);
            osWriter.flush();
        }
        catch(Exception exp){
           logger.error(ExceptionUtil.toString(exp));
        }
        finally {
            try{
                if(osWriter != null)
                    osWriter.close();

                if(fsLastResponseDateFile != null)
                    fsLastResponseDateFile.close();
            }
            catch (Exception exp){
                logger.error("File " + strFileName + " cannot be closed due to " + exp.getMessage());
            }
        }
    }
}

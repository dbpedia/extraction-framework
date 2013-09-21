package org.dbpedia.extraction.live.util;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;

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
        return DateUtil.transformToUTC(new Date());
    }

    /**
     * Use getLastResponseDate(File file) instead.
     *
     * @param strFileName
     * @return
     */
    public static String getLastResponseDate(String strFileName)
    {
        // TODO Actually, we should get a file-object here in the first place
        File file = new File(strFileName);

        return getLastResponseDate(file);
    }


    public static String getLastResponseDate(File file)
    {
        if(!file.exists()) {
            return null;
        }


        String strLastResponseDate = null;
        FileInputStream fsLastResponseDateFile = null;

        try{
            fsLastResponseDateFile = new FileInputStream(file);

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
                logger.error("File " + file.getAbsolutePath() + " cannot be closed due to " + exp.getMessage());
            }

        }

        return strLastResponseDate.trim();

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

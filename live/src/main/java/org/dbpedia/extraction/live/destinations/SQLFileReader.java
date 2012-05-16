package org.dbpedia.extraction.live.destinations;

import java.io.FileInputStream;
import java.sql.PreparedStatement;
import java.util.ArrayList;

import org.apache.log4j.*;
import org.dbpedia.extraction.live.core.JDBC;


/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Aug 20, 2010
 * Time: 4:12:34 PM
 * This class reads the SQL file created by the SQLFileDestination class during dump generation process and creates the
 * appropriate insert statements and executes them accordingly
 */
public class SQLFileReader {
    private static Logger logger = null;
    static{
        logger = Logger.getLogger(SQLFileReader.class);
    }

    static Appender myAppender;


    private static final String FILENAME = "./SqlLog.sql";
    private static final String TABLENAME = "dbpedia_triples";
    private static final String FIELD_OAIID = "oaiid";
    private static final String FIELD_RESOURCE = "resource";
    private static final String FIELD_JSON_BLOB = "content";

    /**
     * Reads the SQL file, creates the insert statements and then performs it against the database.
     */
    public static void readSQLFile(){

        logger.setLevel(Level.ALL);

        // Define Appender
        myAppender = new ConsoleAppender(new SimpleLayout());

        logger.addAppender(myAppender);

        FileInputStream fs = null;
        try{
            fs= new FileInputStream(FILENAME);
            //We should read the data as a byte sequence and not as strings because the JSON object may contain
            //some characters that are escaped e.g. \" will be converted automatically to " when it is insreted
            //and the JSON cannot be restored if it is read again form the database

            String oaiid = "", uri = "", json = "";
            //Use ArrayList of bytes as we don't know the length of each field
            ArrayList<Byte> arrBytes = new ArrayList<Byte>();
            byte b;
            //Read byte by byte from the stream and place it in an ArrayList and then convert it into byte array
            while((b = (byte)fs.read()) != -1){
                try{

                    char ch = (char)b;
                    if(ch == '\n'){
                        if(oaiid.equals("")){
                            oaiid = new String(convertToByteArray(arrBytes));
                        }
                        else if(uri.equals("")){
                            uri = new String(convertToByteArray(arrBytes));
                        }
                        else{
                            json = new String(convertToByteArray(arrBytes));

                            JDBC jdbc = JDBC.getDefaultConnection();
                            String sqlStmt = "INSERT INTO " + TABLENAME + "(" + FIELD_OAIID + ", " +
                            FIELD_RESOURCE + " , " + FIELD_JSON_BLOB + " ) VALUES ( ?, ? , ?  ) ";

                            PreparedStatement stmt = jdbc.prepare(sqlStmt,"");
                            jdbc.executeStatement(stmt, new String[]{oaiid, uri, json});
                            logger.info("Record for page # " + oaiid + ", and resource = " + uri +
                                    " has been successfully inserted into database");
                            oaiid = uri = json = "";
                        }
                        arrBytes = new ArrayList<Byte>();

                    }
                    else
                        arrBytes.add(b);

                }
                catch(Exception exp){
                    logger.info("Record for page # " + oaiid + " cannot be inserted into database");
                    oaiid = uri = json = "";
                }

            }
            fs.close();
        }
        catch(Exception exp){
            logger.error("SQL file cannot be read due to " + exp.getMessage());
        }
    }

    private static byte[] convertToByteArray(ArrayList<Byte> arrBytes) {
        byte [] outputArrBytes = new byte[arrBytes.size()];
        for(int i = 0; i<arrBytes.size(); i++)
            outputArrBytes[i] = arrBytes.get(i);
        return outputArrBytes;
    }

}

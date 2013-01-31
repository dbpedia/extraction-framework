package org.dbpedia.extraction.live.storage;


import org.apache.log4j.Logger;

import java.sql.*;

/**
 * This class contains usefull funtions to deal with JDBC
 */
public class JDBCUtil {
    //Initializing the Logger
    private static Logger logger = Logger.getLogger(JDBCUtil.class);


    /*
    * Execs a SPARUL Query and returns true if everything went ok or false in case of exception
    * */
    public static boolean execSPARUL(String sparul) {
        String query = sparul;
        if (!sparul.startsWith("SPARQL"))
            query = "SPARQL " + query;

        return execSQL(query);
    }

    /*
    * Execs an SQL query and returns true if everything went ok or false  in case of exception
    * */
    public static boolean execSQL(String query) {

        Connection conn = null;
        Statement stmt = null;
        ResultSet result = null;
        try {
            conn = JDBCPoolConnection.getPoolConnection();
            stmt = conn.createStatement();
            result = stmt.executeQuery(query);

            return true;
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return false;
        } finally {
            try {
                if (result != null)
                    result.close();
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }
        }
    }

    /*
    * Execs a prepared Statement SQL query and returns true if everything went ok or false  in case of exception
    * */
    public static boolean execPrepared(String preparedQuery, String[] parameterList) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            conn = JDBCPoolConnection.getPoolConnection();
            stmt = conn.prepareStatement(preparedQuery);

            for (int i = 0; i < parameterList.length; i++) {
                stmt.setString(i + 1, parameterList[i]);
            }
            stmt.execute();

            return true;
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return false;
        } finally {
            try {
                if (result != null)
                    result.close();
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }
        }
    }

    /*
    * Custon function for retrieving Cache contents (this is application specific)
    * */
    public static JSONCacheObject getCacheContent(String query, long pageID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            conn = JDBCPoolConnection.getPoolConnection();
            stmt = conn.prepareStatement(query);

            stmt.setLong(1, pageID);

            result = stmt.executeQuery();

            StringBuilder json = new StringBuilder();

            if (result.next()) {
                int timesUpdated = result.getInt("timesUpdated");
                Blob jsonBlob = result.getBlob("json");
                byte[] jsonData = jsonBlob.getBytes(1, (int) jsonBlob.length());

                Blob subjectsBlob = result.getBlob("subjects");
                byte[] subjectsData = subjectsBlob.getBytes(1, (int) subjectsBlob.length());

                return new JSONCacheObject(pageID, timesUpdated, new String(jsonData), new String(subjectsData));
            } else {
                return null;
            }

        } catch (Exception e) {
            logger.warn(e.getMessage());
            return null;
        } finally {
            try {
                if (result != null)
                    result.close();
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
                logger.warn(e.getMessage());
            }
        }
    }


}

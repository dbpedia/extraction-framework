package org.dbpedia.extraction.live.storage;


import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

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
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            result = stmt.executeQuery(query);

            return true;
        } catch (Exception e) {

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
    * Custon function for retrieving JSON Cache contents
    * */
    public static String getJSONCacheContent(long pageID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            conn = JDBCPoolConnection.getPoolConnection();
            stmt = conn.prepareStatement(getJSONCacheSelect());

            stmt.setLong(1, pageID);

            result = stmt.executeQuery();

            StringBuilder json = new StringBuilder();

            if (result.next()) {
                return result.getBlob(1).toString();
            } else {
                return "";
            }

        } catch (Exception e) {
            logger.warn(e.getMessage());
            return "";
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


    public static String getJSONCacheSelect() {
        return "SELECT content FROM DBPEDIA_TRIPLES WHERE oaiid = ?";
    }

    public static String getJSONCacheInsert() {
        return "INSERT INTO DBPEDIA_TRIPLES (oaiid, resource, content) VALUES ( ?, ? , ?  ) ";
    }

    public static String getJSONCacheDelete() {
        return "DELETE FROM DBPEDIA_TRIPLES WHERE oaiid = ?";
    }
}

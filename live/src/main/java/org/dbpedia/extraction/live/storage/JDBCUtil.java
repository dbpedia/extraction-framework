package org.dbpedia.extraction.live.storage;


import org.slf4j.Logger;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.util.DateUtil;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class contains usefull funtions to deal with JDBC
 */
public class JDBCUtil {
    //Initializing the Logger
    private static Logger logger = LoggerFactory.getLogger(JDBCUtil.class);


    /*
    * Execs an SQL query and returns true if everything went ok or false  in case of exception
    * */
    public static boolean execSQL(String query) {

        return execSQL(query, false);
    }

    /*
    * Execs an SQL query and returns true if everything went ok or false  in case of exception
    * */

    public static boolean execSQL(String query, boolean sparql) {
        try {
            _execSQL(query,sparql);
            return true;
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }
        return false;
    }

    private static void _execSQL(String query, boolean sparql) throws Exception {

        Connection conn = null;
        Statement stmt = null;
        ResultSet result = null;
        try {
            conn = JDBCPoolConnection.getCachePoolConnection() ;
            stmt = conn.createStatement();
            result = stmt.executeQuery(query);
        } catch (Exception e) {
            logger.warn(e.getMessage());
        } finally {
            try {
                if (result != null)
                    result.close();
            } catch (Exception e) {
                throw new Exception(e.getMessage());
            }
            try {
                if (stmt != null)
                    stmt.close();
            } catch (Exception e) {
                throw new Exception(e.getMessage());
            }
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception e) {
                throw new Exception(e.getMessage());
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
            conn = JDBCPoolConnection.getCachePoolConnection();
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
    public static JSONCacheItem getCacheContent(String query, long pageID) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            conn = JDBCPoolConnection.getCachePoolConnection();
            stmt = conn.prepareStatement(query);

            stmt.setLong(1, pageID);

            result = stmt.executeQuery();

            if (result.next()) {
                int timesUpdated = result.getInt("timesUpdated");
                Blob jsonBlob = result.getBlob("json");
                byte[] jsonData = jsonBlob.getBytes(1, (int) jsonBlob.length());
                String jsonString = new String(jsonData);//.toString().getBytes("UTF8")); // convert to UTF8

                Blob subjectsBlob = result.getBlob("subjects");
                byte[] subjectsData = subjectsBlob.getBytes(1, (int) subjectsBlob.length());
                String subjects = new String(subjectsData);//.toString().getBytes("UTF8"));  // convert to UTF8
                Set<String> subjectSet = new HashSet<>();
                for (String item: subjects.split("\n")) {
                    String subject = item.trim();
                    if (!subject.isEmpty())
                        subjectSet.add(org.apache.commons.lang.StringEscapeUtils.unescapeJava(subject));
                }

                return new JSONCacheItem(pageID, timesUpdated, jsonString, subjectSet);
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

    /*
    * Custom function that returns a list with unmodifies pages from cache
    * */
    public static List<LiveQueueItem> getCacheUnmodified(int daysAgo, long limit) {
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet result = null;
        List<LiveQueueItem> items = null;
        try {
            conn = JDBCPoolConnection.getCachePoolConnection();
            stmt = conn.prepareStatement(DBpediaSQLQueries.getJSONCacheUnmodified());

            stmt.setInt(1, daysAgo);
            stmt.setLong(2, limit);

            result = stmt.executeQuery();

            items = new ArrayList<LiveQueueItem>((int)limit);

            while (result.next()) {
                long pageID = result.getLong("pageID");
                Timestamp t = result.getTimestamp("updated");
                String timestamp = DateUtil.transformToUTC(t.getTime());
                items.add(new LiveQueueItem(pageID, timestamp));
            }
            return items;
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

package org.dbpedia.extraction.live.storage;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.main.Main;

import java.sql.Connection;
import java.sql.SQLException;

public class JDBCPoolConnection {
    //Initializing the Logger
    private static Logger logger = Logger.getLogger(JDBCPoolConnection.class);

    private static volatile BoneCP connectionStorePool = null;
    private static volatile BoneCP connectionCachePool = null;

    protected JDBCPoolConnection() {
    }

    private static void initStoreConnection() {

        try {
            BoneCPConfig config = new BoneCPConfig();
            Class.forName(LiveOptions.options.get("store.class"));
            config.setJdbcUrl(LiveOptions.options.get("store.dsn"));
            config.setUsername(LiveOptions.options.get("store.user"));
            config.setPassword(LiveOptions.options.get("store.pw"));
            connectionStorePool = new BoneCP(config); // setup the connection pool
        } catch (Exception e) {
            logger.fatal(e.getMessage());
            logger.fatal("Could not initialize DB connection! Exiting...");
            Main.stopLive();
            System.exit(1);
        }
    }


    public static Connection getStorePoolConnection() throws SQLException {
        if (connectionStorePool == null) {
            synchronized (JDBCPoolConnection.class) {
                if (connectionStorePool == null) {
                    initStoreConnection();
                }
            }
        }
        return connectionStorePool.getConnection();
    }

    private static void initCacheConnection() {

        try {
            BoneCPConfig config = new BoneCPConfig();
            Class.forName(LiveOptions.options.get("cache.class"));
            config.setJdbcUrl(LiveOptions.options.get("cache.dsn"));
            config.setUsername(LiveOptions.options.get("cache.user"));
            config.setPassword(LiveOptions.options.get("cache.pw"));
            connectionCachePool = new BoneCP(config); // setup the connection pool
        } catch (Exception e) {
            logger.fatal(e.getMessage());
            logger.fatal("Could not initialize DB connection! Exiting...");
            Main.stopLive();
            System.exit(1);
        }
    }


    public static Connection getCachePoolConnection() throws SQLException {
        if (connectionCachePool == null) {
            synchronized (JDBCPoolConnection.class) {
                if (connectionCachePool == null) {
                    initCacheConnection();
                }
            }
        }
        return connectionCachePool.getConnection();
    }


    public static void shutdown() {
        if (connectionStorePool != null) {
            try {
                connectionStorePool.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }


}

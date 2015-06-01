package org.dbpedia.extraction.live.storage;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import org.slf4j.Logger;
import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.main.Main;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class JDBCPoolConnection {
    //Initializing the Logger
    private static Logger logger = LoggerFactory.getLogger(JDBCPoolConnection.class);

    private static volatile BoneCP connectionCachePool = null;

    private JDBCPoolConnection() {
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
            logger.error(e.getMessage());
            logger.error("Could not initialize DB connection! Exiting...");
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
        if (connectionCachePool != null) {
            try {
                connectionCachePool.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }


}

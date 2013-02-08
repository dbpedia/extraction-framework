package org.dbpedia.extraction.live.storage;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.core.LiveOptions;

import java.sql.Connection;
import java.sql.SQLException;

public class JDBCPoolConnection {
    //Initializing the Logger
    private static Logger logger = Logger.getLogger(JDBCPoolConnection.class);

    private static BoneCP connectionPool = null;

    private static final String URL = LiveOptions.options.get("Store.dsn");
    private static final String UNAME = LiveOptions.options.get("Store.user");
    private static final String PWD = LiveOptions.options.get("Store.pw");

    protected JDBCPoolConnection() {
    }

    private static void initConnection() {

        try {
            BoneCPConfig config = new BoneCPConfig();
            Class.forName("virtuoso.jdbc4.Driver");
            config.setJdbcUrl(URL);
            config.setUsername(UNAME);
            config.setPassword(PWD);
            config.setMinConnectionsPerPartition(2);
            config.setMaxConnectionsPerPartition(5);
            config.setPartitionCount(1);
            connectionPool = new BoneCP(config); // setup the connection pool
        } catch (Exception e) {
            logger.fatal("Could not initialize databse connection! Exiting...");
            System.exit(1);
        }
    }


    public static Connection getPoolConnection() throws SQLException {
        if (connectionPool == null) {
            synchronized (JDBCPoolConnection.class) {
                if (connectionPool == null) {
                    initConnection();
                }
            }
        }
        return connectionPool.getConnection();
    }

    public static void shutdown() {
        if (connectionPool != null) {
            try {
                connectionPool.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }


}

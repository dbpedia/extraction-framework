package org.dbpedia.extraction.live.storage;

import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.core.Timer;
import org.dbpedia.extraction.live.core.XMLConverter;

import java.sql.*;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jun 24, 2010
 * Time: 1:01:10 PM
 * This class provides support for reading JDBC information, this information is required for live extraction
 */
public class JDBC {

    //Initializing the Logger
    private Logger logger = Logger.getLogger(JDBC.class);

    String dsn;
    String user;
    String pw;
    static Connection con = null;
    final int wait = 5;

    public JDBC(String DSN, String USER, String Password) {
        this.dsn = DSN;
        this.user = USER;
        this.pw = Password;

        if (con == null)
            this.connect();

        //Assert.assertNotNull("Connection cannot be null", con);
    }

    public static JDBC getDefaultConnection() {
        String dataSourceName = LiveOptions.options.get("Store.dsn");
        String username = LiveOptions.options.get("Store.user");
        String password = LiveOptions.options.get("Store.pw");

        return new JDBC(dataSourceName, username, password);
    }

    /*
    * Blocks until connection exists
    *
    * */
    public void connect(boolean debug) {
        try {
            //Make sure that the JDBC driver for virtuoso exists
            Class.forName("virtuoso.jdbc4.Driver");

            Connection Conn = DriverManager.getConnection(this.dsn, this.user, this.pw);
            while (Conn == null) {
                logger.warn("JDBC connection to " + this.dsn + " failed, waiting for "
                        + wait + " and retrying");
                Thread.sleep(wait);

                //Retrying to connect to the database
//                Conn = DriverManager.getConnection("jdbc:virtuoso://localhost:1111/UID=dba/PWD=dba");
                Conn = DriverManager.getConnection(this.dsn, this.user, this.pw);
            }
            if (debug) {
                logger.info("JDBC connection re-established");
            }
            con = Conn;
        } catch (ClassNotFoundException exp) {
            logger.fatal("JDBC driver of Virtuoso cannot be loaded");
            System.exit(1);
        } catch (Exception exp) {
            logger.warn(exp.getMessage() + " Function connect ");
        }
    }

    public void connect() {
        connect(false);
    }

    /**
     * Prepares a sql/sparql statement
     *
     * @param query        The required query
     * @param logComponent The component used in logging
     * @return A prepared statement object
     */
    public PreparedStatement prepare(String query, String logComponent) {
        try {

            //If the application is working in multithreading mode, we must attach the thread id to the timer name
            //to avoid the case that a thread stops the timer of another thread.
            String timerName = logComponent + "::JDBC_prepare";

            Timer.start(timerName);
            PreparedStatement result = con.prepareStatement(query);

            Timer.stop(timerName);
            return result;
        } catch (Exception exp) {

            return null;
        }

    }

    public boolean executeStatement(PreparedStatement sqlStatement, String[] parameterList) {
        boolean successfulExecution = false;
        try {
            if ((con == null) || (con.isClosed()))
                con = DriverManager.getConnection(this.dsn, this.user, this.pw);

            //Prepare the statement that will receive the parameters
            //PreparedStatement preparedSQL = this.con.prepareStatement(sqlStatement);

            for (int i = 0; i < parameterList.length; i++) {
                sqlStatement.setString(i + 1, parameterList[i]);
            }
            sqlStatement.execute();
            successfulExecution = true;
            sqlStatement.close();
        } catch (Exception exp) {
            logger.warn(exp.getMessage() + " Function executeStatement ");
            successfulExecution = false;
            //Try to reconnect, as the number of allowed statements may be exceeded
            reconnect();
        }

        return successfulExecution;
    }

    //This function executes the passed query
    public ResultSet exec(String query, String logComponent) {
        ResultSet result = null;
        try {
            if ((con == null) || (con.isClosed()))
                con = DriverManager.getConnection(this.dsn, this.user, this.pw);

            //If the application is working in multithreading mode, we must attach the thread id to the timer name
            //to avoid the case that a thread stops the timer of another thread.
            String timerName = logComponent + "::exec query";

            Timer.start(timerName);
            Statement requiredStatement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            result = requiredStatement.executeQuery(query);

            Timer.stop(timerName);
            logger.info(logComponent + "::SUCCESS ( " + result + " ): ");
        } catch (Exception exp) {
            logger.warn(exp.getMessage() + " Function executeStatement ");
            //Try to reconnect, as the number of allowed statements may be exceeded
            reconnect();
        }

        return result;
    }

    private String _adjustEscapeSequence(String str) {
        return "";
    }


    public HashMap execAsJson(String query, String logComponent) {
        try {
            query = "sparql define output:format \" RDF/XML\" " + query;

            ResultSet resultSet = this.exec(query, logComponent);

            String data = "";
            while (resultSet.next()) {
                String temp = resultSet.getString(0);
                if (temp != null)
                    data += temp;
            }

            XMLConverter conv = new XMLConverter();
            HashMap arr = conv.toArray(data);

            return arr;
        } catch (Exception exp) {
            this.logger.warn(exp.getMessage());
            return null;
        }
    }

    /*
       * returns the JDBC callable statement, which is useful in case of calling stored procedures.
       * @Param requiredStatement    The statement containing the call of the stored procedure
       * */
    public CallableStatement prepareCallableStatement(String requiredStatement, String logComponent) {
        try {

            String timerName = logComponent + "::JDBC_prepare";

            Timer.start(timerName);
            CallableStatement result = con.prepareCall(requiredStatement);

            Timer.stop(timerName);
            return result;
        } catch (Exception exp) {

            return null;
        }

    }

    /**
     * Executes a callable statement which can be used in case of calling stored procedures
     *
     * @param requiredStatement The statement to be executed
     * @param parameterList     The list of parameters that should be passed to the procedure
     * @return True if the execution was successful, and false otherwise.
     */
    public boolean executeCallableStatement(CallableStatement requiredStatement, String[] parameterList) {
        boolean successfulExecution = false;
        try {
            if ((con == null) || (con.isClosed()))
                con = DriverManager.getConnection(this.dsn, this.user, this.pw);

            for (int i = 0; i < parameterList.length; i++) {
                requiredStatement.setString(i + 1, parameterList[i]);
            }
            requiredStatement.execute();
            successfulExecution = true;
            requiredStatement.close();
        } catch (Exception exp) {
            logger.warn(exp.getMessage() + " Function executeStatement ");
            successfulExecution = false;
        }

        return successfulExecution;
    }

    /**
     * This function closes the connection in order to release all resources it holds, and creates a new connection
     */
    public void reconnect() {
        try {
            con.close();
            con = null;
            connect();
        } catch (SQLException exp) {
            logger.warn("Connection cannot be closed Function reconnect");
        }

    }


}

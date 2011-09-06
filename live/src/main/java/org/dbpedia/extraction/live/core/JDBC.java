package org.dbpedia.extraction.live.core;

import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.extraction.LiveExtractionConfigLoader;

import java.sql.*;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jun 24, 2010
 * Time: 1:01:10 PM
 * This class provides support for reading JDBC information, this information is required for live extraction
 */
public class JDBC{
    
    public static final int JDBC_MAX_LONGREAD_LENGTH = 8000;
    //Initializing the Logger
    private Logger logger = Logger.getLogger(this.getClass().getName());

    String dsn;
    String user;
    String pw;
    static Connection con = null;
    String previous;
    final int wait = 5;
    final int cutstring = 1000;

    public JDBC(String DSN, String USER, String Password)
    {
        this.dsn = DSN;
        this.user = USER;
        this.pw = Password;

        //Assert.assertTrue("DSN cannot be null or empty", (dsn != null && dsn != ""));
        //Assert.assertTrue("Username cannot be null or empty", (user != null && user != ""));
        //Assert.assertTrue("Password cannot be null or empty", (pw != null && pw != ""));

        if(con == null)
            this.connect();

        //Assert.assertNotNull("Connection cannot be null", con);
    }

    public static JDBC getDefaultConnection()
    {
        String dataSourceName = LiveOptions.options.get("Store.dsn");
        String username = LiveOptions.options.get("Store.user");
        String password = LiveOptions.options.get("Store.pw");

        return new JDBC(dataSourceName, username, password);
    }

    /*
    * Blocks until connection exists
    *
    * */
    public void connect(boolean debug)
    {
        try{
            //Make sure that the JDBC driver for virtuoso exists
            Class.forName("virtuoso.jdbc4.Driver");

            //odbc_close_all();//TODO all old JDBC connections must be closed here

            boolean FailedOnce = false;

            //Connection Conn = DriverManager.getConnection("jdbc:virtuoso://localhost:1111/UID=dba/PWD=dba");
            //Assert.assertTrue("DSN cannot be null or empty", (dsn != null && dsn != ""));
            //Assert.assertTrue("Username cannot be null or empty", (user != null && user != ""));
            //Assert.assertTrue("Password cannot be null or empty", (pw != null && pw != ""));
            
            Connection Conn = DriverManager.getConnection(this.dsn, this.user, this.pw);
            //logger.log(Level.INFO, "Connection to Virtuoso has been established");
            while(Conn == null){
                logger.warn("JDBC connection to " + this.dsn + " failed, waiting for "
                                                    + wait + " and retrying");
                Thread.sleep(wait);

                //Retrying to connect to the database
//                Conn = DriverManager.getConnection("jdbc:virtuoso://localhost:1111/UID=dba/PWD=dba");
                Conn = DriverManager.getConnection(this.dsn, this.user, this.pw);
            }
            if(debug)
            {
                logger.info("JDBC connection re-established");
            }
            con = Conn;
        }
        catch(ClassNotFoundException exp)
        {
           logger.fatal("JDBC driver of Virtuoso cannot be loaded");
           System.exit(1);
        }
        catch(Exception exp)
        {
             logger.warn(exp.getMessage() + " Function connect ");
        }
    }

    public void connect(){
        connect(false);
    }

     /*
	 * returns the jdbc statement
	 * */
    //TODO this function must be modified to reflect the prepare process 
	public PreparedStatement prepare(String query, String logComponent)
    {
        try{

            //If the application is working in multithreading mode, we must attach the thread id to the timer name
            //to avoid the case that a thread stops the timer of another thread.
            String timerName = logComponent + "::JDBC_prepare" +
                (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

            //Assert.assertTrue("Query cannot be null or empty", (query != null && query != ""));

            Timer.start(timerName);
    	 	PreparedStatement result = con.prepareStatement(query);

            //Assert.assertNotNull("Result of prepare statement cannot be null", result);
	     	//Timer.stop(logComponent + "::JDBC_prepare");
            Timer.stop(timerName);
            return result;
        }
        catch(Exception exp){

            return null;
        }

//		if (false === $result) {
//			$errornr = odbc_error($this->con);
//			$err = odbc_errormsg($this->con);
//			//echo $err.$errornr;die;
//			switch ($errornr){
//				case 37000: {
//					Logger::warn($logComponent."::odbc_prepare failed: query length = ".strlen($query)."\n".$err);
//					break;
//					}
//				case '08S01': {
//					Logger::warn($logComponent."::odbc_prepare: lost connection to server going into loop".$err);
//					Logger::warn("Waiting at: ***********\n".substr($query, 0, self::cutstring));
//					Logger::warn("Previous was:**********\n".$this->previous);
//					do{
//						Logger::warn('Currently looping last odbc_prepare, waiting '.self::wait.' and retrying');
//						sleep(self::wait);
//						$this->connect(true);
//						$result = @odbc_prepare($this->con, $query);
//						$errornr = odbc_error($this->con);
//					}while (false === $result && $errornr == '08S01' );
//					break;
//				}
//                case 40001: {}
//				case 'SR172': {
//					Logger::warn($logComponent."::odbc_prepare: Transaction deadlocked, going into loop".$err);
//					Logger::warn("Waiting at: ***********\n".substr($query, 0, self::cutstring));
//					Logger::warn("Previous was:**********\n".$this->previous);
//					do{
//						Logger::warn('Currently looping last odbc_prepare, waiting '.self::wait.' and retrying');
//						sleep(self::wait);
//						$this->connect(true);
//						$result = @odbc_prepare($this->con, $query);
//						$errornr = odbc_error($this->con);
//					}while (false === $result && $errornr == 'SR172' );
//					break;
//				}
//				default: {
//					Logger::warn($logComponent."::odbc_prepare failed: \n".$query."\nnumber: ".$errornr."\nerror: ".$err);
//					}
//
//				}
//
//		}
//        else
//        {
//			Logger::debug( $logComponent.":: successfully prepared  ($query): ");
//		}
//	 $this->setPrevious( $query);
//	 return $result;
        //return "";
	}

    public boolean executeStatement(PreparedStatement sqlStatement, String[] parameterList)
    {
        //Assert.assertNotNull("Statement cannot be null", sqlStatement);
        boolean successfulExecution = false;
        try{
            if((con == null) || (con.isClosed()))
                con = DriverManager.getConnection(this.dsn, this.user, this.pw);

            //Assert.assertNotNull("Connection cannot be null", con);
            /*else{
                con.close();
                con = DriverManager.getConnection("jdbc:virtuoso://localhost:1111/UID=dba/PWD=dba");
            }*/

            //Prepare the statement that will receive the parameters
            //PreparedStatement preparedSQL = this.con.prepareStatement(sqlStatement);

            for(int i=0;i<parameterList.length; i++)
            {
                sqlStatement.setString(i+1, parameterList[i]);
            }
            sqlStatement.execute();
            successfulExecution = true;
            sqlStatement.close();
            //con.close();
        }
        catch(Exception exp){
            logger.warn(exp.getMessage() + " Function executeStatement ");
            successfulExecution = false;
        }
        
        return successfulExecution;
    }

	/*public function setPrevious($s){
		$this->previous = substr($s,0,self::cutstring);
		}*/

    //This function executes the passed query
    public ResultSet exec(String query, String logComponent)//TODO what is logComponent
    {
        //Assert.assertTrue("Query cannot be null or empty", (query != null && query != ""));
        ResultSet result = null;
        try
        {
            if((con == null) || (con.isClosed()))
                con = DriverManager.getConnection(this.dsn, this.user, this.pw);

            //Assert.assertNotNull("Connection cannot be null", con);
            /*else{
                con.close();
                con = DriverManager.getConnection("jdbc:virtuoso://localhost:1111/UID=dba/PWD=dba");
            }*/

            //If the application is working in multithreading mode, we must attach the thread id to the timer name
            //to avoid the case that a thread stops the timer of another thread.
            String timerName = logComponent + "::exec query" +
                (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

            Timer.start(timerName);
            Statement requiredStatement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            //Assert.assertNotNull("requiredStatement cannot be successfully created", requiredStatement);

            result = requiredStatement.executeQuery(query);
            //Timer.stop(logComponent + "::exec query");
            Timer.stop(timerName);
            logger.info(logComponent + "::SUCCESS ( "+ result +" ): ");
            

//            System.out.println("--------------------------------------------------------------------------------");
//            System.out.println("QUERY = " + query);
//            System.out.println("--------------------------------------------------------------------------------");

            //con.close();
        }
        catch(Exception exp)
        {
//            System.out.println("--------------------------------------------------------------------------------");
//            System.out.println("QUERY = " + query);
//            System.out.println("--------------------------------------------------------------------------------");
            logger.warn(exp.getMessage() + " Function executeStatement ");
        }


		/*$result = @odbc_exec($this->con, $query);
		Timer::stop($logComponent.'::odbc_exec');

		if (false === $result) {
			$errornr = odbc_error($this->con);
			$err = odbc_errormsg($this->con);
			switch ($errornr){
				case 37000: {
					Logger::warn($logComponent."::odbc_exec failed: query length = ".strlen($query)."\n".$err);
					Logger::warn($query);
					break;
				}
				//lost connection to server
				case '08S01': {
					Logger::warn($logComponent."::odbc_exec: lost connection to server, going into loop".$err);
					Logger::warn("Waiting at: ***********\n".substr($query, 0, self::cutstring));
					Logger::warn("Previous was:**********\n".$this->previous);
					do{

						sleep(self::wait);
						$this->connect(true);
						$result = @odbc_exec($this->con, $query);
						$errornr = odbc_error($this->con);
						Logger::warn('Currently looping last odbc_exec, waiting '.self::wait.' and retrying '.$errornr.$result);
					}while (false === $result && $errornr == '08S01' );
					break;
				}
                case 40001: {}
				case 'SR172': {
					Logger::warn($logComponent."::odbc_exec: Transaction deadlocked, going into loop".$err);
					Logger::warn("Waiting at: ***********\n".substr($query, 0, self::cutstring));
					Logger::warn("Previous was:**********\n".$this->previous);
					do{

						sleep(self::wait);
						$this->connect(true);
						$result = @odbc_exec($this->con, $query);
						$errornr = odbc_error($this->con);
						Logger::warn('Currently looping last odbc_exec, waiting '.self::wait.' and retrying '.$errornr.$result);
					}while (false === $result && ($errornr == 'SR172' || $errornr == 40001  ));
					break;
				}
				//query on a non existant db table
				case '42S02': {}
				case 'S0002': {
					//do nothing returning false is ok
					Logger::info('no db table found'."\n". $err);
					break;
					}

				default: {
					Logger::warn($logComponent."::odbc_exec failed: \n".$query."\nnumber: ".$errornr."\nerror: ".$err);
					}

				}

		}else{
			Logger::info( $logComponent."::SUCCESS ($result): ");
		}
		$this->setPrevious( $query);
		return $result;*/
        return result;
	}

    private String _adjustEscapeSequence (String str){
        return "";
    }

    /*public ResultSet exec(String query){
        return exec(query, "");
    }*/


    public HashMap execAsJson(String query, String logComponent)
    {
        try{
            //Assert.assertTrue("Query cannot be null or empty", (query != null && query != ""));
            query = "sparql define output:format \" RDF/XML\" "  + query;

            ResultSet resultSet = this.exec(query, logComponent);

            //TODO this part of the code must be converted

            String data = "";
            while(resultSet.next())
            {
                String temp = resultSet.getString(0);
                if(temp!=null)
                    data += temp;
            }

            XMLConverter conv = new XMLConverter();
            HashMap arr = conv.toArray(data);

            return arr;
        }
        catch (Exception exp){
            this.logger.warn(exp.getMessage());
            return null;
        }
    }


}

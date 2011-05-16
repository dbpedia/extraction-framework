package org.dbpedia.extraction.live.core;

import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.extraction.LiveExtractionConfigLoader;
import org.json.simple.JSONValue;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;

import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jun 24, 2010
 * Time: 12:57:57 PM
 * This class holds the hashing capabilities that are used for storing and retrieving triples from the database table   
 */
public class Hash{
    
    //Initializing the Logger
    private static Logger logger = null;

    //Constant initialization
    private final int JDBC_MAX_LONGREAD_LENGTH = 8000;
    private static final String TABLENAME = "dbpedia_triples";
    private static final String FIELD_OAIID = "oaiid";
    private static final String FIELD_RESOURCE = "resource";
    private static final String FIELD_JSON_BLOB = "content";

    public String oaiId;

    private static JDBC jdbc;
    private HashMap hashesFromStore=null;
    private String hashesFromExtractor = null;
    private boolean hasHash = false;
    private boolean active = false;

    public HashMap newJSONObject = new HashMap();
    private HashMap originalJSONObject ;
    //normal rdftriples
    private HashMap addTriples = new HashMap();
    //special array with sparulpattern
    private HashMap deleteTriples = new HashMap();

    private String Subject = null; //TODO This member is not explicitly typed in the php file, so we must make sure of its type

    static
    {
        try
        {
            logger = Logger.getLogger(Class.forName("org.dbpedia.extraction.live.core.Hash").getName());
        }
        catch (Exception exp){

        }
    }

    public static void initDB()
    {
        initDB(false);
    }

    public static void initDB(boolean clearHashtable )
    {
        if(LiveOptions.options.get("LiveUpdateDestination.useHashForOptimization")!=null)
        {
            jdbc = JDBC.getDefaultConnection();
            if(clearHashtable)
            {
                String sqlStatment = "DROP TABLE " + TABLENAME;
                ResultSet result = jdbc.exec(sqlStatment, "Hash::initDB");
                if(result != null)
                {
                    logger.info("dropped hashtable");
                }
            }
            //test if table exists
            String testSQLStatment = "SELECT TOP 1 * FROM " + TABLENAME;

            ResultSet result = jdbc.exec(testSQLStatment, "Hash::initDB");

            if(result == null)
            {
                String SQLStatement = "CREATE TABLE " + TABLENAME + " ( " + FIELD_OAIID + "INTEGER NOT NULL PRIMARY KEY, "
                    + FIELD_RESOURCE+ " VARCHAR(510)," + FIELD_JSON_BLOB + " LONG VARCHAR ) ";

                ResultSet ResultMake = jdbc.exec(SQLStatement, "Hash::initDB");
                ResultSet ResultSecondTest = jdbc.exec(testSQLStatment, "Hash::initDB");

                if(ResultMake == null || ResultSecondTest ==null)
                {
                    System.out.println("could not create table " + TABLENAME );
                    System.exit(1);
                }
                else
                {
                    logger.info("created table " + TABLENAME);
                }
            }
            else
            {
                logger.info("Hash:: table " + TABLENAME + " found");
            }
        }
    }

    public Hash(String oaiId, String subject)
    {
        this.Subject = subject;

        this.oaiId = oaiId;
        //logger.log(INFO, "_construct: " + this.oaiId + " " + this.Subject);

        if(Boolean.parseBoolean(LiveOptions.options.get("LiveUpdateDestination.useHashForOptimization")))
        {
            this.jdbc = JDBC.getDefaultConnection();
            this.hasHash = this._retrieveHashValues();
            this.active = true;
        }
    }

    private boolean _retrieveHashValues()
    {
        try
        {
            String sqlStatement = "Select " + FIELD_RESOURCE + ", " + FIELD_JSON_BLOB + " From " + TABLENAME +" Where "
                    + FIELD_OAIID +" = " + this.oaiId;

            ResultSet jdbcResult= this.jdbc.exec(sqlStatement, "Hash._retrieveHashValues");

            
            //Calculate the number of rows returned by the query
            //jdbcResult.last();
            //int NumberOfRows = jdbcResult.getRow();
            int NumberOfRows = 0;

            while(jdbcResult.next())
                NumberOfRows++;

            if(NumberOfRows <= 0)
            {
				logger.info(this.Subject + " : no hash found");
				return false;
			}

            jdbcResult.beforeFirst();
            String Temp = "";

            //TODO In the original code there is a variable called $data that is initialized to false and is concatenated
            //TODO with Temp
            
            while(jdbcResult.next())
            {
                Blob blob = jdbcResult.getBlob(2);
                byte[] bdata = blob.getBytes(1, (int) blob.length());
                Temp += new String(bdata);

                //Temp += jdbcResult.getBlob(2);
            }

            jdbcResult.close();
            
            //This class is object is created to force the JSON decoder to return a HashMap, as hashesFromStore is a HashMap 
            ContainerFactory containerFactory = new ContainerFactory(){
                public List creatArrayContainer() {
                  return new LinkedList();
                }

                public Map createObjectContainer() {
                  return new HashMap();
                }

              };


            JSONParser parser = new JSONParser();
            parser.parse(Temp);
            hashesFromStore = (HashMap) parser.parse(Temp, containerFactory);
            //this.hashesFromStore = json_decode(Temp, true);

            if(this.hashesFromStore == null)
            {
                logger.warn("conversion to JSON failed, not using hash this time");
                logger.warn(Temp);
                return false;
            }
            logger.info(this.Subject + " retrieved hashes from " + this.hashesFromStore + " extractors ");
            return true;
        }
        catch(Exception exp)
        {
            logger.warn(exp.getMessage());
            return false;
        }
    }

    public void updateDB()
    {
		if(this.active == false)
        {
			return;
		}

        //If the application is working in multithreading mode, we must attach the thread id to the timer name
        //to avoid the case that a thread stops the timer of another thread.
        String timerName = "Hash::updateDB" +
                (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

		Timer.start(timerName);

        String jsonEncodedString = JSONValue.toJSONString(this.newJSONObject);

		String sql = "Update " + TABLENAME + " Set " + FIELD_RESOURCE +" = ? , " + FIELD_JSON_BLOB + " = ? Where "
                + FIELD_OAIID + " = " + this.oaiId;

        PreparedStatement stmt = this.jdbc.prepare(sql , "Hash::updateDB");

        boolean updateExecutedSuccessfully= jdbc.executeStatement(stmt, new String[]{this.Subject, jsonEncodedString} );

	    String needed = Timer.stopAsString(timerName);
        if(updateExecutedSuccessfully == false)
        {
            logger.fatal("FAILED to update hashes " + sql);
        }
        else
        {
            logger.info(this.Subject + " updated hashes for " + this.newJSONObject.size() + " extractors " + needed);
        }
	}


	public void insertIntoDB()
    {
        if(!this.active)
        {
            return;
        }

        //If the application is working in multithreading mode, we must attach the thread id to the timer name
        //to avoid the case that a thread stops the timer of another thread.
        String timerName = "Hash::insertIntoDB" +
                (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

        Timer.start(timerName);
        
        String jsonEncodedString = JSONValue.toJSONString(this.newJSONObject);
        String sql = "Insert Into " + TABLENAME + "(" + FIELD_OAIID + ", " +
                FIELD_RESOURCE + " , " + FIELD_JSON_BLOB + " ) VALUES ( ?, ? , ?  ) ";

        PreparedStatement stmt = this.jdbc.prepare(sql , "Hash::insertIntoDB");

        boolean insertExecutedSuccessfully= jdbc.executeStatement(stmt, new String[]{this.oaiId, this.Subject,
                jsonEncodedString} );

	    String needed = Timer.stopAsString(timerName);

        if(insertExecutedSuccessfully == false)
        {
            logger.fatal("FAILED to insert hashes for " + this.newJSONObject.size() + " extractors ");
        }
        else
        {
            logger.info(this.Subject + " inserted hashes for " + this.newJSONObject.size() + " extractors " + needed);
        }
	}

    private void _compareHelper(String extractorID, ArrayList triples)
    {

        int matchCount = 0;
        int addCount = 0;
        int delCount = 0 ;
        int total = 0;

        //init

        if((triples != null) && (triples.size()!=0))
        {
            //$this->newJSONObject[$extractorID] = array();
            //TODO this items must be an array
            this.newJSONObject.put(extractorID, new HashMap());
        }

        ArrayList <String>keys = new ArrayList<String>();
       //TODO newHashSet must be an array
        HashMap newHashSet = new HashMap();
        for(int i=0; i<triples.size(); i++)
        {
            HashMap tmp = new HashMap();

            tmp.put("s", Util.convertToSPARULPattern(((RDFTriple)triples.get(i)).getSubject()));
            tmp.put("p",  Util.convertToSPARULPattern(((RDFTriple)triples.get(i)).getPredicate()));
            tmp.put("o",  Util.convertToSPARULPattern(((RDFTriple)triples.get(i)).getObject()));
            tmp.put("triple", triples.get(i));

            //using keys guarantees set properties
            keys.add(((RDFTriple)triples.get(i)).getMD5HashCode());
            newHashSet.put(((RDFTriple)triples.get(i)).getMD5HashCode(), tmp);
        }

//        newHashSet.remove(keys.get(20));
//        newHashSet.remove(keys.get(21));
//        newHashSet.remove(keys.get(22));
//        newHashSet.remove(keys.get(23));
//        newHashSet.remove(keys.get(24));
//        newHashSet.remove(keys.get(25));
//        newHashSet.remove(keys.get(26));
//        newHashSet.remove(keys.get(27));
//        newHashSet.remove(keys.get(28));
//        newHashSet.remove(keys.get(29));
//        newHashSet.remove(keys.get(30));

        Iterator tripleHashsetIterator = newHashSet.entrySet().iterator();

        while(tripleHashsetIterator.hasNext())
        {
            total++;
            Map.Entry pairs = (Map.Entry)tripleHashsetIterator.next();
            String hash = pairs.getKey().toString();

            //triple exists
            if((this.hashesFromStore.get(extractorID) != null) &&(
                    ((HashMap)this.hashesFromStore.get(extractorID)).get(hash)) != null)
            {

//                this.newJSONObject.put(extractorID, hash);
//                HashMap sourceHashMap = new HashMap();
//                sourceHashMap.put(hash, ((HashMap)this.hashesFromStore.get(extractorID)).get(hash));
//
//                HashMap destinationHashMap =  (HashMap)this.newJSONObject.get(extractorID);
//                destinationHashMap.put(hash, sourceHashMap);
//
//                this.hashesFromStore.remove(extractorID);

                //This is the first triple to be added to the list of triples that should be removed from the store,
                //so we must construct the Hashmap
                HashMap tmpHashmap = null;
                if(this.newJSONObject.get(extractorID) == null){
                    tmpHashmap = new HashMap();
                }
                else//Hashmap already exists, so we can use it directly 
                    tmpHashmap = (HashMap)this.newJSONObject.get(extractorID);

                tmpHashmap.put(hash, ((HashMap)this.hashesFromStore.get(extractorID)).get(hash));

                this.newJSONObject.put(extractorID, tmpHashmap);

                ((HashMap)this.hashesFromStore.get(extractorID)).remove(hash);

                matchCount++;
            }
            else
            {
                //add it
                //$this->addTriples[$hash] = $tripleArray['triple'];
                //Intialize the hashmap if it is already null
                if(addTriples == null)
                    addTriples = new HashMap();

                this.addTriples.put(hash, ((HashMap)pairs.getValue()).get("triple"));

                //unset($tripleArray['triple']);
                ((HashMap)pairs.getValue()).remove("triple");

                //$this->newJSONObject[$extractorID][$hash] = $tripleArray;
                HashMap destinationHashMap = (HashMap)this.newJSONObject.get(extractorID);
                destinationHashMap.put(hash, pairs.getValue());

                addCount++;
            }
         }

    //  foreach($this->hashesFromStore[$extractorID] as $hash => $triple)
        HashMap hmHashAndTriples = (HashMap)hashesFromStore.get(extractorID);
        Iterator hashesForIterator = hmHashAndTriples.entrySet().iterator();
        while(hashesForIterator.hasNext())
        {
            Map.Entry pairs = (Map.Entry)hashesForIterator.next();

            //initialize the hashmap if it is already null 
            if(deleteTriples == null)
                deleteTriples = new HashMap();

            //In case of Abstract extraction we should convert BLOB into string in order to decode non-English characters
            //as they are stored as a sequence of unicode escaped characters e.g. \u664B must be converted into 晋, in order
            //for the triple to found and renewed with the new triple value.
            if(extractorID.toLowerCase().contains("abstractextractor")){
                String strUnicodeDecoded = (String)((HashMap) (pairs.getValue())).get("o");
                ((HashMap) (pairs.getValue())).put("o", strUnicodeDecoded);
            }

            this.deleteTriples.put(pairs.getKey(), pairs.getValue());
            delCount++;
        }

        int percent = (total==0)?0:Math.round((matchCount/total)*100);
        logger.info( "Extractor ID = "+ extractorID + ", stats for diff (match, add, del, total): " +
                "("+ matchCount +", " + addCount + ", " + delCount + ", " + total + "[" + percent + "%] )");

    }

/*
     * returns the diff of the extractionresult with the db
 * used for collecting triples and compare them to the hash
 * name: compare
 * @param $extractionResult
 * @return
 */
    public void compare(ExtractionResult  extractionResult)
    {
        if(!this.active)
        {
            return;
        }

        //If the application is working in multithreading mode, we must attach the thread id to the timer name
        //to avoid the case that a thread stops the timer of another thread.
        String timerName = "Hash.compare" +
                (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");
        Timer.start(timerName);
        
        String extractorID = extractionResult.getExtractorID();
        ArrayList triples = extractionResult.getTriples();
        String nicename = extractorID.replace(Constants.DB_META_NS, "");

        //first of all some exceptions, e.g. MetaInformationextractor
        if(!this.hasHash)
        { //no saved hash, add to Hash
            //add
            this._addToJSON(extractorID, triples);

        }
        else if(this._isExtractorInHash(extractorID))
        {
            //do the normal thing
            this._compareHelper(extractorID, triples);
        }
        else
        {
            //add
            this._addToJSON(extractorID, triples);
        }

        Timer.stop(timerName);
    }

    public HashMap getTriplesToAdd()
    {
        logger.info("removing " + this.deleteTriples.size() + " previous triples ");
        return this.addTriples;
	}
	public HashMap getTriplesToDelete()
    {
        logger.info("adding " + this.addTriples.size() + " triples ");
        return this.deleteTriples;
	}


	private boolean _isExtractorInHash(String extractorID)
    {
		return ((this.hashesFromStore != null)&&(this.hashesFromStore.get(extractorID) != null));
	}

/*
 * name: _addToJSON
 * @param
 * @return
 */
    private void _addToJSON(String extractorID, ArrayList triples)
    {
        //If the application is working in multithreading mode, we must attach the thread id to the timer name
        //to avoid the case that a thread stops the timer of another thread.
        String timerName = "Hash.compare._addToJSON" +
                (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");
        Timer.start(timerName);

        //Timer.start("Hash.compare._addToJSON");
        if((triples != null) && (triples.size() > 0))
        {
            HashMap newTriplesToBeAdded = this._generateHashSet(triples);



//            addTriples.put()
            this.newJSONObject.put(extractorID, newTriplesToBeAdded);
            String nicename = extractorID.replace(Constants.DB_META_NS, "");

            //TODO array_keys($this->newJSONObject[$extractorID]) should be mapped to java, but I think I converted it
            //TODO correctly
            //$this->log(DEBUG, ' added : '.count(array_keys($this->newJSONObject[$extractorID])).' of '. count($triples).' triples to JSON object for '.$nicename. '[removed duplicates]' );

            HashMap hmTemp = (HashMap)this.newJSONObject.get(extractorID);
            logger.info(" added : " + hmTemp.size() + " of " +
                    triples.size() + " triples to JSON object for " + nicename +  "[removed duplicates]" );
        }
        Timer.stop(timerName);
    }

    private HashMap _generateHashSet(ArrayList triples)
    {
        if((triples == null) || (triples.size() == 0))
            return new HashMap();

        //If the application is working in multithreading mode, we must attach the thread id to the timer name
        //to avoid the case that a thread stops the timer of another thread.
        String timerName = "Hash.compare._generateHashSetFromTriples" +
                (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");
        Timer.start(timerName);

        //Timer.start("Hash.compare._generateHashSetFromTriples");

        HashMap newHashSet = new HashMap();
        for (Object objTriple : triples)
        {
            RDFTriple triple = (RDFTriple)objTriple;

            HashMap tmp = new HashMap();
            //TODO we should use the function toSPARULPattern with getSubject, getPredicate, and getObject
            tmp.put("s", Util.convertToSPARULPattern(triple.getSubject()));
            tmp.put("p", Util.convertToSPARULPattern(triple.getPredicate()));
            tmp.put("o", Util.convertToSPARULPattern(triple.getObject()));

            String hashValue = triple.getMD5HashCode();

            //We should add the triple also to the addTriples map in order to ensure that it is also inserted to the store   
            addTriples.put(hashValue, triple);

            //using keys guarantees set properties
            newHashSet.put(hashValue, tmp);
        }
        Timer.stop(timerName);
        return newHashSet;

        //add the generated triples to the list of triples that should be added to the store
//            Iterator tripleHashsetIterator = newTriplesToBeAdded.entrySet().iterator();
//            while(tripleHashsetIterator.hasNext()){
//                Map.Entry pairs = (Map.Entry)tripleHashsetIterator.next();
//                String hash = pairs.getKey().toString();
//                addTriples.put(hash, pairs.getValue());
//            }
    }


    public boolean hasHash()
    {
        return this.hasHash;
    }

    //TODO this method depends on class logger, and we may or may not need it, as we have a class fro logging in java. 
    /*private boolean log(Level lvl, String message)
    {
        logger.logp(lvl, this.getClass().getName(), "core", message);    
        Logger::logComponent('core', get_class(this), lvl , message);
    }*/

    /**
     * Returns the previously generated triples for an extractor
     * @param extractorID   The ID of the extractor
     * @return  A map containing the triples, or null if no such triples exist
     */
    public HashMap getTriplesForExtractor(String extractorID){

        if((this._isExtractorInHash(extractorID)) && (this.hashesFromStore.get(extractorID) != null)){
            return (HashMap)this.hashesFromStore.get(extractorID);
        }
        return null;
    }

    public void updateJSONObjectForExtractor(String extractorID){
        try{
            if(hashesFromStore == null)//No previous hashes for the current page exist, i.e. no further processing required
                return;
            HashMap mapHashesInStore = (HashMap)hashesFromStore.get(extractorID);
            this.newJSONObject.put(extractorID, mapHashesInStore);
        }
        catch (Exception exp){
            logger.warn("Hashes for extractor " + extractorID + " cannot be fetched");
        }
    }

}
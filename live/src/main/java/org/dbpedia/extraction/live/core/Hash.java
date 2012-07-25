package org.dbpedia.extraction.live.core;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.delta.Delta;
import org.dbpedia.extraction.live.delta.DeltaCalculator;
import org.dbpedia.extraction.live.extraction.LiveExtractionConfigLoader;
import org.dbpedia.extraction.live.main.Main;
import org.dbpedia.extraction.live.statistics.RecentlyUpdatedInstance;
import org.json.simple.JSONValue;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;

import java.io.StringReader;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;


/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jun 24, 2010
 * Time: 12:57:57 PM
 * This class holds the hashing capabilities that are used for storing and retrieving triples from the database table   
 */
public class Hash{

    public static enum TriplesType
    {
        PreviousTriples, LatestTriples
    }

    //Initializing the Logger
    private static Logger logger = Logger.getLogger(Hash.class);

    //Constant initialization
    private static final String TABLENAME = "dbpedia_triples";
    private static final String DIFF_TABLENAME = "dbpedia_triples_diff";
    private static final String FIELD_OAIID = "oaiid";
    private static final String FIELD_RESOURCE = "resource";
    private static final String FIELD_JSON_BLOB = "content";

    public String oaiId;

    private static JDBC jdbc;
    private HashMap hashesFromStore=null;
    private boolean hasHash = false;
    private boolean active = false;

    private HashMap originalHashes=null;//This object will contain a copy of the original triples, in order to compare them later

    public HashMap newJSONObject = new HashMap();
    private HashMap originalJSONObject ;
    //normal rdftriples
    private HashMap addTriples = new HashMap();
    //special array with sparulpattern
    private HashMap deleteTriples = new HashMap();

    private String Subject = null; //TODO This member is not explicitly typed in the php file, so we must make sure of its type

    //I managed to move this member from function _compareHelper as the variables sequences used in deleting old
    // abstracts may coincide, so it is better to keep it as a static member and reset it from time to time.
    private static int delCount = 0 ;

//    static
//    {
//        try
//        {
//            logger = Logger.getLogger(Class.forName("org.dbpedia.extraction.live.core.Hash").getName());
//        }
//        catch (Exception exp){
//
//        }
//    }



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
                    try{
                        result.close();
                        result.getStatement().close();
                    }
                    catch (SQLException sqlExp){
                        logger.warn("SQL statement cannot be closed");
                    }

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

                //Closing the underlying statement, in order to avoid overwhelming Virtuoso
                try{
                    if(ResultMake != null){
                        ResultMake.close();
                        ResultMake.getStatement().close();
                    }
                }
                catch (SQLException sqlExp){
                    logger.warn("SQL statement cannot be closed");
                }

                //Closing the underlying statement, in order to avoid overwhelming Virtuoso
                try{
                    if(ResultSecondTest != null){
                        ResultSecondTest.close();
                        ResultSecondTest.getStatement().close();
                    }
                }
                catch (SQLException sqlExp){
                    logger.warn("SQL statement cannot be closed");
                }

            }
            else
            {
                logger.info("Hash:: table " + TABLENAME + " found");

                //Closing the underlying statement, in order to avoid overwhelming Virtuoso
                try{
                    result.close();
                    result.getStatement().close();
                }
                catch (SQLException sqlExp){
                    logger.warn("SQL statement cannot be closed");
                }

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

            //jdbcResult.close();
            //Closing the underlying statement, in order to avoid overwhelming Virtuoso
            try{
                if(jdbcResult != null){
                    jdbcResult.close();
                    jdbcResult.getStatement().close();
                }
            }
            catch (SQLException sqlExp){
                logger.warn("SQL statement cannot be closed");
            }


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
            originalHashes = (HashMap) parser.parse(Temp, containerFactory);
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
        if(!this.active)
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
        if(!updateExecutedSuccessfully)
        {
            logger.fatal("FAILED to update hashes " + sql);
        }
        else
        {
            logger.info(this.Subject + " updated hashes for " + this.newJSONObject.size() + " extractors " + needed);
        }

        //Closing the underlying statement, in order to avoid overwhelming Virtuoso
        try{
            stmt.close();
        }
        catch (SQLException sqlExp){
            logger.warn("SQL statement stmt cannot be closed in function updateDB");
        }

        ExecutorService es = Executors.newSingleThreadExecutor();

        final Future future = es.submit(new Callable() {
            public Object call() throws Exception {
                _insertNewDiffTriples();
                return null;
            }
        });


        try {
            future.get(); // blocking call - the main thread blocks until task is done
        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
        }


    }

    private void _insertNewDiffTriples(){
        Delta delta = DeltaCalculator.calculateDiff(this.Subject, originalHashes, newJSONObject);

        if(this.Subject.contains("/File:")){
            return;
        }

        CallableStatement updateOldDiffTriplesStsmt = jdbc.prepareCallableStatement("update_dbpedia_triples_diff_for_resource(?,?,?,?)",
                "Hash::updateDB");

        String addedTripleString = delta.formulateAddedTriplesAsNTriples(true);
        String deletedTripleString = delta.formulateDeletedTriplesAsNTriples(true);
        String modifiedTripleString = delta.formulateModifiedTriplesAsNTriples(true);

        jdbc.executeCallableStatement(updateOldDiffTriplesStsmt,new String[]{this.Subject, addedTripleString, deletedTripleString
                , modifiedTripleString});

        //Closing the underlying statement, in order to avoid overwhelming Virtuoso
        try{
            if(updateOldDiffTriplesStsmt != null){
                updateOldDiffTriplesStsmt.close();
            }
        }
        catch (SQLException sqlExp){
            logger.warn("SQL statement cannot be closed");
        }

        boolean hasDelta = (addedTripleString.compareTo("") != 0 && deletedTripleString.compareTo("") != 0)
                && modifiedTripleString.compareTo("") != 0;



        if(hasDelta){

            int itemPos = Arrays.asList(Main.recentlyUpdatedInstances).indexOf(new RecentlyUpdatedInstance(this.Subject));
            Main.recentlyUpdatedInstances[itemPos].setHasDelta(true);
        }

    }


    /**
     * Inserts a new record into dbpedia_triples table, in order to later use it to compare hashes
     */
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

        PreparedStatement stmt = jdbc.prepare(sql , "Hash::insertIntoDB");

        boolean insertExecutedSuccessfully= jdbc.executeStatement(stmt, new String[]{this.oaiId, this.Subject,
                jsonEncodedString} );

        String needed = Timer.stopAsString(timerName);

        if(!insertExecutedSuccessfully)
        {
            logger.fatal("FAILED to insert hashes for " + this.newJSONObject.size() + " extractors ");
        }
        else
        {
            logger.info(this.Subject + " inserted hashes for " + this.newJSONObject.size() + " extractors " + needed);
        }

        //Closing the underlying statement, in order to avoid overwhelming Virtuoso
        try{
            stmt.close();
        }
        catch (SQLException sqlExp){
            logger.warn("SQL statement stmt cannot be closed in function insertIntoDB");
        }

        Delta delta = DeltaCalculator.calculateDiff(this.Subject, originalHashes, newJSONObject);
        //First delete old diff triples
        CallableStatement deleteOldDiffTriplesStsmt = jdbc.prepareCallableStatement("DELETE_ALL_RESOURCE_TRIPLES_FROM_dbpedia_triples_diff(?)",
                "Hash::updateDB");
        jdbc.executeCallableStatement(deleteOldDiffTriplesStsmt,new String[]{this.Subject});

        //Insert the added triples as an N-Triples string
        CallableStatement insertNewDiffTriplesStsmt = jdbc.prepareCallableStatement("INSERT_IN_dbpedia_triples_diff(?, ?, ?)",
                "Hash::updateDB");
        jdbc.executeCallableStatement(insertNewDiffTriplesStsmt,new String[]{this.Subject, Integer.toString(Delta.DiffType.ADDED.getCode()),
                delta.formulateAddedTriplesAsNTriples(true)});

        //Closing the underlying statement, in order to avoid overwhelming Virtuoso
        try{
            if(deleteOldDiffTriplesStsmt != null){
                deleteOldDiffTriplesStsmt.close();
            }
        }
        catch (SQLException sqlExp){
            logger.warn("SQL statement deleteOldDiffTriplesStsmt cannot be closed in insertIntoDB");
        }

        //Closing the underlying statement, in order to avoid overwhelming Virtuoso
        try{
            if(insertNewDiffTriplesStsmt != null){
                insertNewDiffTriplesStsmt.close();
            }
        }
        catch (SQLException sqlExp){
            logger.warn("SQL statement insertNewDiffTriplesStsmt cannot be closed in insertIntoDB");
        }

        //Main.

    }

    /**
     * Deletes a record for a specific resource from dbpedia_triples table.
     */
    public void deleteFromDB(){
        if(!this.active)
        {
            return;
        }

        //If the application is working in multithreading mode, we must attach the thread id to the timer name
        //to avoid the case that a thread stops the timer of another thread.
        String timerName = "Hash::deleteFromDB" +
                (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

        Timer.start(timerName);

        String sql = "DELETE FROM " + TABLENAME + " WHERE " + FIELD_OAIID + " = " + this.oaiId;

        PreparedStatement stmt = jdbc.prepare(sql , "Hash::deleteFromDB");

        boolean deleteExecutedSuccessfully= jdbc.executeStatement(stmt, new String[]{} );

        String needed = Timer.stopAsString(timerName);

        if(!deleteExecutedSuccessfully)
        {
            logger.fatal("FAILED to delete hashes for " + this.newJSONObject.size() + " extractors ");
        }
        else
        {
            logger.info("Hashes for " + this.Subject + " has been deleted");
        }

        //Closing the underlying statement, in order to avoid overwhelming Virtuoso
        try{
            stmt.close();
        }
        catch (SQLException sqlExp){
            logger.warn("SQL statement stmt cannot be closed in function deleteFromDB");
        }
    }


    private void _compareHelper(String extractorID, ArrayList triples)
    {

        int matchCount = 0;
        int addCount = 0;
        int total = 0;

        //reset delCount if necessary
        if(delCount > 10000)
            delCount = 0;

        //init

        if((triples != null) && (triples.size()!=0))
        {
            this.newJSONObject.put(extractorID, new HashMap());
        }

        ArrayList <String>keys = new ArrayList<String>();
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

        HashMap hmHashAndTriples = (HashMap)hashesFromStore.get(extractorID);
        Iterator hashesForIterator = hmHashAndTriples.entrySet().iterator();
        while(hashesForIterator.hasNext())
        {
            Map.Entry pairs = (Map.Entry)hashesForIterator.next();

            //initialize the hashmap if it is already null 
            if(deleteTriples == null)
                deleteTriples = new HashMap();

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
    }


    public boolean hasHash()
    {
        return this.hasHash;
    }

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

    public static Model getTriples(String requiredResource, TriplesType typeOfRequiredTriples)
    {
        try
        {
            String sqlStatement = "";

            if(typeOfRequiredTriples == TriplesType.LatestTriples)
                sqlStatement = "SELECT " + FIELD_OAIID + ", " + FIELD_JSON_BLOB + " FROM " + TABLENAME +" WHERE "
                        + FIELD_RESOURCE +" = '" + requiredResource + "'";
            else
                sqlStatement = "SELECT " + FIELD_OAIID + ", " + FIELD_JSON_BLOB + " FROM " + DIFF_TABLENAME +" WHERE "
                        + FIELD_RESOURCE +" = '" + requiredResource + "'";

            JDBC con = JDBC.getDefaultConnection();

            ResultSet jdbcResult= con.exec(sqlStatement, "getTriples");
            int NumberOfRows = 0;

            while(jdbcResult.next())
                NumberOfRows++;

            if(NumberOfRows <= 0)
            {
                logger.info("No triples found for " + requiredResource);
                return null;
            }

            jdbcResult.beforeFirst();
            String Temp = "";

            while(jdbcResult.next())
            {
                Blob blob = jdbcResult.getBlob(2);
                byte[] bdata = blob.getBytes(1, (int) blob.length());
                Temp += new String(bdata);
            }

//            jdbcResult.close();

            //Closing the underlying statement, in order to avoid overwhelming Virtuoso
            try{
                if(jdbcResult != null){
                    jdbcResult.close();
                    jdbcResult.getStatement().close();
                }
            }
            catch (SQLException sqlExp){
                logger.warn("SQL statement cannot be closed");
            }

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
            HashMap hashRetrieved = (HashMap) parser.parse(Temp, containerFactory);
            //this.hashesFromStore = json_decode(Temp, true);

            if(hashRetrieved == null)
            {
                logger.warn("conversion to JSON failed, not using hash this time");
                return null;
            }

            logger.info(requiredResource + " retrieved triples");

            //The following code snippet is responsible for extracting the triples from the HashMap retrieved from the
            //database and convert it into Jena Model in order to ease comparison of triples
            Model requiredTriples = ModelFactory.createDefaultModel();
            Iterator currentTriplesIterator = hashRetrieved.entrySet().iterator();
            while (currentTriplesIterator.hasNext()){
                Map.Entry pairsWithExtractorKey = (Map.Entry)currentTriplesIterator.next();
                HashMap extractorTriplesHashMap = (HashMap) pairsWithExtractorKey.getValue();

                if(extractorTriplesHashMap == null)
                    continue;

                Iterator extractorTriplesIterator = extractorTriplesHashMap.entrySet().iterator();

                while (extractorTriplesIterator.hasNext()){
                    Map.Entry pairsTripleWithHash = (Map.Entry)extractorTriplesIterator.next();


                    HashMap hmActualTriple = (HashMap)pairsTripleWithHash.getValue();

                    //I convert the triple into a string by concatenating its 3 parts, in order to ease its parsing
                    //and conversion into Jena statement object
                    String tripleString = hmActualTriple.get("s") + " " + hmActualTriple.get("p") + " " +
                            hmActualTriple.get("o") + " .";

                    tripleString = tripleString.replace("\"\"\"", "\"");

                    Model tmpModel = requiredTriples.read(new StringReader(tripleString), null, "N-TRIPLE");
                    requiredTriples.add(tmpModel);
                }

            }
            ///////////////End of conversion code//////////////////////

            return requiredTriples;
        }
        catch(Exception exp)
        {
            logger.warn(exp.getMessage());
            return null;
        }
    }


}
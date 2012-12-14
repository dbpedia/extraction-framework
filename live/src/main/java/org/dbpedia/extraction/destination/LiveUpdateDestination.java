package org.dbpedia.extraction.destination;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import org.apache.log4j.Logger;
import org.dbpedia.extraction.destinations.Destination;
import org.dbpedia.extraction.destinations.Quad;
import org.dbpedia.extraction.live.core.*;
import org.dbpedia.extraction.live.core.Timer;
import org.dbpedia.extraction.live.extraction.LiveExtractionConfigLoader;
import org.dbpedia.extraction.live.helper.ExtractorSpecification;
import org.dbpedia.extraction.live.helper.LiveConfigReader;
import org.dbpedia.extraction.live.helper.MatchPattern;
import org.dbpedia.extraction.live.main.Main;
import org.dbpedia.extraction.live.publisher.PublishingData;
import org.dbpedia.extraction.live.storage.JDBC;
import org.dbpedia.extraction.live.storage.RDFTriple;
import org.dbpedia.extraction.live.storage.SPARQLEndpoint;
import org.dbpedia.extraction.live.storage.SPARQLToRDFTriple;
import org.dbpedia.extraction.util.Language;
import org.dbpedia.extraction.wikiparser.WikiTitle;
import org.dbpedia.helper.CoreUtil;
import org.ini4j.Options;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import scala.Function1;
import scala.collection.JavaConversions;
import scala.collection.Seq;
import scala.collection.Traversable;
import scala.runtime.AbstractFunction1;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;

/*import org.openrdf.model.URI;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;*/

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 6, 2010
 * Time: 3:15:03 PM
 * This class represents the destination that should be updated when live data is extracted i.e. Virtuoso server
 */
public class LiveUpdateDestination implements Destination{

    private static Logger logger = Logger.getLogger(LiveUpdateDestination.class);

    //This is used for placing a time limit on the execution of triples update process
    private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

    public static Options options = new Options();

    final String LUD_SPARQLFILTER = "lud_sparqlfilter";
    final int TEST_DELAY = 0;

    /*
	 * Options they should be initiialized
	 * only once at the beginning
	 * */
	private Resource uri;
	private String language;
	private String oaiId;
    private long pageId;

	private String graphURI;
    private String annotationGraphURI;
	private String generateOWLAxiomAnnotations;
	private String languageProperties;
	private boolean debug_turn_off_insert;
	private boolean debug_run_tests;

	private Hash hash;

	//helpers
	private JDBC jdbc;
	private String subjectSPARULpattern;

	//statistic
	private int counterInserts = 0;
	private int counterDelete = 0;
    private int counterTotalJDBCOperations = 0;

    //this is set in ExtractionGroup
    private ArrayList<String> activeExtractors = new ArrayList<String>();
    private ArrayList<String> purgeExtractors = new ArrayList<String>();
    private ArrayList<String> keepExtractors = new ArrayList();
    private ArrayList<MatchPattern> producesFilterList = new ArrayList();
    public ArrayList tripleFromExtractor  = new ArrayList();

    ArrayList<RDFTriple> addedTriplesList;
    String deletedTriplesString;

    public LiveUpdateDestination(String pageURI, String language, String oaiID){
//        this.uri =   RDFTriple.page(pageTitle);
        this.uri =  ResourceFactory.createResource(CoreUtil.encodeURI(pageURI));
        this.language = language;
        this.oaiId = oaiID;

        initDestination();


    }

    /**
     * Initializes the destination variables
     */
    private void initDestination() {
        this.graphURI  = LiveOptions.options.get("graphURI");
        this.annotationGraphURI = LiveOptions.options.get("annotationGraphURI");
        this.generateOWLAxiomAnnotations = LiveOptions.options.get("generateOWLAxiomAnnotations");
        this.languageProperties = LiveOptions.options.get("stringPredicateWithForeignlanguages");
        this.debug_turn_off_insert = Boolean.parseBoolean(LiveOptions.options.get("debug_turn_off_insert"));
        this.debug_run_tests = Boolean.parseBoolean(LiveOptions.options.get("debug_run_tests"));

        this.hash = new Hash(this.oaiId, this.uri.toString());

        this.subjectSPARULpattern = Util.convertToSPARULPattern(uri);

        if((options.get("predicateFilter") != null) && (!options.get("predicateFilter").equals(""))){
            String p = options.get("predicateFilter");
            logger.warn("currently not working");
        }

        if((options.get("objectFilter") != null) && (!options.get("objectFilter").equals(""))){
            String o = options.get("objectFilter");
            logger.warn("currently not working");
        }

        if((options.get("predicateObjectFilter") != null) && (!options.get("predicateObjectFilter").equals(""))){
            String po = options.get("predicateObjectFilter");
           logger.warn("currently not working");
        }


        //Add the extractors to the destination and divide them into 3 groups according to the status
        for(ExtractorSpecification extractorSpec : LiveConfigReader.extractors.get(Language.apply(this.language))){
            addExtractor(extractorSpec);
        }
    }

    public LiveUpdateDestination(WikiTitle pageWikiTitle, String language, String oaiID){
        String resourceURI = pageWikiTitle.resourceIri();
        if (LiveOptions.options.get("language_use_IRI") == "false")
            resourceURI = CoreUtil.encodeURI(resourceURI);

        this.uri =  ResourceFactory.createResource(resourceURI);
        this.language = language;
        this.oaiId = oaiID;

        initDestination();

    }

    @Override
    public void open() {}

    private static <T> T timedCall(FutureTask<T> task, long timeout, TimeUnit timeUnit) throws InterruptedException, ExecutionException, TimeoutException {
        THREAD_POOL.execute(task);
        return task.get(timeout, timeUnit);
    }

    /**
     * Handles the extractor according to its status, so it can be added to active, purge or keep extractors
     * @param extractorSpec The specification of the extractor
     */
    private void addExtractor(ExtractorSpecification extractorSpec){
        switch(extractorSpec.status){
            case ACTIVE:
                activeExtractors.add(extractorSpec.extractorID);
                break;
            case KEEP:
                addFilter(extractorSpec.generatedTriplePatterns);
                keepExtractors.add(extractorSpec.extractorID);
                break;
            case PURGE:
                purgeExtractors.add(extractorSpec.extractorID);
                break;
        }
    }

    //this is set in ExtractionGroup
    private void addActiveExtractor(String extractorID){
        activeExtractors.add(extractorID);
    }
    private void addPurgeExtractor(String extractorID){
        purgeExtractors.add(extractorID);
    }

    //this is set in ExtractionGroup
    //they are the produces entries from ExtractorConfigurator
    public void addFilter(ArrayList<MatchPattern> filter){
        producesFilterList = filter;
    }

    private void _prepare(String languageProperties){
        }

    public void start() { }

    public void accept(ExtractionResult extractionResult) {
        ArrayList triples = extractionResult.getTriples();
        addedTriplesList = (ArrayList<RDFTriple>) triples;
        for (Object objTriple : triples){
            RDFTriple triple = (RDFTriple) objTriple;
            this.tripleFromExtractor.add(triple);

        }

        this.hash.compare(extractionResult);

        HashMap hmDeletedTriples = this.hash.getTriplesToDelete();
        Iterator deletedTriplesKeysIterator = hmDeletedTriples.keySet().iterator();
        if(deletedTriplesString == null)
            deletedTriplesString = "";

        while (deletedTriplesKeysIterator.hasNext()){
             String keyPredicateHash = (String)deletedTriplesKeysIterator.next();

            String strDeletedTriple = convertHashMapToString((HashMap)hmDeletedTriples.get(keyPredicateHash));
		for(int i = 0; i < 2; i++){
			int pos = strDeletedTriple.indexOf("\"");
			if(pos < 0)
				continue;
			strDeletedTriple  = strDeletedTriple.substring(0,pos) + strDeletedTriple.substring(pos+1);

			pos = strDeletedTriple.lastIndexOf("\"");
			if(pos < 0)
				continue;
			strDeletedTriple  = strDeletedTriple.substring(0,pos) + strDeletedTriple.substring(pos+1);
		}
		deletedTriplesString += strDeletedTriple;
        }

        PublishingData pubData;
        if(!Util.isStringNullOrEmpty(deletedTriplesString))
            pubData = new PublishingData(addedTriplesList, deletedTriplesString);
        else
            pubData = new PublishingData(addedTriplesList, true);
//        logger.info("Inside ACCEPT");
        Main.publishingDataQueue.add(pubData);

    }

    /**
     * Converts a triple represented as a HashMap, with S, P, and O as Keys and their values as Values for those keys
     * @param hmTriple  Triple represented as HashMap
     * @return  String representation of the triple
     */
    private String convertHashMapToString(HashMap hmTriple){
        Model tmpModel = ModelFactory.createDefaultModel();

        String pattern = "";
        if(hmTriple.get("s").toString().contains("<"))//The format is right no need to convert it to SPARUL pattern
                pattern += hmTriple.get("s") + " " + hmTriple.get("p") + " " + hmTriple.get("o")+" . \n";
        else//The statement must be converted to SPARUL
            pattern += Util.convertToSPARULPattern(ResourceFactory.createResource(CoreUtil.encodeURI(hmTriple.get("s").toString())))
                + " " + Util.convertToSPARULPattern(ResourceFactory.createProperty(hmTriple.get("p").toString()))
                + " " + Util.convertToSPARULPattern(tmpModel.createLiteral(hmTriple.get("o").toString()))+" . \n";

        return pattern;
    }

    public void write(Seq<Quad> graph){

        Function1<Quad,String> quadDataset = new AbstractFunction1<Quad,String>() {
          public String apply(Quad quad) { return quad.dataset(); }
        };
        Map<String, Traversable<Quad>> tripleWithDataset = JavaConversions.mapAsJavaMap(graph.groupBy(quadDataset));

        Set<String> keySet = tripleWithDataset.keySet();
        Iterator<String> keysIterator = keySet.iterator();

        while(keysIterator.hasNext()){
            String dsKey = keysIterator.next();
            scala.collection.immutable.List<Quad> quadList = (scala.collection.immutable.List<Quad>)tripleWithDataset.get(dsKey);
            ExtractionResult rs = new ExtractionResult(pageId, language, dsKey);

            List<Quad> listQuads = JavaConversions.seqAsJavaList(quadList);
            for(Quad quad : listQuads){
                rs.addTriple(ResourceFactory.createResource(CoreUtil.encodeURI(quad.subject())), ResourceFactory.createProperty(quad.predicate()),
                        constructTripleObject(quad));
            }

            accept(rs);
        }

    }

    public void write(Seq<Quad> graph, String extr){

        ExtractionResult rs = new ExtractionResult(pageId, language, extr);
        Model tmpModel = ModelFactory.createDefaultModel();

        for(Quad quad : JavaConversions.asJavaIterable(graph)){
            rs.addTriple(ResourceFactory.createResource(CoreUtil.encodeURI(quad.subject())), ResourceFactory.createProperty(quad.predicate()),
                    constructTripleObject(quad));
        }

        accept(rs);
    }

    public void setLanguage(String Language){
        language = Language;
    }

    public void setPageID(long PageID){
        pageId = PageID;
    }

    public void setOAIID(String OaiId){
        oaiId = OaiId;
    }

    public void setURI(String pageTitle){
        this.uri = RDFTriple.page(pageTitle);
    }


    private RDFNode constructTripleObject(Quad quad){
        String Lang = quad.language().toString();
        String datatype = quad.datatype();
        Model tmpModel = ModelFactory.createDefaultModel();

        if (datatype != null){
            if (datatype.equals("http://www.w3.org/2001/XMLSchema#string"))
                return tmpModel.createLiteral(quad.value(), Lang);
            else
                 return tmpModel.createTypedLiteral(quad.value(), datatype);
        }
        else
            return ResourceFactory.createResource(quad.value());
    }

    public int countLiveAbstracts(){
        String testquery = "SELECT COUNT(*) as ?count FROM <" + this.graphURI + "> {" +
                this.subjectSPARULpattern + " <" + Constants.DBCOMM_ABSTRACT + "> ?o }";
        SPARQLEndpoint se = SPARQLEndpoint.getDefaultEndpoint();
        return se.executeCount(testquery, this.getClass(), this.graphURI);
    }

    @Override
    public void close() {
        try{
            int abstractCount = this.countLiveAbstracts();
            if(this.hash.hasHash()){
                //We should place a time limit on the extraction process, so if it doesn't stop within the allowed timeframe
                //we should use radical strategy (deleting previously inserted triples and inserting new ones)
                UpdateTriplesWithTimeLimit triplesUpdaterWithLimit = new UpdateTriplesWithTimeLimit();
                FutureTask<Integer> updateExecutionTask = new FutureTask<Integer>(triplesUpdaterWithLimit);
                try{
                    timedCall(updateExecutionTask, 30, TimeUnit.SECONDS);
                }
                catch (Exception exp){

//                    updateTriplesPrimarily();

                    //If normal update process fails with the allowed timeframe, then we should use the primary update
                    //strategy
                    UpdateTriplesPrimarilyWithTimeLimit triplesUpdaterPrimarilyWithLimit = new UpdateTriplesPrimarilyWithTimeLimit();
                    FutureTask<Integer> primarilyUpdateExecutionTask = new FutureTask<Integer>(triplesUpdaterPrimarilyWithLimit);
                    try{
                        timedCall(primarilyUpdateExecutionTask, 1, TimeUnit.MINUTES);
                    }
                    catch (TimeoutException timeoutExp){
                        logger.error("Updating instance " + this.uri + " FAILED, both with main and primary methods...");
                        logger.error("Instance " + this.uri + " not updated");
                        jdbc.reconnect();
                    }

                }
            }
            else {

                //If the application is working in multithreading mode, we must attach the thread id to the timer name
                //to avoid the case that a thread stops the timer of another thread.
                String timerName = "LiveUpdateDestination._primaryStrategy" +
                    (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

                Timer.start(timerName);
                this._primaryStrategy();
                Timer.stop(timerName);
                //does nothing if not active
                this.hash.insertIntoDB();
            }

            int abstractCountAfter = this.countLiveAbstracts();

            boolean errorOccurred = false;

            if((abstractCountAfter-abstractCount)>0 && abstractCountAfter!=1){
                errorOccurred = true;
        }
        if(errorOccurred)
            logger.fatal(" abstracts before/after: " + abstractCount +" / " +abstractCountAfter);
        else
            logger.info(" abstracts before/after: " + abstractCount +" / " +abstractCountAfter);

        }
        catch(Exception exp){

        }
    }

    /**
     * This update star
     */
    private void updateTriplesPrimarily(){
        String timerName = "LiveUpdateDestination._primaryStrategy" +
            (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

        Timer.start(timerName);
        this._primaryStrategy();
        Timer.stop(timerName);

        this.hash.deleteFromDB();
        this.hash.insertIntoDB();
    }

    private void _hashedUpdate(HashMap addTriples, HashMap deleteTriples){
        /*
         * DELETION
         * */

        this.jdbc = JDBC.getDefaultConnection();
        if((deleteTriples != null) && (deleteTriples.size() > 0)){
            this._alt_delete_all_triples(deleteTriples);
        }

		/*
		 * STRATEGIES FOR INSERTION
		 * will do nothing if Options::getOption('debug_turn_off_insert') is true
		 * */
        if((addTriples != null) && (addTriples.size() > 0)){

            //This part of code is used to remove old comment and abstract, as there sometimes comments or abstracts
            // that are existing in the graph but the JSON string for its' resource doesn't contain it, which sometimes
            // cause that the resource has 2 different comments and/or abstracts. So we should make sure that the old one is removed
            Iterator addedTriplesIterator = addTriples.keySet().iterator();
            while (addedTriplesIterator.hasNext()){
                String tripleHash = (String)addedTriplesIterator.next();
                RDFTriple triple = (RDFTriple) addTriples.get(tripleHash);
                String predicate =  triple.getPredicate().toString();
                if((predicate.compareTo(Constants.RDFS_COMMENT) == 0) || (predicate.compareTo(Constants.DB_ABSTRACT) == 0)){
                    removeOldRDFSAbstractOrComment(triple);

                }
                else if((predicate.compareTo(Constants.DBM_EDITLINK) == 0) || (predicate.compareTo(Constants.DBM_REVISION) == 0)
                        || (predicate.compareTo(Constants.DC_MODIFIED) == 0)){
                    removeOldMetaInformation(triple);
                }
            }

			this._jdbc_ttlp_insert_triples(addTriples);
		}

	}


    /**
     * Removes the old RDFS comment or abstract for the passed triple, as the comment and abstracts caused some problems upon normal removal, i.e.
     * sometimes they cannot be removed and so 2 different comments or abstracts may exist for the same subject
     * @param triple    A triple containing subject, predicate, and object for which the old comment or abstract should be removed.
     */
    private void removeOldRDFSAbstractOrComment(RDFTriple triple){
        String pattern = Util.convertToSPARULPattern(triple.getSubject()) + " " +
                Util.convertToSPARULPattern(triple.getPredicate()) + " " + "?o"+" . \n";
//        String sparul = "DELETE FROM <" + this.graphURI + "> { \n  " + pattern + " }" + " WHERE {\n" + pattern + " }";

//        ResultSet result = this._jdbc_sparul_execute(sparul);
//
//        //Closing the underlying statement, in order to avoid overwhelming Virtuoso
//        try{
//            if(result!= null){
//                result.close();
//                result.getStatement().close();
//            }
//        }
//        catch (SQLException sqlExp){
//            logger.warn("SQL statement of result cannot be closed in function removeOldRDFSAbstractOrComment");
//        }

//        boolean isSuccessful = this._jdbcSPARULExecute("SPARQL " + sparul);
        boolean isSuccessful = this._jdbcDeleteTriples(pattern);
        logger.info("Deleted = " + isSuccessful);
    }

    /**
     * Removes the old meta information for the passed triple, sometimes the meta information cannot be removed and so
     * multiple meta information may exist for the same page
     * @param triple    A triple containing subject, predicate, and object for which the old meta information should be removed.
     */
    private void removeOldMetaInformation(RDFTriple triple){
        String pattern = Util.convertToSPARULPattern(triple.getSubject()) + " " +
                Util.convertToSPARULPattern(triple.getPredicate()) + " " + "?o"+" . \n";
        String sparul = "DELETE FROM <" + this.graphURI + "> { \n  " + pattern + " }" + " WHERE {\n" + pattern + " }";

        boolean isSuccessful = this._jdbcDeleteTriples(pattern);
        logger.info("Deleted = " + isSuccessful);
    }

	private void _primaryStrategy(){

        if(!TheContainer.wasSet(LUD_SPARQLFILTER)){
			SPARQLToRDFTriple store = null;
			TripleDiff tripleDiff = new TripleDiff(this.uri,this.language ,this.producesFilterList, store);
			TheContainer.set(LUD_SPARQLFILTER , tripleDiff.createFilter(this.producesFilterList));
        }

		this.jdbc = JDBC.getDefaultConnection();
		graphURI  = this.graphURI ;
        annotationGraphURI  = this.annotationGraphURI ;
		generateOWLAxiomAnnotations = this.generateOWLAxiomAnnotations ;
		/*
		 * STRATEGIES FOR DELETION
		 * */

        //If the application is working in multithreading mode, we must attach the thread id to the timer name
        //to avoid the case that a thread stops the timer of another thread.
        String timerName = "LiveUpdateDestination._jdbc_sparul_delete_total" +
            (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

		Timer.start(timerName);
		this._jdbc_clean_sparul_delete_subresources();
		this._jdbc_sparul_delete_subject_not_static(this.subjectSPARULpattern , TheContainer.get(LUD_SPARQLFILTER ) );

		Timer.stop(timerName);

		this._jdbc_ttlp_insert_triples(this.tripleFromExtractor);
		logger.info("no of queries, insert: " + this.counterInserts + " delete: " + this.counterDelete + " jdbc_total: " + this.counterTotalJDBCOperations);

	}

    private void _alt_delete_all_triples(HashMap fromStore){

        String strDeletedTriples = "";
        //If the application is working in multithreading mode, we must attach the thread id to the timer name
        //to avoid the case that a thread stops the timer of another thread.
        String timerName = "LiveUpdateDestination._alt_delete_all_triples" +
            (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

        Timer.start(timerName);
        String sparul;
        String pattern = "";
        int directCount = 0;
        Model tmpModel = ModelFactory.createDefaultModel();
//         Iterator predicatesIterator = this.predicates.entrySet().iterator();
        Iterator fromStoreIterator = fromStore.entrySet().iterator();
        while(fromStoreIterator.hasNext()){

            Map.Entry pairs = (Map.Entry)fromStoreIterator.next();
            HashMap triple = (HashMap)pairs.getValue();

            if(triple.get("s").toString().contains("<"))//The format is right no need to convert it to SPARUL pattern
                    pattern += triple.get("s") + " " + triple.get("p") + " " + triple.get("o")+" . \n";
            else//The statement must be converted to SPARUL
                pattern += Util.convertToSPARULPattern(new ResourceImpl(triple.get("s").toString()))
                    + " " + Util.convertToSPARULPattern(new PropertyImpl(triple.get("p").toString()))
                    + " " + Util.convertToSPARULPattern(tmpModel.createLiteral(triple.get("o").toString()))+" . \n";

            strDeletedTriples += pattern;

            if(triple.get("s").equals(this.subjectSPARULpattern)){
               directCount++;
            }

        }

//        sparul = "DELETE FROM <" + this.graphURI + "> { \n  " + pattern + " }" + " WHERE {\n" + pattern + " }";

        int countbefore = 0;
        //TESTS>>>>>>>>>>>>
        if(debug_run_tests){
            countbefore = this._testsubject(this.uri.toString());
        }
        //TESTS<<<<<<<<<<<

        this.counterDelete +=1;

        boolean isDeletionSuccessful = this._jdbcDeleteTriples(pattern);

        if(!isDeletionSuccessful){
            logger.info("using fallback strategy (deleting single triples)" );
            strDeletedTriples="";
            fromStoreIterator = fromStore.entrySet().iterator();
            while(fromStoreIterator.hasNext()){
                Map.Entry pairs = (Map.Entry)fromStoreIterator.next();

                String Hash = pairs.getKey().toString();
                HashMap triple = (HashMap)pairs.getValue();

                //TODO this statement is modified because the triples returned from the store are without <>
                if(triple.get("s").toString().contains("<"))//The format is right no need to convert it to SPARUL pattern
                    pattern = triple.get("s") + " " + triple.get("p") + " " + triple.get("o")+" . \n";
                else//The statement must be converted to SPARUL
                    pattern += Util.convertToSPARULPattern(new ResourceImpl(triple.get("s").toString()))
                        + " " + Util.convertToSPARULPattern(new PropertyImpl(triple.get("p").toString()))
                        + " " + Util.convertToSPARULPattern(tmpModel.createLiteral(triple.get("o").toString()))+" . \n";

                strDeletedTriples += pattern;

//                sparul = "DELETE FROM <" + this.graphURI +"> { " + pattern + " }" + " WHERE {\n" + pattern + " }";

                boolean isSuccessful = this._jdbcDeleteTriples(pattern);
                logger.info("Deleted = " + isSuccessful);


            }
        }
        String needed = Timer.stopAsString(timerName);
        logger.info("alt: deleted " + fromStore.size() + " triples directly (" + directCount+")" + needed);

        deletedTriplesString += strDeletedTriples;
        //echo sparul;
        //TESTS>>>>>>>>>>>>
        if(debug_run_tests){
            try{
                Thread.sleep(TEST_DELAY);
            }
            catch (Exception exp){

            }


            logger.info("delaying: " + TEST_DELAY);
            int countafter =  this._testsubject(this.uri.toString());
            logger.info("TEST _alt_delete_all_triples, before: " + countbefore + " after: "+countafter+" triples");
            int diff = countbefore-countafter;
            if( diff !=  directCount ){

                 String eachtriplelog ="";
                 fromStoreIterator = fromStore.entrySet().iterator();
                while(fromStoreIterator.hasNext()){
                    Map.Entry pairs = (Map.Entry)fromStoreIterator.next();

//                    String Hash = pairs.getKey().toString();
                    HashMap triple = (HashMap)pairs.getValue();
                    String testpattern = "where { " + triple.get("s") + " " + triple.get("p") + " " + triple.get("o")+" + } \n";
                    int testOnePattern = this._testwherepart(testpattern);
                    eachtriplelog += testOnePattern+" "+( (testOnePattern > 0)?"NOT deleted: ":"SUCCESS deleted: ");
                    eachtriplelog +=  this._testwhereQuery(testpattern);
                }
                logger.warn("TEST FAILED, AFTER SHOULD BE SMALLER, testing each triple:\neachtriplelog");
                logger.warn("Count executed again, yields :"+ this._testsubject(this.uri.toString()));

                if(!isDeletionSuccessful){
                    logger.warn("Used Fallback last query no advanced testing implemented yet ");
                }else{
                    logger.warn("Delete query: \nsparul");
                    }
                logger.warn("Test query: \n"+this._testsubjectQuery(this.uri.toString()));

            }  else{
                logger.info("SUCCESS");
            }
        }

    }

    private String _testwhereQuery(String testwhere){
        String g = " FROM <" + this.graphURI + "> ";
        String testquery = "SELECT count(*) as ?count " + g + ""+ testwhere;
        return testquery;
    }

	private int _testwherepart(String testwhere){
        String testquery = this._testwhereQuery(testwhere);

        SPARQLEndpoint se = SPARQLEndpoint.getDefaultEndpoint();
        return se.executeCount(testquery, this.getClass(), this.graphURI);
	}

    private String _testprintSPARQLResult(String testwhere){
        String g = " FROM <" + this.graphURI + "> ";
        String testquery = "SELECT * "+ g + testwhere;

        SPARQLEndpoint se = SPARQLEndpoint.getDefaultEndpoint();
        String json =  se.executeQuery(testquery,this.getClass(), this.graphURI);

        JSONParser parser = new JSONParser();

        //This class is object is created to force the JSON decoder to return a HashMap, as hashesFromStore is a HashMap
        ContainerFactory containerFactory = new ContainerFactory(){
            public List creatArrayContainer() {
              return new LinkedList();
            }

            public Map createObjectContainer() {
              return new HashMap();
            }
          };
        HashMap arr = new HashMap();
        try{
                arr = (HashMap)parser.parse(json);
        }
        catch(Exception exp){
            logger.warn("Unable to parse JSON: " + exp.getMessage());
            return "";
        }

        ArrayList vars = (ArrayList)((HashMap)arr.get("head")).get("vars");
        ArrayList bindings = (ArrayList)((HashMap)arr.get("results")).get("bindings");
        String logstr = "";

        if((bindings == null) || (bindings.size()<=0)){
            return logstr;
            }

        for(Object objBinding : bindings){
            HashMap b = (HashMap)objBinding;
            boolean firstElement = true;
            for(Object objVar : vars){
                String var = (String)objVar;
                if(false){
                    logstr +=  ((HashMap)b.get(var)).get("value")+"  ";
                }else{

                    if(firstElement){
                        logstr += this.subjectSPARULpattern+" ";
                        firstElement=false;
                    }
                    if(((HashMap)b.get(var)).get("type").equals("uri")){
                        logstr += "<" + ((HashMap)b.get(var)).get("value") + "> ";
                    }else if(((HashMap)b.get(var)).get("type").equals("literal")){
                        logstr += "\"" + ((HashMap)b.get(var)).get("value") + "\"";

                        String Lang = ((HashMap)b.get(var)).get("xml:lang").toString();
                        if(Util.isStringNullOrEmpty(Lang))
                            logstr += " ";
                        else
                            logstr += "@" + Lang;
                        //logstr += (isset($b[$var]["xml:lang"]))?"@"+$b[$var]["xml:lang"]:" ";
                    }else if(((HashMap)b.get(var)).get("type").equals("typed-literal")){
                        logstr += "\"" + ((HashMap)b.get(var)).get("value") + "\"^^<" + ((HashMap)b.get(var)).get("datatype") +"> ";
                    }
                }
            }

            logstr +="  .\n";
        }
        return logstr;
    }

    private String _testsubjectQuery(String subject){
            String g = " FROM <" + this.graphURI + "> ";
            String testquery = "SELECT count(*) as ?count " + g + " { <" + subject + "> ?p ?o }";
            return testquery;
        }

	private int _testsubject(String subject ){
			String testquery = this._testsubjectQuery(subject);
			SPARQLEndpoint se = SPARQLEndpoint.getDefaultEndpoint();
			return se.executeCount(testquery, this.getClass(), this.graphURI);
	}

    private void _jdbc_sparul_delete_subject_not_static(String subjectPattern, String filterWithLang){
        //***********************
        //DELETE ALL NON STATIC TRIPLES
        //**********************
        //delete all triples with the current subject
        //according to the filters
        //do not delete special properties see below
        String tmpFilter = (filterWithLang.trim().length() > 0) ? "FILTER( \n" + filterWithLang + "). " : " ";
        String sparul = "DELETE FROM <" + this.graphURI + "> { " + this.subjectSPARULpattern + " ?p ?o } FROM <" + this.graphURI + "> ";
        String where = " WHERE { " + subjectPattern + " ?p ?o . " + tmpFilter + '}';
        sparul += where;
            //TESTS>>>>>>>>>>>>
        int countbefore = 0;
        String triplesBefore = "";
        if(debug_run_tests){
            countbefore = this._testwherepart(where);
            triplesBefore = this._testprintSPARQLResult(where);
            }

        //If the application is working in multithreading mode, we must attach the thread id to the timer name
        //to avoid the case that a thread stops the timer of another thread.
        String timerName = "LiveUpdateDestination::_jdbc_sparul_delete_subject_not_static" +
            (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

        Timer.start(timerName);

        if(this._jdbcSPARULExecute("SPARQL " + sparul) ){
            this.counterDelete+=1;
        }

        String needed = Timer.stopAsString(timerName);
        logger.info("deleted subject_not_static, needed " + needed);

        //TESTS>>>>>>>>>>>>
        if(debug_run_tests){
            try{
                Thread.sleep(TEST_DELAY);
            }
            catch (Exception exp){

            }

            logger.info("delaying: " + TEST_DELAY);
            int countafter = this._testwherepart(where);
            logger.info("TEST delete_subject_not_static, before: " + countbefore + " after: " + countafter + " triples");
            if(countafter > 0 && countbefore > 0){
                logger.warn("<TRIPLES_BEFORE>\n" + triplesBefore + "</TRIPLES_BEFORE>");
                logger.warn("<TRIPLES_AFTER>\n" + this._testprintSPARQLResult(where) + "</TRIPLES_AFTER>");
                logger.warn("TEST FAILED, AFTER SHOULD BE 0");
                logger.warn("Testquery: " + this._testwhereQuery(where));
                logger.warn("Deletequery: " + sparul);
                logger.warn("Remaining triples, diplayed below: \n" + this._testprintSPARQLResult(where));
                logger.warn("Count executed again, yields: " + this._testwherepart(where));

            }  else{
                logger.info("SUCCESS");
            }
        }

    }

    private void _jdbc_clean_sparul_delete_subresources(){
        _jdbc_clean_sparul_delete_subresources("");
    }


    private void _jdbc_clean_sparul_delete_subresources(String log){
		Resource subject = this.uri;
		String sparul = "DELETE FROM <" + this.graphURI + ">	{ ?subresource ?p  ?o .  }"; //FROM <" + this.graphURI + ">";
        String where = " where { " + this.subjectSPARULpattern + " ?somep ?subresource . ?subresource ?p  ?o . FILTER (?subresource LIKE <"
        + subject + "/%>)}";
		sparul += where ;

        int countbefore = 0, countafter = 0;

		if(debug_run_tests){
				countbefore = this._testwherepart(where);
			}

        //If the application is working in multithreading mode, we must attach the thread id to the timer name
        //to avoid the case that a thread stops the timer of another thread.
        String timerName = "LiveUpdateDestination._jdbc_clean_sparul_delete_subresources" +
            (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

		Timer.start(timerName);

        if(this._jdbcSPARULExecute("SPARQL " + sparul) ){
            this.counterDelete+=1;
        }

		String needed = Timer.stopAsString(timerName);
		logger.info("deleted subresources, needed "+needed );
		//TESTS>>>>>>>>>>>>
		if(debug_run_tests){
            try{
                Thread.sleep(TEST_DELAY);
            }
            catch (Exception exp){

            }

            logger.info("delaying: "+TEST_DELAY);
            countafter = this._testwherepart(where);
            logger.info("TEST delete_subResources, before: "+countbefore+ " after: "+countafter+" triples");
            if(countafter > 0 && countbefore > 0){
                logger.warn("TEST FAILED, AFTER SHOULD BE 0");
                logger.warn("Test: "+this._testwhereQuery(where));
                logger.warn("Delete: "+ sparul);
            }  else{
                logger.info("SUCCESS");
            }
        }
		//TESTS<<<<<<<<<<<<
	}

    private void _jdbc_ttlp_insert_triples(HashMap triplesToAdd){
        if(this.debug_turn_off_insert){
            return;
            }
        //**********************
        //GENERATE NEW TRIPLES
        //**********************

        //If the application is working in multithreading mode, we must attach the thread id to the timer name
        //to avoid the case that a thread stops the timer of another thread.
        String firstTimerName = "LiveUpdateDestination._jdbc_ttlp_insert_triples" +
            (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

        String secondTimerName = "LiveUpdateDestination._jdbc_ttlp_insert_triples.string_creation" +
            (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

        Timer.start(firstTimerName);
        Timer.start(secondTimerName);

        String globalTripleNTriplePattern = "";
        int tripleCounter = triplesToAdd.size();
        logger.info("number of triple inserts: " + tripleCounter );


        /*foreach (triplesToAdd as triple){
                globalTripleNTriplePattern += triple.toNTriples();
        }*/

        Iterator triplesToAddIterator = triplesToAdd.entrySet().iterator();
        while(triplesToAddIterator .hasNext()){
            Map.Entry pairs = (Map.Entry)triplesToAddIterator.next();
            RDFTriple triple = (RDFTriple) pairs.getValue();
            globalTripleNTriplePattern += triple.toNTriples();
        }


        logger.info("length globalTriplePattern: " + globalTripleNTriplePattern.length());

        Timer.stop(secondTimerName);
        int countbefore = 0;
        String where = "WHERE { " + this.subjectSPARULpattern + " ?p ?o } ";
        if(debug_run_tests){
            countbefore = this._testwherepart(where );
        }

        String insertTimerName = "LiveUpdateDestination._jdbc_ttlp_insert_triples.insert_operation" +
            (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

        Timer.start(insertTimerName);
        boolean globalSuccess = this._jdbc_ttlp_execute( globalTripleNTriplePattern, this.graphURI) ;
        if(globalSuccess) {
            this.counterInserts+=1;
        }

        Timer.stop(insertTimerName);
        Timer.stop(firstTimerName);

        //TESTS>>>>>>>>>>>>
        if(debug_run_tests){
            int countafter = this._testwherepart(where);

            logger.info("TEST _jdbc_ttlp_insert_triples, before: " + countbefore + " after: " + countafter + " triples");
            if(countafter - countbefore < 0 && tripleCounter >0){
                logger.warn("TEST FAILED, INSERT TRIPLES AFTER SHOULD BE BIGGER THAN BEFORE");
            }else{
                logger.info("SUCCESS");
            }
        }
        //TESTS<<<<<<<<<<<<
    }

    private void _jdbc_ttlp_insert_triples(ArrayList triplesToAdd){
        if(this.debug_turn_off_insert){
            return;
            }
        //**********************
        //GENERATE NEW TRIPLES
        //**********************

        //If the application is working in multithreading mode, we must attach the thread id to the timer name
        //to avoid the case that a thread stops the timer of another thread.
        String firstTimerName = "LiveUpdateDestination._jdbc_ttlp_insert_triples" +
            (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

        String secondTimerName = "LiveUpdateDestination._jdbc_ttlp_insert_triples.string_creation" +
            (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

        Timer.start(firstTimerName);
        Timer.start(secondTimerName);
        String globalTripleNTriplePattern = "";
        int tripleCounter = triplesToAdd.size();
        logger.info("number of triple inserts: " + tripleCounter );

        for(Object objTriple : triplesToAdd){
            RDFTriple triple = (RDFTriple) objTriple;
            globalTripleNTriplePattern += triple.toNTriples();
        }


        logger.info("length globalTriplePattern: " + globalTripleNTriplePattern.length());

        Timer.stop(secondTimerName);
        //TESTS>>>>>>>>>>>>
        int countbefore = 0;
        String where = "WHERE { " + this.subjectSPARULpattern + " ?p ?o } ";
        if(debug_run_tests){
            countbefore = this._testwherepart(where );
        }
        //TESTS<<<<<<<<<<<<
        String insertTimerName = "LiveUpdateDestination._jdbc_ttlp_insert_triples.insert_operation" +
            (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

        Timer.start(insertTimerName);
        boolean globalSuccess = this._jdbc_ttlp_execute( globalTripleNTriplePattern, this.graphURI) ;
        if(globalSuccess) {
            this.counterInserts+=1;
        }

        Timer.stop(insertTimerName);
        Timer.stop(firstTimerName);

        //TESTS>>>>>>>>>>>>
        if(debug_run_tests){
            int countafter = this._testwherepart(where);

            logger.info("TEST _jdbc_ttlp_insert_triples, before: " + countbefore + " after: " + countafter + " triples");
            if(countafter - countbefore < 0 && tripleCounter >0){
                logger.warn("TEST FAILED, INSERT TRIPLES AFTER SHOULD BE BIGGER THAN BEFORE");
            }else{
                logger.info("SUCCESS");
            }
        }
    }

    private ResultSet _jdbc_sparul_execute(String query){
        try{
            ResultSet results = null;
            //dryRun means only to log the query without executing it
            boolean dryRun = Boolean.parseBoolean(LiveOptions.options.get("dryRun"));

            if(this.jdbc == null)
                this.jdbc = JDBC.getDefaultConnection();

            if(dryRun){
                logger.info(query);
            }else{
                String virtuosoPl = "sparql " + query + "";
                virtuosoPl = virtuosoPl.replace("\"\"\"", "\"");
                results = this.jdbc.exec( virtuosoPl, "LiveUpdateDestination");

                if(results != null){

                    //Calculate the number of rows returned by the query
                    results.last();
                    int NumberOfRows = results.getRow();
                    if(NumberOfRows <=0)
                        logger.info("JDBC.exec returned empty array");
                    else{
                        results.first();
                        while(results.next()){
                            logger.info("jdbc_exec returned: " + results.getString(0));
                        }
                    }

                    //Closing the underlying statement, in order to avoid overwhelming Virtuoso
                    try{
                        results.close();
                        results.getStatement().close();
                    }
                    catch (SQLException sqlExp){
                        logger.warn("SQL statement results cannot be closed in function _jdbc_sparul_execute");
                    }
                }

                this.counterTotalJDBCOperations+=1;
                logger.trace(virtuosoPl);

            }
            return results;

        }
        catch(Exception exp){
            return null;
        }

    }

    private boolean _jdbcSPARULExecute(String sparulStmt){
        boolean isSuccessful = false;
        try{

            sparulStmt = sparulStmt.replace("\"\"\"", "\"");

            PreparedStatement stmt = jdbc.prepare(sparulStmt, "LiveUpdateDestination");
            isSuccessful = jdbc.executeStatement(stmt, new String[]{});
            stmt.close();
        }
        catch (SQLException exp){
            logger.error("SPARUL Statement \"" + sparulStmt + "\" cannot be executed");
        }
        return isSuccessful;
    }

    /**
     * Deletes the specified triples from the store
     * @param sparulPattern The pattern containing the triples that should be deleted
     * @return  Whether the deletion was successful or not
     */
    private boolean _jdbcDeleteTriples(String sparulPattern){
        boolean isSuccessful = false;
        int numberOfTriplesToDelete = sparulPattern.split("\n").length;

        String sparulDeleteStatement = "SPARQL DELETE FROM <" + this.graphURI + "> { \n" + sparulPattern + " }" + " WHERE {\n" + sparulPattern + " }";

        try{

            sparulPattern = sparulPattern.replace("\"\"\"", "\"");
            if(this.jdbc == null)
                this.jdbc = JDBC.getDefaultConnection();

            ResultSet results = this.jdbc.exec( sparulDeleteStatement, "LiveUpdateDestination");
            //The string returned from Virtuoso is of the form "Delete from <http://dbpedia.org>, x triples -- etc",
            //so we should split the string int order to get the actual number of affected triples
            results.last();
            int numberOfAffectedTriples = Integer.parseInt(results.getString(1).split("--")[0].split(",")[1].trim().split(" ")[0]);

            if(numberOfAffectedTriples < numberOfTriplesToDelete)//if the number of affected triples is less, then deletion was not successful
                return false;
            else
                return true;


        }
        catch (Exception exp){
            logger.error("SPARUL Statement \"" + sparulPattern + "\" cannot be executed");
        }
        return isSuccessful;
    }


    private boolean _jdbc_ttlp_execute(String ntriples, String graphURI){

        try{
            boolean jdbc_result;

            boolean dryRun = Boolean.parseBoolean(LiveOptions.options.get("dryRun"));
            if(this.jdbc == null)
                this.jdbc = JDBC.getDefaultConnection();
            if(dryRun){
                String virtuosoPl = "DB.DBA.TTLP_MT (\n'"+ ntriples + "', '" + graphURI + "', '" + graphURI + "', 255)";
                logger.info(virtuosoPl);
                jdbc_result = true;
            }else{
                String virtuosoPl = "DB.DBA.TTLP_MT (?, '" + graphURI + "', '" + graphURI + "', 255)";


                PreparedStatement stmt = this.jdbc.prepare(virtuosoPl , "LiveUpdateDestination");
                stmt.setString(1, ntriples);
                jdbc_result = stmt.execute();

                if(!jdbc_result){
                    logger.error("ttlp insert failes");
                    logger.error(virtuosoPl);
                    logger.error(ntriples.substring(0,100));

                }else{
                    logger.info("insert returned a true via jdbc_execute");
                }

                this.counterTotalJDBCOperations += 1;
                logger.trace(virtuosoPl);
            }
            return jdbc_result;

        }
        catch(Exception exp){
            return false;
        }
    }

    /**
     * Removes the triples that were created by extractors their statuses are currently PURGE
     */
    public void removeTriplesForPurgeExtractors(){
        deletedTriplesString ="";
        for (String extractorID : purgeExtractors){
            HashMap triplesMap = this.hash.getTriplesForExtractor(extractorID);
            if(triplesMap != null){
                _alt_delete_all_triples(triplesMap);

            }

        }

        //Publish the deleted triples
        PublishingData pubData = new PublishingData(addedTriplesList,deletedTriplesString);
        Main.publishingDataQueue.add(pubData);

    }

    /**
     * Keeps the triples generated by extractors their statuses are currently PURGE
     */
    public void retainTriplesForKeepExtractors(){
        //We should only update the JSON object that will be written to the database, so
        //we need not to do anything with the triples themselves
        for (String extractorID : keepExtractors){
            this.hash.updateJSONObjectForExtractor(extractorID);
        }

    }

    /**
     * Removes the previously generated triples for an extractor, i.e. In case that this extractor is in
     * PURGE status
     * @param extractorID   The ID of the extractor
     */
    private void _removeTriplesForExtractor(String extractorID){
        HashMap map = this.hash.getTriplesForExtractor(extractorID);
        logger.info(map);
    }

    private void writeAddedTriples(ArrayList arrTriplesToBeAdded){

        logger.info("///////////////////////////////////////ADDED TRIPLES////////////////////////////////");

        for(int i = 0; i < arrTriplesToBeAdded.size(); i++){
            RDFTriple t = (RDFTriple)arrTriplesToBeAdded.get(i);
            logger.info(t.toNTriples());

        }

        logger.info("/////////////////////////////////////////////////////////////////////////////////////");
    }

     private void writeDeletedTriples(ArrayList arrTriplesToBeDeleted){

        logger.info("///////////////////////////////////////ADDED TRIPLES////////////////////////////////");

        for(int i = 0; i < arrTriplesToBeDeleted.size(); i++){
            RDFTriple t = (RDFTriple) arrTriplesToBeDeleted.get(i);
//            logger.info(arrTriplesToBeDeleted.get(i));
            logger.info(t.toNTriples());

        }

        logger.info("/////////////////////////////////////////////////////////////////////////////////////");
    }

    /**
     * This class implements Callable interface, which provides the ability to place certain time limit on the execution
     * time of a function, so it must end after a specific time limit.
     * This helps in case of hanging while extraction from a page
     */
    private class UpdateTriplesWithTimeLimit implements Callable<Integer> {

        public UpdateTriplesWithTimeLimit(){

        }
        public Integer call(){

             logger.info("Inside hasHash In thread "+ Thread.currentThread().getId());

                //If the application is working in multithreading mode, we must attach the thread id to the timer name
                //to avoid the case that a thread stops the timer of another thread.
                String timerName = "LiveUpdateDestination._hashedUpdate_Strategy" +
                    (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");
                Timer.start(timerName);
                HashMap addTriples = hash.getTriplesToAdd();
                HashMap deleteTriples = hash.getTriplesToDelete();

                //update db
                //update triples
                _hashedUpdate(addTriples, deleteTriples);
                hash.updateDB();
                Timer.stop(timerName);

            return 0;

        }

    }

    /**
     * It the complement of class UpdateTriplesWithTimeLimit, that should be used in case of failure of the main
     * update methodology, but we should also run it within a specific timeframe in order not to block the extraction process
     * upon a failure in Virtuoso, e.g. Stack Overflow error sometimes occurs and the deletion process block and so the
     * whole system blocks.
     * Ideally the timeframe assigned to it should be higher than that assigned to UpdateTriplesWithTimeLimit
     */
    private class UpdateTriplesPrimarilyWithTimeLimit implements Callable<Integer> {

        public UpdateTriplesPrimarilyWithTimeLimit(){

        }
        public Integer call(){

            logger.info("Inside hasHash In thread "+ Thread.currentThread().getId());

            String timerName = "LiveUpdateDestination._primaryStrategy" +
                    (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

            Timer.start(timerName);
            _primaryStrategy();
            Timer.stop(timerName);

            hash.deleteFromDB();
            hash.insertIntoDB();

            return 0;

        }

    }

}
package org.dbpedia.extraction.live.delta;

import com.hp.hpl.jena.rdf.model.*;
import org.dbpedia.extraction.live.core.JDBC;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;

import java.io.StringReader;
import java.sql.Blob;
import java.sql.ResultSet;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: 11/12/11
 * Time: 11:58 AM
 * Calculates the delta of a specific resource, i.e. it calculates the added/modified/deleted triples for a specific
 * resource
 */
public class DeltaCalculator {
    private static final String DBPEDIA_TABLENAME = "dbpedia_triples";
    private static final String DBPEDIA_DIFF_TABLENAME = "dbpedia_triples_for_diff";
    private static final String FIELD_OAIID = "oaiid";
    private static final String FIELD_RESOURCE = "resource";
    private static final String FIELD_JSON_BLOB = "content";

    public static enum TriplesType
    {
        PreviousTriples, LatestTriples;
    }

    public static Model getTriples(String requiredResource, TriplesType typeOfRequiredTriples)
    {
        try
        {
            String sqlStatement = "";

            if(typeOfRequiredTriples == TriplesType.LatestTriples)
                sqlStatement = "SELECT " + FIELD_OAIID + ", " + FIELD_JSON_BLOB + " FROM " + DBPEDIA_TABLENAME +" WHERE "
                    + FIELD_RESOURCE +" = '" + requiredResource + "'";
            else
                sqlStatement = "SELECT " + FIELD_OAIID + ", " + FIELD_JSON_BLOB + " FROM " + DBPEDIA_DIFF_TABLENAME +" WHERE "
                    + FIELD_RESOURCE +" = '" + requiredResource + "'";

            JDBC con = JDBC.getDefaultConnection();

            ResultSet jdbcResult= con.exec(sqlStatement, "getTriples");
            int NumberOfRows = 0;

            while(jdbcResult.next())
                NumberOfRows++;

            if(NumberOfRows <= 0)
            {
//				logger.info("No triples found for " + requiredResource);
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
            HashMap hashRetrieved = (HashMap) parser.parse(Temp, containerFactory);
            //this.hashesFromStore = json_decode(Temp, true);

            if(hashRetrieved == null)
            {
//                logger.warn("conversion to JSON failed, not using hash this time");
                return null;
            }

//            logger.info(requiredResource + " retrieved triples");

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

                   // tripleString = "<http://dbpedia.org/resource/Version_1.0_Editorial_Team/Wikipedia_articles_by_quality_log> <http://purl.org/dc/terms/subject> <http://dbpedia.org/resource/Category:Project-Class_Wikipedia_articles> . ";
                    //String tripleStatement =  requiredTriples.createStatement(hmActualTriple.get("s") );
                    Model tmpModel = requiredTriples.read(new StringReader(tripleString), null, "N-TRIPLE");
                    requiredTriples.add(tmpModel);
                }

            }
            ///////////////End of conversion code//////////////////////

            return requiredTriples;
        }
        catch(Exception exp)
        {
//            logger.warn(exp.getMessage());
            return null;
        }
    }

    public static Delta calculateDiff(String requiredResource){
        try{
            //2 sets of triples, one for the latest version of the triples, and one for the previous set
            Model latestTriples = getTriples(requiredResource, TriplesType.LatestTriples);
            Model previousTriples = getTriples(requiredResource, TriplesType.PreviousTriples);

            //Diffing the latest and the previous set, gives us a list of all added and modified triples, in their new form
            Model addedAndModifiedTriples = latestTriples.difference(previousTriples);

            //Diffing the latest and the previous set, gives us a list of all added and modified triples, but in their old form
            Model deletedAndModifiedTriples = previousTriples.difference(latestTriples);

            /*Model modifiedTriples = addedAndModifiedTriples.intersection(deletedAndModifiedTriples);
            Model addedTriples = addedAndModifiedTriples.difference(modifiedTriples);
            Model deletedTriples = deletedAndModifiedTriples.difference(modifiedTriples);*/

            //This model will contain the modified triples only, in their new form
            Model modifiedTriples = ModelFactory.createDefaultModel();

            //This model will contain the modified triples only, in old new form
            Model modifiedTriplesWithOldValues = ModelFactory.createDefaultModel();

            //Iterate through the two lists and compare the subject and predicate values, if they are the same, then
            //those triples are the same but the object is modified
            StmtIterator addedAndModifiedTriplesIterator = addedAndModifiedTriples.listStatements();
            while (addedAndModifiedTriplesIterator.hasNext()){
                Statement addedOrModifiedStatement = addedAndModifiedTriplesIterator.nextStatement();

                StmtIterator deletedAndModifiedTriplesIterator = deletedAndModifiedTriples.listStatements();

                while(deletedAndModifiedTriplesIterator.hasNext()){

                    Statement deletedOrModifiedStatement = deletedAndModifiedTriplesIterator.nextStatement();

                    //If the subject and the predicate are the same, then the change is in object value, which means
                    //that this a modified triple
                    if((addedOrModifiedStatement.getSubject().getURI().compareTo(deletedOrModifiedStatement.getSubject().getURI()) == 0)
                            && (addedOrModifiedStatement.getPredicate().getURI().compareTo(deletedOrModifiedStatement.getPredicate().getURI()) == 0)){

                        modifiedTriples.add(addedOrModifiedStatement);
                        modifiedTriplesWithOldValues.add(deletedOrModifiedStatement);

                    }

                }
            }

            //The added triples can now be calculated by just diffing addedAndModifiedTriples and modifiedTriples
            Model addedTriples = addedAndModifiedTriples.difference(modifiedTriples);
            Model deletedTriples = deletedAndModifiedTriples.difference(modifiedTriplesWithOldValues);

            return new Delta(requiredResource, addedTriples, deletedTriples, modifiedTriples);

        }
        catch(Exception exp){
//            logger.warn(exp.getMessage());
            return null;

        }

    }
}

package org.dbpedia.extraction.destination;

import org.apache.log4j.Logger;
import org.dbpedia.extraction.destinations.Dataset;
import org.dbpedia.extraction.destinations.Destination;
import org.dbpedia.extraction.destinations.Graph;
import org.dbpedia.extraction.destinations.Quad;
import org.dbpedia.extraction.ontology.datatypes.Datatype;


import org.dbpedia.helper.CoreUtil;
import org.dbpedia.helper.Triple;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import scala.collection.JavaConversions;
import org.json.simple.*;

import java.io.FileOutputStream;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Aug 19, 2010
 * Time: 12:11:33 PM
 * This class is used to write the values of the SQL columns required to insert the page id along with the resource id and the JSON
 * representation of the triples into dbpedia_triples table that helps in live extraction.
 */
public class SQLFileDestination implements Destination {

    //Constants used for inserting into the table
    private static final String FILENAME = "./SqlLog.sql";
    private static Logger logger = null;

    static{

        logger = Logger.getLogger(SQLFileDestination.class.getName());
    }

    public HashMap JSONObject = new HashMap();
    private String oaiId;
    private URI uri;

    public SQLFileDestination(String pageTitle, String oaiID){
        this.uri = Triple.page(pageTitle);
        oaiId = oaiID;
    }

    public synchronized void write(Graph graph){

        List tripleList = JavaConversions.asList(graph.quads());
        Map<Dataset, scala.collection.immutable.List<Quad>> tripleWithDataset = JavaConversions.asMap(graph.quadsByDataset());

        Set keySet = tripleWithDataset.keySet();
        Iterator keysIterator = keySet.iterator();

        while(keysIterator.hasNext()){
            Dataset ds = (Dataset) keysIterator.next();

            HashMap newHashSet = new HashMap();
            scala.collection.immutable.List<Quad> quadList = (scala.collection.immutable.List<Quad>)tripleWithDataset.get(ds);

            List<Quad> listQuads = JavaConversions.asList(quadList);
            for(Quad quad : listQuads){

                HashMap tmp = new HashMap();

                Triple tr = new Triple(new URIImpl(quad.subject()), new URIImpl(quad.predicate()),
                                    constructTripleObject(quad));

                tmp.put("s", CoreUtil.convertToSPARULPattern(tr.getSubject()));
                tmp.put("p",  CoreUtil.convertToSPARULPattern(tr.getPredicate()));
                tmp.put("o",  CoreUtil.convertToSPARULPattern(tr.getObject()));

                newHashSet.put(tr.getMD5HashCode(), tmp);
            }
            JSONObject.put(ds.name(), newHashSet);
        }
    }

    private Value constructTripleObject(Quad quad){

        String Lang = "en";
        Datatype datatype = quad.datatype();

        if (datatype != null){
            if (datatype.uri().equals("http://www.w3.org/2001/XMLSchema#string"))
            {
                return new LiteralImpl(quad.value(), Lang);
            }
            else
            {
                 return new LiteralImpl(quad.value(), new URIImpl(datatype.uri()));
            }
        }
        else
        {
            return new URIImpl(quad.value());
        }
    }

    public synchronized void close() {
        try{
            String strJSONString = JSONValue.toJSONString(JSONObject);

//            String actualInsertStatement = "INSERT INTO " + TABLENAME + "(" + FIELD_OAIID + ", " +
//                FIELD_RESOURCE + " , " + FIELD_JSON_BLOB + " ) VALUES ("+ this.oaiId +
//                    ",'" + this.uri + "','" + strJSONString + "')\n";
            String separator = "\n";

//            String actualInsertStatement = String.format(rawInsertStatement, this.oaiId, this.uri, this.JSONObject);

            //Create OutputStream and OutputStreamWriter in order to be able to write strings directly
            FileOutputStream outputStream = new FileOutputStream(FILENAME, true);
//            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, "US-ASCII");
//
//            outputStreamWriter.write(actualInsertStatement);
//            outputStreamWriter.flush();
            outputStream.write(this.oaiId.getBytes());
            outputStream.write(separator.getBytes());
            outputStream.write(this.uri.toString().getBytes());
            outputStream.write(separator.getBytes());
            outputStream.write(strJSONString.getBytes());
            outputStream.write(separator.getBytes());
//            outputStream.write(actualInsertStatement.getBytes());
            outputStream.close();
//            outputStreamWriter.close();
        }
        catch (Exception exp){
            logger.error("SQL statement cannot be written to file due to " + exp.getMessage());
        }
    }
}

/*

        try{
            outputStream = new FileOutputStream(FILENAME, true);
            outputStreamWriter = new OutputStreamWriter(outputStream, "UTF-8");
        }
        catch (Exception exp){
            logger.error("SQL statements' file cannot be created due to " + exp.getMessage());
        }
        */
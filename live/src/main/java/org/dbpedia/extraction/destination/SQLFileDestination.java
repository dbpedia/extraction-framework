package org.dbpedia.extraction.destination;

import java.io.FileOutputStream;
import java.nio.channels.FileLock;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dbpedia.extraction.destinations.DBpediaDatasets;
import org.dbpedia.extraction.destinations.Dataset;
import org.dbpedia.extraction.destinations.Destination;
import scala.collection.Seq;
import scala.collection.Traversable;
import org.dbpedia.extraction.destinations.Quad;
import scala.Function1;
import scala.runtime.AbstractFunction1;
import org.dbpedia.extraction.ontology.datatypes.Datatype;
import org.dbpedia.helper.CoreUtil;
import org.dbpedia.helper.Triple;
import org.json.simple.JSONValue;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;

import scala.collection.JavaConversions;
import scala.collection.immutable.List;

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
    private static final String FILENAME = "/home/mohamed/DBpediaDumpOutput/SqlLog.sql";
    private static final Logger logger = Logger.getLogger(SQLFileDestination.class.getName());

    public Map<String, Map<String, Map<String, String>>> jsonObject = new HashMap<String, Map<String, Map<String, String>>>();
    private String oaiId;
    private URI uri;

    public SQLFileDestination(String pageTitle, String oaiID){
        this.uri = Triple.page(pageTitle);
        oaiId = oaiID;
    }

    @Override
    public void open() {}
    
    @Override
    public synchronized void write(Seq<Quad> graph){

        Function1<Quad,Dataset> quadDataset = new AbstractFunction1<Quad,Dataset>() {
          public Dataset apply(Quad quad) { return quad.dataset(); }
        };
        Map<Dataset, Traversable<Quad>> datasets = JavaConversions.mapAsJavaMap(graph.groupBy(quadDataset));
        for (Entry<Dataset, Traversable<Quad>> e : datasets.entrySet()) {
            Dataset ds = e.getKey();
            Traversable<Quad> quads = e.getValue();

            Map<String, Map<String, String>> newHashSet = new HashMap<String, Map<String, String>>();
            for(Quad quad : JavaConversions.asJavaIterable(quads.toSeq())){

                Map<String, String> tmp = new HashMap<String, String>();

                Triple tr = new Triple(new URIImpl(quad.subject()), new URIImpl(quad.predicate()),
                                    constructTripleObject(quad));

                tmp.put("s", CoreUtil.convertToSPARULPattern(tr.getSubject()));
                tmp.put("p",  CoreUtil.convertToSPARULPattern(tr.getPredicate()));
                tmp.put("o",  CoreUtil.convertToSPARULPattern(tr.getObject()));

                newHashSet.put(tr.getMD5HashCode(), tmp);
            }
            jsonObject.put(mapDatasetToExtractorID(ds), newHashSet);
        }
    }

    // TODO: remove this method when Dataset objects include a reference to the appropriate class
    private String mapDatasetToExtractorID(Dataset dataset){

        String extractorID = "";
        /*if(dataset == DBpediaDatasets.Labels())
            extractorID = "org.dbpedia.extraction.mappings.LabelExtractor";*/
        /*switch (dataset){
            case DBpediaDatasets.Labels(): extractorID = "org.dbpedia.extraction.mappings.LabelExtractor";
            break;
            case DBpediaDatasets.LinksToWikipediaArticle(): extractorID = "org.dbpedia.extraction.mappings.WikiPageExtractor";
            break;
            case DBpediaDatasets.Infoboxes(): extractorID = "org.dbpedia.extraction.mappings.InfoboxExtractor";
            break;
            case DBpediaDatasets.InfoboxProperties(): extractorID = "org.dbpedia.extraction.mappings.InfoboxExtractor";
            break;
            case DBpediaDatasets.PageLinks(): extractorID = "org.dbpedia.extraction.mappings.PageLinksExtractor";
            break;
            case DBpediaDatasets.GeoCoordinates(): extractorID = "org.dbpedia.extraction.mappings.GeoExtractor";
            break;
        } */
        if(dataset == DBpediaDatasets.Labels())
            extractorID = "org.dbpedia.extraction.mappings.LabelExtractor";
        else if(dataset == DBpediaDatasets.LinksToWikipediaArticle())
             extractorID = "org.dbpedia.extraction.mappings.WikiPageExtractor";
        else if(dataset == DBpediaDatasets.Infoboxes())
             extractorID = "org.dbpedia.extraction.mappings.InfoboxExtractor";
        else if(dataset == DBpediaDatasets.InfoboxProperties())
             extractorID = "org.dbpedia.extraction.mappings.InfoboxExtractor";
        else if(dataset == DBpediaDatasets.PageLinks())
             extractorID = "org.dbpedia.extraction.mappings.PageLinksExtractor";
        else if(dataset == DBpediaDatasets.GeoCoordinates())
             extractorID = "org.dbpedia.extraction.mappings.GeoExtractor";
        else if(dataset == DBpediaDatasets.CategoryLabels())
             extractorID = "org.dbpedia.extraction.mappings.CategoryLabelExtractor";
        else if(dataset == DBpediaDatasets.ArticleCategories())
             extractorID = "org.dbpedia.extraction.mappings.ArticleCategoriesExtractor";
        else if(dataset == DBpediaDatasets.ExternalLinks())
             extractorID = "org.dbpedia.extraction.mappings.ExternalLinksExtractor";
        else if(dataset == DBpediaDatasets.Homepages())
             extractorID = "org.dbpedia.extraction.mappings.HomepageExtractor";
        else if(dataset == DBpediaDatasets.DisambiguationLinks())
             extractorID = "org.dbpedia.extraction.mappings.DisambiguationExtractor";
        else if(dataset == DBpediaDatasets.Persondata())
             extractorID = "org.dbpedia.extraction.mappings.PersondataExtractor";
        else if(dataset == DBpediaDatasets.Pnd())
             extractorID = "org.dbpedia.extraction.mappings.PndExtractor";
        else if(dataset == DBpediaDatasets.SkosCategories())
             extractorID = "org.dbpedia.extraction.mappings.SkosCategoriesExtractor";
        else if(dataset == DBpediaDatasets.Redirects())
             extractorID = "org.dbpedia.extraction.mappings.RedirectExtractor";
        else if(dataset == DBpediaDatasets.PageIds())
             extractorID = "org.dbpedia.extraction.mappings.PageIdExtractor";
        else if(dataset == DBpediaDatasets.LongAbstracts())
             extractorID = "org.dbpedia.extraction.mappings.AbstractExtractor";
        else if(dataset == DBpediaDatasets.ShortAbstracts())
             extractorID = "org.dbpedia.extraction.mappings.AbstractExtractor";
        else if(dataset == DBpediaDatasets.Revisions())
             extractorID = "org.dbpedia.extraction.mappings.RevisionIdExtractor";
        else
            extractorID = "org.dbpedia.extraction.mappings.MappingExtractor";




        return extractorID;
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
            String strJSONString = JSONValue.toJSONString(jsonObject);

//            String actualInsertStatement = "INSERT INTO " + TABLENAME + "(" + FIELD_OAIID + ", " +
//                FIELD_RESOURCE + " , " + FIELD_JSON_BLOB + " ) VALUES ("+ this.oaiId +
//                    ",'" + this.uri + "','" + strJSONString + "')\n";
            String separator = "\n";

//            String actualInsertStatement = String.format(rawInsertStatement, this.oaiId, this.uri, this.JSONObject);

            //Create OutputStream and OutputStreamWriter in order to be able to write strings directly
            FileOutputStream outputStream = new FileOutputStream(FILENAME, true);
            FileLock fl = outputStream.getChannel().lock();
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
            outputStream.flush();
            outputStream.close();
            fl.release();
//            outputStreamWriter.close();
        }
        catch (Exception exp){
            logger.log(Level.WARNING, "SQL statement cannot be written to file", exp);
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
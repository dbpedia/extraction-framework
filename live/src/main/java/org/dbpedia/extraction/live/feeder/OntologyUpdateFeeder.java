package org.dbpedia.extraction.live.feeder;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.shared.impl.PrefixMappingImpl;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.PropertyConfigurator;
import org.dbpedia.extraction.live.util.*;
import org.dbpedia.extraction.live.util.sparql.ISparulExecutor;
import org.dbpedia.extraction.live.util.sparql.VirtuosoJdbcSparulExecutor;
import org.dbpedia.extraction.ontology.io.OntologyReader;
import org.dbpedia.extraction.sources.Source;
import org.dbpedia.extraction.sources.XMLSource;
import org.ini4j.Ini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import virtuoso.jdbc4.VirtuosoDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by IntelliJ IDEA.
 *
 * @author Mohamed Morsey, Claus Stadler
 *         Date: Jul 28, 2010
 *         Time: 2:38:54 PM
 *         This class detects the update in mappings and then gets a list of all page IDs that are affected by a change in the
 *         mapping and feeds those page IDs to a common priority pageQueue for further processing
 */
public class OntologyUpdateFeeder extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(OntologyUpdateFeeder.class);


    public static void main(String[] args)
            throws Exception {
        PropertyConfigurator.configure("log4j.ontology.properties");

        Ini ini = new Ini(new File("cfg/mappings.dbpedia.org/config.ini"));

        OntologyUpdateFeederConfig config = new OntologyUpdateFeederConfig();
        ini.get("HARVESTER").to(config);
        ini.get("BACKEND_VIRTUOSO").to(config);
        ini.get("PROPERTY_DEFINITION_EXTRACTOR").to(config);
        ini.get("LOGGING").to(config);

        File lastResponseDateFile = new File("cfg/mappings.dbpedia.org/last-response-date.txt");

        String startDate = config.isStartNow()
                ? LastResponseDateManager.getNow()
                : LastResponseDateManager.getLastResponseDate(lastResponseDateFile);

        if (startDate == null) {
            startDate = "2009-01-01T15:00:00Z";
        }


        logger.info("Loading uri namespaces");
        Model prefixModel = ModelFactory.createDefaultModel();
        InputStream in = new FileInputStream(new File("cfg/mappings.dbpedia.org/prefixes.ttl"));
        prefixModel.read(in, null, "TTL");
        in.close();
        Map<String, String> prefixMap = prefixModel.getNsPrefixMap();

        PrefixMapping prefixMapping = new PrefixMappingImpl();
        prefixMapping.setNsPrefixes(prefixMap);

        PrefixMapping tmp = new PrefixMappingBase(prefixMapping, prefixModel.getNsPrefixURI("base"));


        VirtuosoDataSource ds = new VirtuosoDataSource();
        ds.setServerName(config.getServerName());
        ds.setUser(config.getUsername());
        ds.setPassword(config.getPassword());
        ds.setPortNumber(config.getPort());
        Connection conn = ds.getConnection();

        ISparulExecutor executor = new VirtuosoJdbcSparulExecutor(conn, null);


        TBoxTripleDestination destination = new TBoxTripleDestination(executor, config.getDataGraphName(), config.getMetaGraphName(), config.getReifierPrefix());


        TBoxExtractor2 extractor = new TBoxExtractor2(tmp, config.getBaseUri(), config.getRootPrefix(), config.getExpressionPrefix(), destination);

        String mappingsOAIUri = "http://mappings.dbpedia.org/index.php/Special:OAIRepository";

        // Create an iterator which keeps polling the OAIRepository
        Iterator<Document> recordIterator =
                OAIUtil.createEndlessRecordIterator(mappingsOAIUri, startDate, 0, config.getPollInterval() * 1000, config.getSleepInterval() * 1000);


        OntologyUpdateFeeder feeder = new OntologyUpdateFeeder(recordIterator, extractor, lastResponseDateFile);

        // Let's not run as a Thread for now
        //feeder.start();

        feeder.run();
    }


    private Iterator<Document> recordIterator;
    private TBoxExtractor2 extractor;
    private File lastResponseDateFile;


    public OntologyUpdateFeeder(Iterator<Document> recordIterator, TBoxExtractor2 extractor, File lastResponseDateFile) {
        this.recordIterator = recordIterator;
        this.extractor = extractor;
        this.lastResponseDateFile = lastResponseDateFile;
    }

    public void run() {
        try {
            _run();
        } catch (Exception e) {
            logger.error("Something went wrong", e);
        }
    }


    public void _run()
            throws Exception {
        Pattern oaiPattern = Pattern.compile("<identifier>(.*)</identifier>", Pattern.MULTILINE);


        //String baseWikiUri = "http://mappings.dbpedia.org/wiki/";


        OntologyReader reader = new OntologyReader();
        while (recordIterator.hasNext()) {
            try {
                Document doc = recordIterator.next();

                //NodeToRecordTransformer transformer = new NodeToRecordTransformer(baseWikiUri, mappingsOAIUri, oaiPrefix);


                String str = XMLUtil.toString(doc);
                if (str.contains("header status=\"deleted\"")) {
                    Matcher matcher = oaiPattern.matcher(str);
                    if (matcher.find()) {
                        String oaiId = matcher.group(1);
                        Resource oai = ResourceFactory.createResource(oaiId);

                        extractor.delete(oai);
                        //System.out.println(oaiId);
                    }
                } else {

                    scala.xml.Node element = scala.xml.XML.loadString(str);
                    Source source = XMLSource.fromOAIXML((scala.xml.Elem) element);
                    extractor.handle(source);

                }

                //System.out.println(str);


                String timeStamp = XPathUtil.evalToString(doc, DBPediaXPathUtil.getOAIDatestampExpr());

                Files.createFile(lastResponseDateFile, timeStamp);

                //MappingAffectedPagesHelper.GetMappingPages(wikiPageSource, lastResponseDate);


            } catch (Exception exp) {
                logger.error(ExceptionUtils.getStackTrace(exp));
            }
        }

    }
}


// JUNK BELOW THIS LINK


/*
    public void process(String data) {

        System.out.println(data);

        scala.xml.Node element = scala.xml.XML.loadString(data);
        Source source = LiveExtractionXMLSource.fromXML((scala.xml.Elem) element);

        Model model = ModelFactory.createDefaultModel();
        //MultiMap<Resource, Model> map = extractor.readJava(source);
        //extractor.read(model, source);


        /*
        Ontology ontology = reader.read(source);
        OntologyOWLWriter writer = new OntologyOWLWriter(true);

        Object x = writer.write(ontology);

        String data = x.toString();
        InputStream in = new ByteArrayInputStream(data.getBytes());
        Model model = ModelFactory.createDefaultModel();
        model.read(in, null, "RDF/XML");

        model.write(fos, "N-TRIPLE");
        fos.flush();
        * /

        //System.out.println(x.getClass() + "\n" + x.toString());
        //System.exit(0);
//                org.dbpedia.extraction.sources.Source wikiPageSource = XMLSource.fromXML((scala.xml.Elem) element);
    }

        /*
        RDFExpression expr = RDFUtil.interpretMos("Teigware And rdf:hatBelag some rdf:Belag", tmp, "http://inner", true);

        Model model = expr.getTriples();
        model.write(System.out, "N-TRIPLE");

        System.exit(0);
*/
//File file = new File("live/testdata.dat");
//File file = new File("live/testdata-class-spaceshuttle.xml");
/*
        File file = new File("live/testdata-class-organisation.xml");
        System.out.println(file.getAbsolutePath());
        String data = Files.readFile(file);

        VirtuosoDataSource ds = new VirtuosoDataSource();
        ds.setUser("dba");
        ds.setPassword("dba");
        ds.setPortNumber(1111);
        Connection conn = ds.getConnection();


        ISparulExecutor executor = new VirtuosoJdbcSparulExecutor(conn, "http://test.org");
*/
/*
String rootPrefix = "http://dbpedia.org/ontology/";
String expressionPrefix = "http://dbpedia.org/ontology/expr";
String reifierPrefix = "http://dbpedia.org/meta/axiom";
String dataGraphName = "http://live.dbpedia.org/ontology";
String metaGraphName = "http://live.dbpedia.org/ontology/meta";
String baseUri = "http://mappings.dbpedia.org/";
*/

//RDFExpression expr = RDFUtil.interpretMos("Person", tmp, reifierPrefix, false);

/*
        TBoxTripleDestination destination = new TBoxTripleDestination(executor, dataGraphName, metaGraphName, reifierPrefix);


        TBoxExtractor2 extractor = new TBoxExtractor2(tmp, baseUri, rootPrefix, expressionPrefix, destination);

        scala.xml.Node element = scala.xml.XML.loadString(data);
        Source source = LiveExtractionXMLSource.fromXML((scala.xml.Elem) element);

        if(true) {
            extractor.handle(source);
            System.exit(0);
        }
        TBoxTripleDestination destination = new TBoxTripleDestination(executor, dataGraphName, metaGraphName, reifierPrefix);


        extractor = new TBoxExtractor2(tmp, baseUri, rootPrefix, expressionPrefix, destination);

        /*
        scala.xml.Node element = scala.xml.XML.loadString(data);
        Source source = LiveExtractionXMLSource.fromXML((scala.xml.Elem) element);

        if(true) {
            extractor.handle(source);
            System.exit(0);
        }
        /*
        String rootPrefix = "http://dbpedia.org/ontology/";
        String expressionPrefix = "http://dbpedia.org/ontology/expr";
        String reifierPrefix = "http://dbpedia.org/meta/axiom";
        String dataGraphName = "http://live.dbpedia.org/ontology";
        String metaGraphName = "http://live.dbpedia.org/ontology/meta";
        String baseUri = "http://mappings.dbpedia.org/";
        * /

        //RDFExpression expr = RDFUtil.interpretMos("Person", tmp, reifierPrefix, false);

        */

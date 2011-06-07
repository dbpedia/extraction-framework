package org.dbpedia.extraction.live.core;

import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.extraction.LiveExtractionConfigLoader;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 6, 2010
 * Time: 5:17:21 PM
 * This class represents a SPARQL end-point
 */
public class SPARQLEndpoint {

    //Initializing the Logger
    private Logger logger = Logger.getLogger(this.getClass().getName());

    String sparqlendpointURL;
	String defaultGraphURI;
	String format = "JSON";
	final int wait = 5;
	final int cutstring = 1000;

    public SPARQLEndpoint(String SparqlendpointURL, String DefaultGraphURI, String Format){
        //Assert.assertTrue("SparqlendpointURL cannot be null or empty",
//                         (SparqlendpointURL != null && SparqlendpointURL != ""));

		this.sparqlendpointURL = SparqlendpointURL;
		this.defaultGraphURI = DefaultGraphURI;
		this.format = Format;
	}

     public SPARQLEndpoint(String SparqlendpointURL, String DefaultGraphURI){
         this(SparqlendpointURL, DefaultGraphURI, "JSON");
     }

    public SPARQLEndpoint(String SparqlendpointURL){
        this(SparqlendpointURL, "");
    }

    public static SPARQLEndpoint getDefaultEndpoint(){
        String SparqlendpointURL = LiveOptions.options.get("sparqlendpoint");
        String DefaultGraphURI = LiveOptions.options.get("graphURI");

        //Assert.assertTrue("SparqlendpointURL read from options file cannot be null or empty",
//                         (SparqlendpointURL != null && SparqlendpointURL != ""));
        //Assert.assertTrue("SparqlendpointURL read from options file cannot be null or empty",
//                         (DefaultGraphURI != null && DefaultGraphURI != ""));

        return new SPARQLEndpoint(SparqlendpointURL, DefaultGraphURI);
    }

    private String _getDefaultGraphURI(String DefaultGraphURI){
        
        if((!Util.isStringNullOrEmpty(this.defaultGraphURI))&& DefaultGraphURI == null){
			return "&default-graph-uri=" + URLEncoder.encode(this.defaultGraphURI);
		} else if(!Util.isStringNullOrEmpty(DefaultGraphURI)){
			return "&default-graph-uri=" + URLEncoder.encode(DefaultGraphURI);
		}else {
			return "";
			}
	}

    private String _getDefaultGraphURI(){
        return _getDefaultGraphURI(null);
    }


    private String _getFormat(String Format){
        if((!Util.isStringNullOrEmpty(this.format))&& format == null){
			return "&format=" + this.format;
		} else if(!Util.isStringNullOrEmpty(Format)){
			return "&format=" + Format;
		}else {
			return "";
			}
	}

    private String _getFormat(){
        return _getFormat(null);
        //this.getClass();
    }

    public String executeQuery(String query,Class logComponent){
         return executeQuery(query, logComponent, null);        
    }

    public String executeQuery(String query,Class logComponent, String DefaultGraphURI){
        return executeQuery(query, logComponent, DefaultGraphURI, null);
    }


    public String executeQuery(String query,Class logComponent, String DefaultGraphURI,String Format){
        //Assert.assertTrue("SparqlendpointURL cannot be null or empty",
//                                 (query != null && query != ""));

        String sparqlendpoint  = this.sparqlendpointURL;
        String url = sparqlendpoint + "?query=";
        url += URLEncoder.encode(query);
        url += this._getDefaultGraphURI(DefaultGraphURI);
        url +=this._getFormat(Format);
        

        String contents = "";
        String timerName = logComponent + ".http_sparqlquery";

        try{
            //If the application is working in multithreading mode, we must attach the thread id to the timer name
            //to avoid the case that a thread stops the timer of another thread.
            timerName = logComponent + ".http_sparqlquery" +
                    (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");
            Timer.start(timerName);

            logger.info(logComponent + ".url = " + url);
            HttpURLConnection con = (HttpURLConnection)new URL(url).openConnection();
            con.setRequestMethod("POST");

            //TODO there is a property called CURLOPT_RETURNTRANSFER that ensures that the returned result is in string
            //TODO I think it is not needed
            con.connect();
            BufferedReader bufRdr = new BufferedReader(new InputStreamReader(con.getInputStream()));

            while(bufRdr.ready())
                contents += (char)bufRdr.read();

            if(Util.isStringNullOrEmpty(contents)){
                throw new Exception("Buffer returned from SPARQL is empty");
            }

        }
        catch(Exception exp){
            String error = "";
            error = "\nSparqlRetrieval: endpoint failure "+ sparqlendpoint + "\n";
            error +="Error: " + exp.getMessage() + "\n";
            error +="URL: " + url + "\n";
            error +="Query: " + query + "\n";
            this.logger.error(logComponent + error);
        }
        finally{
            Timer.stop(timerName);
            this.logger.info(logComponent + "returned: " + contents.length() + " of json code");
        }

        return contents;
   }


    public int executeCount(String query,Class logComponent){
         return executeCount(query, logComponent, null);
    }

    public int executeCount(String query,Class logComponent, String DefaultGraphURI){
        return executeCount(query, logComponent, DefaultGraphURI, null);
    }

    /*
	 * returns just a number
	 * $query must be like SELECT count(*) as ?count WHERE...
	 * */
    public int executeCount(String query,Class logComponent, String DefaultGraphURI,String Format){
        String timerName = logComponent + ".http.count";
        try{
            //If the application is working in multithreading mode, we must attach the thread id to the timer name
            //to avoid the case that a thread stops the timer of another thread.
            timerName = logComponent + ".http.count" +
                    (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");
            Timer.start(timerName);

            String json = this.executeQuery(query, logComponent, DefaultGraphURI, Format);

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
            HashMap jarr = (HashMap) parser.parse(json);
            Timer.stop(timerName);
    
            if((jarr.get("results") != null) && (((HashMap)jarr.get("results")).get("bindings") != null) ){
                HashMap bindings = (HashMap)((HashMap)jarr.get("results")).get("bindings");

            Iterator bindingsIterator = bindings.entrySet().iterator();
            while(bindingsIterator.hasNext()){
                HashMap one = (HashMap)bindingsIterator.next();
                HashMap count = (HashMap)one.get("count");
                return (Integer)count.get("value");
                }
            }
            else
                return 0;
        }
        catch (Exception exp){
        }
        return 0;
    }

    public void test(){
        String query = "SELECT count(*) as ?count WHERE {<http://dbpedia.org/resource/London> ?p ?o .}";
        int c =  this.executeCount(query, this.getClass());
        System.out.println(c + "\n");
        if(c>5){
             System.out.println("bigger than 5" + "\n");
            }
        System.exit(0);
    }

}

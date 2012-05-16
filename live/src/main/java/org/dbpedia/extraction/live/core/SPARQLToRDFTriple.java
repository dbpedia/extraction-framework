package org.dbpedia.extraction.live.core;

import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.extraction.LiveExtractionConfigLoader;
import org.json.simple.parser.ContainerFactory;
import org.json.simple.parser.JSONParser;
import org.openrdf.model.Literal;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.rio.ntriples.NTriplesUtil;

import java.util.*;


/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 8, 2010
 * Time: 4:53:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class SPARQLToRDFTriple {
    //Initializing the Logger
    private Logger logger = Logger.getLogger(this.getClass().getName());
    
    private URI subject;
	private String language;
	private SPARQLEndpoint sparqlEndpoint;
	private JDBC jdbc;
	private String use = null;

	public SPARQLToRDFTriple(URI Subject, String Language){
        subject = Subject;
        language = Language;
        this.use = LiveOptions.options.get("Sparql.use");
        if(this.use.equals("jdbc")){
            this.jdbc = JDBC.getDefaultConnection();
        }else{
            this.sparqlEndpoint = SPARQLEndpoint.getDefaultEndpoint();
        }
    }

/**
    * sub,pred,obj, must be an array, either:
	* [action] = "fix"
	* [value]  = "http://dbpedia.org/resource/subject"
	* or a sparql variable ?o like:
	* [action] = "variable"
	* [value]  = "o"
	* * or use $this->subject:
	* [action] = "classattribute"
	* [value]  = null
	* */
    public ArrayList getRDFTriples(String query, HashMap subject, HashMap predicate, HashMap object){
        return getRDFTriples(query, subject, predicate, object, true);
    }

    public ArrayList getRDFTriples(String query, HashMap subject, HashMap predicate, HashMap object, boolean filterlanguage){

        if(subject.get("action").toString().equals("fix")){
            URI s = new URIImpl(subject.get("value").toString());
        }else if(subject.get("action").equals("classattribute")){
            URI s = this.subject;
        }
        if(predicate.get("action").equals("fix")){
            URI p = new URIImpl(predicate.get("value").toString());
        }
        if(object.get("action").equals("fix")){
            try {
            URI o = new URIImpl(object.get("value").toString());
            }catch(Exception exp) {
                logger.warn("object = fix only for uris currently");
            return new ArrayList();
            }
        }

        ArrayList result = new ArrayList();
        HashMap jarr = new HashMap();

        boolean pass = false;
        String json = "";
        if(this.use.equals("jdbc")){
            //If the application is working in multithreading mode, we must attach the thread id to the timer name
            //to avoid the case that a thread stops the timer of another thread.
            String timerName = this.getClass() + ":jdbc_request" +
                    (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");
            Timer.start(timerName);

             jarr = this.jdbc.execAsJson(query, "SPARQLToRDFTriple");
            Timer.stop(timerName);
        }else{
            //If the application is working in multithreading mode, we must attach the thread id to the timer name
            //to avoid the case that a thread stops the timer of another thread.
            String timerName = this.getClass() + ":http_request" +
                    (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

            Timer.start(timerName);
            json = this.sparqlEndpoint.executeQuery(query, this.getClass());

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

            try{
                jarr = (HashMap)parser.parse(json);
            }
            catch(Exception exp){
                this.logger.warn("Unable to parse JSON: " + exp.getMessage());
                return new ArrayList();
            }
            //$jarr = json_decode($json, true);
            if(jarr.get("results") != null){
                jarr = (HashMap)jarr.get("results");
                }
            //Timer.stop(this.getClass() + ":http_request");
            Timer.stop(timerName);
        }

        URI s = null, p = null;
        Value o = null;

        //print_r($result);
        if(jarr.get("bindings")!=null ){
            ArrayList bindings = (ArrayList)jarr.get("bindings");
            for(Object objBinding:bindings ){
                HashMap one = (HashMap)objBinding;
                try{
                    if(!Util.isStringNullOrEmpty(subject.get("action").toString())){
                        if(subject.get("action").toString().equals("fix")){
                            //$s is already set
                            }
                        else if(subject.get("action").toString().equals("variable")){
                            HashMap subHashmap = (HashMap)one.get(subject.get("value"));
                            s = new URIImpl(subHashmap.get("value").toString());
                            }
                        else if(subject.get("action").toString().equals("classattribute")){
                            //$s is already set
                            }
                    }
                    if(!Util.isStringNullOrEmpty(predicate.get("action").toString())){
                        if(predicate.get("action").toString().equals("fix")){
                            //$p is already set
                            }
                        else if(predicate.get("action").toString().equals("variable")){
                            HashMap predHashmap = (HashMap)one.get(predicate.get("value"));
                            p = new URIImpl(predHashmap.get("value").toString());
                            }
                    }
                    if(!Util.isStringNullOrEmpty(object.get("action").toString())){
                         if(object.get("action").toString().equals("fix")){
                            //$o is already set
                            }
                        else if(object.get("action").toString().equals("variable")){
                            HashMap unknown = (HashMap)one.get(object.get("value"));
                            o = this.toObject(unknown, filterlanguage);
                            if(o == null) {continue;	}
                            }
                    }

                    this.logger.info("*******************");

                    this.logger.info(NTriplesUtil.toNTriplesString(s));
                    this.logger.info(NTriplesUtil.toNTriplesString(p));
                    this.logger.info(NTriplesUtil.toNTriplesString(o));
                    result.add(new RDFTriple(s, p, o));
                    //$this->log(DEBUG, $t->getObject()->toNTriples());
                }catch(Exception exp){
                    this.logger.warn("Found invalid URI: " + exp.getMessage());
                    }
                }
         }else{
            this.logger.warn(json);
             }
        return result;
   }

    public  ArrayList getRDFTriples(String query){
        URI s = this.subject;
        ArrayList result = new ArrayList();

        //If the application is working in multithreading mode, we must attach the thread id to the timer name
        //to avoid the case that a thread stops the timer of another thread.
        String timerName = this.getClass() + ":http_request" +
                (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

        Timer.start(timerName);
        String json = this.sparqlEndpoint.executeQuery(query, this.getClass());
        Timer.stop(timerName);

        //jarr = json_decode(json, true);
        JSONParser parser = new JSONParser();
        HashMap jarr = new HashMap(); 

        //This class is object is created to force the JSON decoder to return a HashMap, as hashesFromStore is a HashMap
            ContainerFactory containerFactory = new ContainerFactory(){
                public List creatArrayContainer() {
                  return new LinkedList();
                }

                public Map createObjectContainer() {
                  return new HashMap();
                }

              };

            try{
                jarr = (HashMap)parser.parse(json);
            }
            catch(Exception exp){
                this.logger.warn("Unable to parse JSON: " + exp.getMessage());
                return new ArrayList();
            }


        //if(isset(jarr["results"]) && isset(jarr["results"]["bindings"]) )
        if((jarr.get("results")!=null) && (((HashMap)jarr.get("results")).get("bindings") != null)){
            ArrayList bindings = (ArrayList)((HashMap)jarr.get("results")).get("bindings");
            for(Object objBinding : bindings){
                HashMap one = (HashMap)objBinding;
                try{
                URI p = new URIImpl(((HashMap)one.get("p")).get("value").toString());
                HashMap unknown = (HashMap)one.get("o");
                Object o = this.toObject(unknown);
                if(o == null) {
                    continue;
                }

                this.logger.info(NTriplesUtil.toNTriplesString(s));
                this.logger.info(NTriplesUtil.toNTriplesString(p));
                this.logger.info(NTriplesUtil.toNTriplesString(new URIImpl(o.toString())));
                result.add(new RDFTriple(s, p, new URIImpl(o.toString())));
                }catch(Exception ex){
                    this.logger.warn("found invalid URI: " + ex.getMessage());
                    }
            }
         }
        return result;
    }


    public Value toObject(HashMap unknown){
        
        return toObject(unknown, true);
    }

    public Value toObject(HashMap unknown, boolean filterlanguage){
			if(unknown.get("type").equals("uri")){
				return new URIImpl(unknown.get("value").toString());
			}else if (unknown.get("type").equals("literal")){
				if(unknown.get("xml:lang") != null){
					if(unknown.get("xml:lang").equals(this.language)){
                        return new LiteralImpl(unknown.get("value").toString(), unknown.get("xml:lang").toString());
						//return new RDFliteral(unknown["value"], null,unknown["xml:lang"] );
					}else if(!filterlanguage) {
                        return new LiteralImpl(unknown.get("value").toString(), unknown.get("xml:lang").toString());
						//return new RDFliteral(unknown["value"], null,unknown["xml:lang"] );
					}
				}else {
                    return new LiteralImpl(unknown.get("value").toString());
					//return new RDFliteral(	unknown["value"],  null,  null);
				}
			}else if (unknown.get("type").equals("typed-literal")){
                return new LiteralImpl(unknown.get("value").toString(), new URIImpl(unknown.get("datatype").toString()));
				//return new RDFliteral(unknown["value"], unknown["datatype"], null );
			}else{
				System.out.println("tail in SPARQLToRDFTriple.toObject ");
                System.exit(1);

				}
            return null;
		}

//	public  function log(lvl, message){
//
//				Logger::logComponent("destination", get_class(this), lvl , message);
//		}



}

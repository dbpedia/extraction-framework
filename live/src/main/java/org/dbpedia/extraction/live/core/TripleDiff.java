package org.dbpedia.extraction.live.core;

import java.util.ArrayList;
import java.util.HashMap;


import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.extraction.LiveExtractionConfigLoader;
import org.dbpedia.extraction.live.helper.MatchPattern;
import org.dbpedia.extraction.live.helper.MatchType;
import org.openrdf.model.*;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed MOrsey
 * Date: Jul 7, 2010
 * Time: 4:56:01 PM
 * This class provides the functionality of checking the difference between tow sets of triples
 */
public class TripleDiff {

    private static Logger logger ;
    private URI resource;
    private String language;
    private ArrayList<MatchPattern> producesFilterList = new ArrayList();
    private SPARQLToRDFTriple store;

    public TripleDiff(URI Resource, String Language,  ArrayList<MatchPattern> ProducesFilterList, SPARQLToRDFTriple Store){
        try{
            logger = Logger.getLogger(Class.forName("TripleDiff").getName());
            this.resource = Resource;
            this.language = Language;
            this.producesFilterList = ProducesFilterList;
            this.store = Store;
        }
        catch(Exception exp){

        }
    }

    public HashMap diff(ArrayList triplesFromExtractor){
		String query = this.createSPARQLQuery();
		ArrayList triplesFromStore = this.store.getRDFTriples(query);
		//print_r(this.resource);die;
		HashMap result = new HashMap();
		ArrayList filteredoutExtractor = new ArrayList(); //should be inserted
		ArrayList filteredoutStore = new ArrayList(); //should be deleted, special handling of object
		ArrayList remainderExtractor = new ArrayList();
		ArrayList remainderStore = new ArrayList();
		ArrayList insert = new ArrayList();
		ArrayList delete = new ArrayList();
		ArrayList untouched_keys = new ArrayList();
		ArrayList intersected = new ArrayList();

        //If the application is working in multithreading mode, we must attach the thread id to the timer name
        //to avoid the case that a thread stops the timer of another thread.
        String timerName = "TripleDiff" +
                (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");
		Timer.start(timerName);
		// this is a first loop that filters all subject which are not the same as
		// this article
		// all filtered objects are remembered
		//echo count(triplesFromExtractor)."\n";
		for (Object objTriplesFromExtractor :triplesFromExtractor ){
            RDFTriple triple = (RDFTriple) objTriplesFromExtractor;
			//filter all which do not have resource as subject
			if(!this.resource.equals( triple.getSubject())){
				filteredoutExtractor.add(triple);
			}else if((triple.getObject() instanceof URI) && (isSubstring(this.resource.toString(),triple.getObject().toString()))) {
				filteredoutExtractor.add(triple);
			}else {
				remainderExtractor.add(triple);
			}
		}// end for each

		for (Object objTriplesFromStore :triplesFromStore) {
            RDFTriple triple = (RDFTriple) objTriplesFromStore;
			//filter all which do not have resource as subject
			if((triple.getObject() instanceof URI) && (isSubstring(this.resource.toString(),triple.getObject().toString()))){
				filteredoutStore.add(triple);
			}else {
				remainderStore.add(triple);
			}
		}// end for each
			//echo count(remainderExtractor)."\n";

        //i.e. property names for both
        ArrayList keysFromExt = this.getPropertyNames(remainderExtractor);
        ArrayList keysFromStore = this.getPropertyNames(remainderStore);
        untouched_keys = array_diff(keysFromStore,  keysFromExt );

        //new properties
        //case 1 new property, just add
        ArrayList toBeAdded = array_diff(keysFromExt, keysFromStore);
        //print_r(toBeAdded);die;
        ArrayList newRemainderExtractor = new ArrayList();
        for(Object objRemainderExtractor : remainderExtractor){

            RDFTriple triple = (RDFTriple)objRemainderExtractor;

            if(in_array(triple.getPredicate().toString(), toBeAdded) ){
                insert.add(triple);
            }else{
                newRemainderExtractor.add(triple);
                }
            }

        remainderExtractor = newRemainderExtractor;
        //echo count(remainderExtractor)."\n";


        // update existing properties
        // case 2 property known delete, insert
        // keep all from languages other than en
        ArrayList tobedeleted = array_intersect(keysFromExt, keysFromStore) ;
        ArrayList newRemainderStore = new ArrayList();
        newRemainderExtractor = new ArrayList();

        //print_r(tobedeleted);die;

        for(Object objRemainderStore : remainderStore){
            RDFTriple triple = (RDFTriple)objRemainderStore;
            if(tobedeleted.contains(triple.getPredicate().toString())){
                    delete.add(triple);
            }else{
                newRemainderStore.add(triple);
            }
        }
        for(Object objRemainderExtractor : remainderExtractor){
            RDFTriple triple = (RDFTriple)objRemainderExtractor;
            if(tobedeleted.contains(triple.getPredicate().toString())){
                insert.add(triple);
            }else{
                newRemainderExtractor.add(triple);
            }
        }
        remainderExtractor = newRemainderExtractor;
        remainderStore = newRemainderStore;

		//Validation:
		if((remainderStore.size() > 0) || (remainderExtractor.size() > 0)){
            this.logger.warn(" remaining triples:  " + remainderStore.size() + "|" + remainderExtractor.size());

            String tmp="";
            for(Object objTriplesFromStore : triplesFromStore){
                RDFTriple triple = (RDFTriple) objTriplesFromStore;
                    tmp += triple.toNTriples() + "";
                }
            //this.log(WARN, " from store:  \n".tmp);
            tmp="";
            for(Object objTriplesFromExtractor :  triplesFromExtractor){
                RDFTriple triple = (RDFTriple) objTriplesFromExtractor;
                tmp += triple.toNTriples() + "";
                }
            //this.log(WARN, " from extractor:  \n".tmp);
            tmp="";
            for(Object objTriplesRemainderStore :  remainderStore){
                RDFTriple triple = (RDFTriple) objTriplesRemainderStore;
                tmp += triple.toNTriples() + "";
                }
            this.logger.info(" left from store:  \n" + tmp);

            tmp="";
            for(Object objRemainderExtractor :  remainderExtractor){
                RDFTriple triple = (RDFTriple) objRemainderExtractor;
                    tmp += triple.toNTriples() + "";
                }
            this.logger.info(" left from extactor:  \n" + tmp);
        }


		result.put("filteredoutExtractor",filteredoutExtractor);
		result.put("filteredoutStore", filteredoutStore);

		result.put("remainderStore", remainderStore);
		result.put("insert", insert);
		result.put("delete", delete);

		Timer.stop(timerName);

		return result;
	}

    public HashMap simplerDiff(ArrayList triplesFromExtractor){
        String query = this.createSPARQLQuery();
		//echo query;die;
	 /**
    * sub,pred,obj, must be an array, either:
	* [action] = "fix"
	* [value]  = "http://dbpedia.org/resource/subject"
	* or a sparql variable ?o like:
	* [action] = "variable"
	* [value]  = "o"
	* * or use this.subject:
	* [action] = "classattribute"
	* [value]  = null
	* */
/*
		pvar = "?p";
		ovar = "?o";
		query = "SELECT * WHERE {<".this->resource->getURI()."> ".pvar." ".ovar. " \n";
*/
        HashMap s = new HashMap();
        HashMap p = new HashMap();
        HashMap o = new HashMap();

        s.put("action", "classattribute");
        s.put("value", null);

        p.put("action", "variable");
        p.put("value", "p");

        o.put("action", "variable");
        o.put("value", "o");

		ArrayList triplesFromStore = this.store.getRDFTriples(query, s, p, o);
		HashMap result = new HashMap();
		ArrayList differentSubjectExtractor = new ArrayList(); //should be inserted
		ArrayList subResourceAsObjectExtractor = new ArrayList(); //should be inserted
		ArrayList subResourceAsObjectStore = new ArrayList(); //should be deleted, special handling of object
		ArrayList remainderExtractor = new ArrayList();
		ArrayList remainderStore = new ArrayList();
		ArrayList insert = new ArrayList();
		ArrayList delete = new ArrayList();
		ArrayList untouched_keys = new ArrayList();
		ArrayList intersected = new ArrayList();

        //If the application is working in multithreading mode, we must attach the thread id to the timer name
        //to avoid the case that a thread stops the timer of another thread.
        String timerName = "TripleDiff" +
                (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

		Timer.start(timerName);
		// this is a first loop that filters all subject which are not the same as
		// this article
		// all filtered objects are remembered
		//echo count($triplesFromExtractor)."\n";
		for(Object objTriplesFromExtractor : triplesFromExtractor) {
            RDFTriple triple = (RDFTriple) objTriplesFromExtractor;

			//filter all which do not have resource as subject
			if(!this.resource.equals( triple.getSubject())){
				differentSubjectExtractor.add(triple);
			// filter out London/review/rating in Object
			}else if((triple.getObject() instanceof URI) && isSubstring(this.resource.toString(),triple.getObject().toString())) {
				subResourceAsObjectExtractor.add(triple);
			}else {
				remainderExtractor.add(triple);
			}
		}// end for each


		for(Object obTtriplesFromStore :  triplesFromStore){
            RDFTriple triple = (RDFTriple) obTtriplesFromStore;
			//filter all which do not have resource as subject
			if((triple.getObject() instanceof URI) && isSubstring(this.resource.toString(),triple.getObject().toString())){
				subResourceAsObjectStore.add(triple);
			}else {
				remainderStore.add(triple);
			}
		}// end for each

		/*
		 * For debugging
		 * remove later
		 *
		 * */
		for (Object objDifferentSubjectExtractor : differentSubjectExtractor){
            RDFTriple triple = (RDFTriple) objDifferentSubjectExtractor;
			if(!isSubstring(this.resource.toString(),triple.getSubject().toString())) {
				this.logger.warn( "Found :\n" + triple.toNTriples());
				}
		}//end for each

		result.put("subResourceAsObjectStore", subResourceAsObjectStore);

		//result['subResourceAsObjectExtractor'] = $subResourceAsObjectExtractor;
		//$result['differentSubjectExtractor'] = $differentSubjectExtractor;

		result.put("triplesFromStore", triplesFromStore);
		//$result['remainderStore'] = $remainderStore ;
		//$result['remainderExtractor'] = $remainderStore ;

		Timer.stop(timerName);

		return result;
	}

    public static boolean isSubstring (String small, String big){
        return ((big.indexOf(small) == -1) && (big.length() > small.length()));
    }

    private ArrayList getPropertyNames(ArrayList triples){
        ArrayList result = new ArrayList();

        for(Object objTriple : triples){
            RDFTriple triple = (RDFTriple)objTriple;
                result.add(triple.getPredicate());
            }
        return result;
    }

    //        private  function log($lvl, $message){
//
//                    Logger::logComponent('destination', get_class($this), $lvl , $message);
//            }
//
//
    private String createSPARQLQuery(){
        String pvar = "?p";
        String ovar = "?o";
        String query = "SELECT * WHERE {<" + this.resource + "> " + pvar + " " + ovar + " \n";
        String tmpFilter = createFilter(this.producesFilterList);
        tmpFilter = (tmpFilter.trim().length() > 0) ? "FILTER( \n" + tmpFilter + "). " : " ";
        query  += tmpFilter + "}";
        return query;
    }

    public static String createFilter(ArrayList<MatchPattern> ProducesFilterList){
        return createFilter(ProducesFilterList, "?p");
    }

    public static String createFilter(ArrayList<MatchPattern> ProducesFilterList, String pVar){
        return createFilter(ProducesFilterList, pVar, "?o");
    }

    public static String createFilter(ArrayList<MatchPattern> ProducesFilterList, String pVar, String oVar) {
		ArrayList piris = new ArrayList();
		ArrayList oiris = new ArrayList();

		ArrayList terms = new ArrayList();
		for (MatchPattern rule : ProducesFilterList){
//            HashMap rule = (HashMap)objRule;
			boolean error = false;
			if(rule.type == MatchType.STARTSWITH){
					if((rule.predicate != null) && !Util.isStringNullOrEmpty(rule.predicate)
                            && (rule.object != null) && !Util.isStringNullOrEmpty(rule.object)) {

						if(rule.pexact == true){
							String t1 =  pVar + " !=  <" +  rule.predicate + "> ";
							String t2 = notcurrent(oVar, rule.object);

                            ArrayList tempArray = new ArrayList();
                            tempArray.add(t1);
                            tempArray.add(t2);

							terms.add(assembleTerms(tempArray, "||"));
						}
                        else{
							String t1 = notcurrent(pVar, rule.predicate);
							String t2 = notcurrent(oVar, rule.object);

                            ArrayList tempArray = new ArrayList();
                            tempArray.add(t1);
                            tempArray.add(t2);

							terms.add(assembleTerms(tempArray, "||"));
						}
					}
                    else if((rule.predicate != null ) && (!Util.isStringNullOrEmpty(rule.predicate))){
						terms.add(notcurrent(pVar, rule.predicate));
					}
                    else if((rule.object != null ) && (!Util.isStringNullOrEmpty(rule.object))){
						terms.add(notcurrent(oVar, rule.object));
					}
                    else{
						error = true;
					}
			}
            else if(rule.type == MatchType.EXACT){
					if((rule.predicate != null) && !Util.isStringNullOrEmpty(rule.predicate)
                            && (rule.object != null) && !Util.isStringNullOrEmpty(rule.object)){
						String t1 = pVar + " !=  <" + rule.predicate + ">";
						String t2 = oVar + " !=  <" + rule.object + ">";

                        ArrayList tempArray = new ArrayList();
                        tempArray.add(t1);
                        tempArray.add(t2);

						terms.add(assembleTerms(tempArray, "||"));

					}else if((rule.predicate != null ) && (!Util.isStringNullOrEmpty(rule.predicate))){
							piris.add("\n<" + rule.predicate + ">");
					}else if((rule.object != null ) && (!Util.isStringNullOrEmpty(rule.object ))){
							oiris.add("\n<" + rule.object + ">");
					}else {
						error = true;
					}
			}

			if(error) {
				logger.error("TripleDiff: Uninterpretable filter in one Extractor ");
                //TODO the following code is not converted
//				ob_start();
//				// write content
//				print_r($rule);
//				$content = ob_get_contents();
//				ob_end_clean();
				logger.error("\n" + rule.toString());
                System.exit(1);
			}
		}
		if(piris.size() > 0){
            //TODO piris is handled as if it is an array
            //$terms[] ='!('.$pVar.' in ( '.implode(",", $piris).'))';
			terms.add("!(" + pVar + " in ( " + implode(piris, ",") + "))");
		}
		if(oiris.size() > 0){
			terms.add("!(" + oVar + " in ( " + implode(oiris, ",") +  "))");
		}

		//FILTER (!(p in (<http://www.w3.org/...>, <second IRI>, <third IRI>...))

		return assembleTerms(terms, "&&");
	}

    private static String implode(ArrayList ary, String delimiter) {
        String out = "";
        for(int i=0; i<ary.size(); i++) {
            if(i!=0) { out += delimiter; }
            out += ary.get(i).toString();
        }
        return out;
    }

    public static String assembleTerms(ArrayList terms, String op) {
		if(!(op.equals("&&") || op.equals("||"))){
            System.out.println("wrong operator in assembleTerms TripleDiff.java " + op);
            System.exit(1);
        }
		String retval = "";

        //If the application is working in multithreading mode, we must attach the thread id to the timer name
        //to avoid the case that a thread stops the timer of another thread.
        String timerName = "TripleDiff.assembleTerms" +
                (LiveExtractionConfigLoader.isMultithreading()? Thread.currentThread().getId():"");

		Timer.start(timerName);
        
		if (terms.size() == 0)
			retval = "";
		else if (terms.size() == 1)
			retval = "(" + terms.get(0) + ")";
		else {
			//op = "&&";
			String ret="";
			for(Object objTerm : terms){
                String one = (String)objTerm;
				if(ret.length() == 0){
					ret += "(" + one + ")";
					}
				else{
					ret  += "\n" + op;
					ret += "(" + one +")";
				}
			}

			retval = "(" + ret +")";
		}
		Timer.stop(timerName);
		return retval;
	}

    public static String notregex(String var, String f){
        return  "!regex(str(" + var + "), '^" + f + "')";
    }

	public static String notlike(String var, String f){
        return  "!( " + var + " LIKE <" + f + "%> )";
    }

	public static String notcurrent(String var, String f){
        return notlike(var, f);
    }

    private ArrayList array_diff(ArrayList arr1, ArrayList arr2){
        ArrayList arrResult = new ArrayList();

        for(Object obj: arr1){
            if(!arr2.contains(obj))
                arrResult.add(obj);
        }
        return arrResult;
    }

    private boolean in_array(String requiredString, ArrayList arr){
        return arr.contains(requiredString);
    }

    private ArrayList array_intersect(ArrayList arr1, ArrayList arr2){
        ArrayList arrResult = new ArrayList();

        for(Object obj: arr1){
            if(arr2.contains(obj))
                arrResult.add(obj);
        }
        return arrResult;
    }

}


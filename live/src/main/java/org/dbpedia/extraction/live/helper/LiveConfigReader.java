package org.dbpedia.extraction.live.helper;

import org.apache.log4j.Logger;
import org.apache.xerces.parsers.DOMParser;
import org.dbpedia.extraction.live.core.Constants;
import org.dbpedia.extraction.live.core.Util;
import org.dbpedia.extraction.mappings.ArticleCategoriesExtractor;
import org.dbpedia.extraction.mappings.Extractor;
import org.dbpedia.extraction.mappings.SkosCategoriesExtractor;
import org.dbpedia.extraction.util.Language;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by IntelliJ IDEA.
 * User: mabrouk
 * Date: Jul 30, 2010
 * Time: 3:56:55 PM
 * This class reads the configuration file of the live extraction.
 */
public class LiveConfigReader {

    private static Logger logger = Logger.getLogger(LiveConfigReader.class);
    private static DOMParser parser = new DOMParser();
    private static final String liveConfigFile = "./live/live.xml";

    private static DocumentBuilderFactory dbFactory;
    private static DocumentBuilder dBuilder;
    private static Document doc;



    //Tag names that are use in live.config file
    private static final String EXTRACTOR_TAGNAME = "extractor";
    private static final String LANUAGE_TAGNAME = "language";
    private static final String MULTITHREADING_MODE_TAGNAME = "multihreadingMode";
    private static final String UPDATE_ONTOLGY_AND_MAPPINGS_PERIOD_TAGNAME = "updateOntologyAndMappingsPeriod";

    private static final String NAME_ATTRIBUTENAME = "name";
    private static final String EXTRACTOR_STATUS_ATTRIBUTENAME = "status";

    private static final String MATCH_PATTERN_TAGNAME = "matchPattern";
    private static final String MATCH_PATTERN_TYPE_ATTRIBUTENAME = "type";
    private static final String PEXACT_ATTRIBUTENAME = "pexact";

    private static final String SUBJECT_TAGNAME = "s";
    private static final String PREDICATE_TAGNAME = "p";
    private static final String OBJECT_TAGNAME = "o";
    private static final String NOTICE_TAGNAME = "notice";


    public static Map<Language,List<ExtractorSpecification>>  extractors = null;

    public static Map<Language,List<Class<Extractor>>> extractorClasses = null;
    public static int updateOntologyAndMappingsPeriod = 5;
    public static boolean multihreadingMode = false;

    //Initialize the static members
    static{
        try{

            dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(new File(liveConfigFile));
            readExtractors();
            readMultihreadingMode();
            readUpdateOntologyAndMappingsPeriod();


            /** Ontology source */
//            JavaConversions.asEnumeration(WikiTitle.Namespace());
//    Source ontologySource = WikiSource.fromNamespaces(Set(WikiTitle.Namespace().OntologyClass, WikiTitle.Namespace.OntologyProperty),
//                                                   new URL("http://mappings.dbpedia.org/api.php"), Language.Default() );
//
//    /** Mappings source */
//    Source mappingsSource =  WikiSource.fromNamespaces(Set(WikiTitle.Namespace.Mapping),
//                                                    new URL("http://mappings.dbpedia.org/api.php"), Language.Default() );
        }
        catch(Exception exp){
            logger.error(exp.getMessage());
        }
    }

    /**
     * Reads the value indicating whether the application should work in multithreading or single threading mode
     */
    private static void readMultihreadingMode() {
        multihreadingMode = Boolean.parseBoolean(doc.getElementsByTagName(MULTITHREADING_MODE_TAGNAME).item(0).getTextContent());
    }

    /**
     * Reads the period between each reload of ontology and mappings
     */
    private static void readUpdateOntologyAndMappingsPeriod() {
        updateOntologyAndMappingsPeriod = Integer.parseInt(
                doc.getElementsByTagName(UPDATE_ONTOLGY_AND_MAPPINGS_PERIOD_TAGNAME).item(0).getTextContent());
    }

    /**
     * Reads each langauge along with its set of extractors
     */
    private static void readExtractors(){
        NodeList languageNodes = doc.getElementsByTagName(LANUAGE_TAGNAME);
        //iterate and build the required list of extractors
        extractors = new HashMap<Language,List<ExtractorSpecification>>();
        extractorClasses = new HashMap<Language,List<Class<Extractor>>>();


        for(int i=0; i<languageNodes.getLength(); i++){

            Element elemLanguage = (Element)languageNodes.item(i);
            String languageName = elemLanguage.getAttribute(NAME_ATTRIBUTENAME);
            Language language = Language.apply(languageName);
            readLanguageExtractors(elemLanguage, language);
        }
    }

    /**
     * Gets the list of extractors specified in the config file along with the status of each extractor
     * @param   elemLanguageExtractors  The XML element containing the extractors of a language
     * @param   lang    The language code 
     * */
    private static void readLanguageExtractors(Element elemLanguageExtractors, Language lang){
        try{
            NodeList extractorNodes = elemLanguageExtractors.getElementsByTagName(EXTRACTOR_TAGNAME);
            ArrayList<ExtractorSpecification> langExtractors = new ArrayList<ExtractorSpecification>(10);
            ArrayList<Class<Extractor>> langExtractorClasses = new ArrayList<Class<Extractor>>(10);

            //iterate and build the required list of extractors
            for(int i=0; i<extractorNodes.getLength(); i++){
                MatchPattern extractorSpecificPattern = null;
                Element elemExtractor = (Element)extractorNodes.item(i);
                String extractorID = elemExtractor.getAttribute(NAME_ATTRIBUTENAME);
                ExtractorStatus status = ExtractorStatus.valueOf(elemExtractor.getAttribute(EXTRACTOR_STATUS_ATTRIBUTENAME));

                langExtractorClasses.add((Class<Extractor>)(ClassLoader.getSystemClassLoader().loadClass(extractorID)));

                //Those types of extractors need special type of handling as we must call the function _addGenerics for
                //them
                if((extractorID.equals(SkosCategoriesExtractor.class.toString())) ||
                        (extractorID.equals(ArticleCategoriesExtractor.class.toString())))
                    extractorSpecificPattern = _addGenerics(lang, extractorID);

                ArrayList<MatchPattern> patternsList = _getExtractorMatchPatterns(elemExtractor);

                if(extractorSpecificPattern != null)
                    patternsList.add(extractorSpecificPattern);

                //Construct the extractor specification object and adds it to the extractors list
                langExtractors.add(new ExtractorSpecification(extractorID, status,
                        patternsList, _getExtractorNotices(elemExtractor)));

                extractors.put(lang, langExtractors);
                extractorClasses.put(lang, langExtractorClasses);
            }

//            LiveExtractionConfigLoader.convertExtractorListToScalaList(extractorClasses);
            System.out.println(extractors);

        }
        catch(Exception exp){
            logger.error(exp.getMessage());
        }

    }

    /**
     * Loads the generic match patterns for some extractors e.g. SkosCategoriesExtractor, because those extractors
     * need a specific pattern for language specific category 
     * @param lang  The required language
     * @param extractorID   The ID of the required extractor
     * @return  The match pattern suitable for the passed extractor
     */
    private static MatchPattern _addGenerics(Language lang, String extractorID) {
        MatchPattern pattern = null;
        if(extractorID.equals(SkosCategoriesExtractor.class.toString())){
            pattern = new MatchPattern(MatchType.STARTSWITH, "", Constants.SKOS_BROADER,
                    ((HashMap)Util.MEDIAWIKI_NAMESPACES.get(lang)).get(Constants.MW_CATEGORY_NAMESPACE).toString() , true);

        }
        else if(extractorID.equals(ArticleCategoriesExtractor.class.toString())){
            pattern = new MatchPattern(MatchType.STARTSWITH, "", Constants.SKOS_SUBJECT,
                    ((HashMap)Util.MEDIAWIKI_NAMESPACES.get(lang)).get(Constants.MW_CATEGORY_NAMESPACE).toString() , true);
        }

        return pattern;
    }

    /**
     * Constructs a list of match patterns associated with the passed extractor 
     * @param extractorElem XML element containing the full specification of the extractor
     * @return  A list of patterns of the extractor
     */
    private static ArrayList<MatchPattern> _getExtractorMatchPatterns(Element extractorElem){
        ArrayList<MatchPattern> patterns = new ArrayList<MatchPattern>();
        NodeList patternNodes = extractorElem.getElementsByTagName(MATCH_PATTERN_TAGNAME);
        try{
            for(int i=0; i<patternNodes.getLength(); i++){
                Element elemPattern = (Element)patternNodes.item(i);

                MatchType type = MatchType.valueOf(elemPattern.getAttribute(MATCH_PATTERN_TYPE_ATTRIBUTENAME));
                boolean pexact = Boolean.parseBoolean(elemPattern.getAttribute(PEXACT_ATTRIBUTENAME));

                String subject = elemPattern.getElementsByTagName(SUBJECT_TAGNAME).item(0).getTextContent();

                //Since we are using name like RDFS_LABEL in the live.xml file, then we should use the reflection
                //to get its actual string value from the Constants class
                try{
                    if(Constants.class.getField(subject).get(Constants.class) != null)
                        subject = Constants.class.getField(subject).get(Constants.class).toString();
                }
                catch(Exception exp){}

                String predicate = elemPattern.getElementsByTagName(PREDICATE_TAGNAME).item(0).getTextContent();

                try{
                    if(Constants.class.getField(predicate).get(Constants.class) != null)
                        predicate = Constants.class.getField(predicate).get(Constants.class).toString();
                }
                catch(Exception exp){}


                String object = elemPattern.getElementsByTagName(OBJECT_TAGNAME).item(0).getTextContent();
                try{
                    if(Constants.class.getField(object).get(Constants.class) != null)
                        object = Constants.class.getField(object).get(Constants.class).toString();
                }
                catch(Exception exp){}

                patterns.add(new MatchPattern(type, subject, predicate, object, pexact));
            }
        }
        catch(Exception exp){

        }



        return patterns.size()>0? patterns : null;
    }

    /**
     * Constructs a list of notices associated with the passed extractor  
     * @param extractorElem extractorElem XML element containing the full specification of the extractor
     * @return  A list of norices of the extractor
     */
    private static ArrayList<String> _getExtractorNotices(Element extractorElem){
        ArrayList<String> notices = new ArrayList<String>();
        NodeList extractorNotices = extractorElem.getElementsByTagName(NOTICE_TAGNAME);

        for(int i=0; i<extractorNotices.getLength(); i++){
            Element noticeElem = (Element)extractorNotices.item(i);
            notices.add(noticeElem.getTextContent());
        }

        return notices.size()>0? notices : null;
    }

    /**
     * Returns the extractors with the passed status
     * @param lang  The required language for which the extractors should be returned
     * @param   requiredStatus  The status of the extractors
     * @return  A list containing the extractors of the passed status 
     */
    public static List<Class<Extractor>> getExtractors(Language lang, ExtractorStatus requiredStatus){

        List<Class<Extractor>> extractorsList = extractorClasses.get(lang);
        for(ExtractorSpecification spec : extractors.get(lang)){
            if(spec.status != requiredStatus){

                try{
                    extractorsList.remove(Class.forName(spec.extractorID));
                }
                catch(Exception exp){
                }

            }
        }
        return extractorsList;
    }

}

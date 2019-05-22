package org.dbpedia.extraction.live.config.extractors;

import org.dbpedia.extraction.live.config.Constants;
import org.dbpedia.extraction.live.config.LiveOptions;
import org.dbpedia.extraction.mappings.ArticleCategoriesExtractor;
import org.dbpedia.extraction.mappings.SkosCategoriesExtractor;
import org.dbpedia.extraction.util.Language;
import org.dbpedia.extraction.wikiparser.Namespace;
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

// import org.apache.xerces.parsers.DOMParser;


/**
 * Created by IntelliJ IDEA.
 * User: mabrouk
 * Date: Jul 30, 2010
 * Time: 3:56:55 PM
 * This class reads the configuration file of the live extraction.
 */
public class LiveExtractorConfigReader {

    private static Logger logger = LoggerFactory.getLogger(LiveExtractorConfigReader.class);
    // private static DOMParser parser = new DOMParser();
    private static final String liveConfigFile = "./" + LiveOptions.options.get("languageExtractorConfig");

    private static DocumentBuilderFactory dbFactory;
    private static DocumentBuilder dBuilder;
    private static Document doc;



    //Tag names that are used in live.xml file
    private static final String EXTRACTOR_TAGNAME = "extractor";
    private static final String LANUAGE_TAGNAME = "languages";
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

    public static List<Language> languages = null;
    public static Map<Language,Map<String, ExtractorSpecification>>  extractors = null;
    public static Map<Language, List<Class>> extractorClasses = null;

    public LiveExtractorConfigReader() {
     readExtractorsForMultilanguage();
    }

    //Initialize the static members
    static{
        try{
            dbFactory = DocumentBuilderFactory.newInstance();
            dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(new File(liveConfigFile));

            readExtractorsForMultilanguage();
            //readExtractors();
        }
        catch (FileNotFoundException e){
            logger.error("Required xml file must be configured in live.ini file like this:\n" +
                    "languageExtractorConfig = <yourfilename>.xml\n" + e.getMessage());
        }
        catch(Exception exp){
            logger.error(exp.getMessage(), exp);
        }
    }


    private static  void readExtractorsForMultilanguage(){
        //iterate and build the required list of extractors
        languages = new ArrayList<>();
        extractors = new HashMap<Language,Map<String,ExtractorSpecification>>();
        extractorClasses = new HashMap<Language,List<Class>>();

        NodeList extractorNodes = doc.getElementsByTagName("extractor");
        for(int i=0;i<extractorNodes.getLength(); i++){
            Element element = (Element) extractorNodes.item(i);
            String extractorName = element.getAttribute("name");
            String status = element.getAttribute("status");
            Arrays.asList(element.getAttribute("languages").split("\\s*,\\s*"))
                    .forEach(elem -> multilangExtractors(Language.apply(elem), extractorName, ExtractorStatus.valueOf(status)));
        }
        System.out.println(extractors);
    }

    private static void multilangExtractors(Language lang , String extractor, ExtractorStatus status){
        System.out.println("Language: " + lang + ", extractor: " + extractor  + ", status: " + status);
        languages.add(lang);
        List<Class> classes = extractorClasses.getOrDefault(lang, new ArrayList<>());
        Map<String, ExtractorSpecification> langExtractors = new HashMap<String, ExtractorSpecification>(20);
        try{
            classes.add(ClassLoader.getSystemClassLoader().loadClass(extractor));
            langExtractors.put(extractor, new ExtractorSpecification(extractor, status));
            //TODO implement parsing of match patterns / notices if it is still important
            //TODO: is the specific configuration for SkosCategoriesExtractor and ArticleCategoriesExtractor still needed here?
            // see readExtractors() and readLanguageExtractors for reference
            extractors.put(lang, langExtractors);
            extractorClasses.put(lang, classes);
        }
        catch (ClassNotFoundException e){
            logger.error("Error when trying to load class " + extractor + "\nBecause of: \n" + e.getMessage());
        }
        catch(Exception exp){
            logger.error(exp.getMessage());
        }


    }

    public static List<String> readLanguages(){
        List<String> languageList = new ArrayList<>();

        NodeList extractorNodes = doc.getElementsByTagName("extractor");
        for(int i=0;i<extractorNodes.getLength(); i++){
            Element element = (Element) extractorNodes.item(i);
            languageList.addAll(Arrays.asList(
                    element
                    .getAttribute("languages")
                    .split("\\s*,\\s*")));
        }
        return languageList;
    }
    /**
     * Reads each languages along with its set of extractors
     */
    private static void readExtractors(){
        NodeList languageNodes = doc.getElementsByTagName(LANUAGE_TAGNAME);
        //iterate and build the required list of extractors
        extractors = new HashMap<Language,Map<String,ExtractorSpecification>>();
        extractorClasses = new HashMap<Language,List<Class>>();


        for(int i=0; i<languageNodes.getLength(); i++){

            Element elemLanguage = (Element)languageNodes.item(i);
            String languageName = elemLanguage.getAttribute(NAME_ATTRIBUTENAME);
            Language language = Language.apply(languageName);
            readLanguageExtractors(elemLanguage, language);
        }
    }

    /**
     * Gets the list of extractors specified in the config file along with the status of each extractor
     * @param   elemLanguageExtractors  The XML element containing the extractors of a languages
     * @param   lang    The languages code
     * */
    private static void readLanguageExtractors(Element elemLanguageExtractors, Language lang){
        try{
            NodeList extractorNodes = elemLanguageExtractors.getElementsByTagName(EXTRACTOR_TAGNAME);
            Map<String, ExtractorSpecification> langExtractors = new HashMap<String, ExtractorSpecification>(20);
            ArrayList<Class> langExtractorClasses = new ArrayList<Class>(20);

            //iterate and build the required list of extractors
            for(int i=0; i<extractorNodes.getLength(); i++){
                MatchPattern extractorSpecificPattern = null;
                Element elemExtractor = (Element)extractorNodes.item(i);
                String extractorID = elemExtractor.getAttribute(NAME_ATTRIBUTENAME);
                ExtractorStatus status = ExtractorStatus.valueOf(elemExtractor.getAttribute(EXTRACTOR_STATUS_ATTRIBUTENAME));

                langExtractorClasses.add((Class)(ClassLoader.getSystemClassLoader().loadClass(extractorID)));

                //Those types of extractors need special type of handling as we must call the function _addGenerics for
                //them
                if((extractorID.equals(SkosCategoriesExtractor.class.toString())) ||
                        (extractorID.equals(ArticleCategoriesExtractor.class.toString())))
                    extractorSpecificPattern = _addGenerics(lang, extractorID);

                ArrayList<MatchPattern> patternsList = _getExtractorMatchPatterns(elemExtractor);

                if(extractorSpecificPattern != null)
                    patternsList.add(extractorSpecificPattern);

                //Construct the extractor specification object and adds it to the extractors list
                langExtractors.put(extractorID, new ExtractorSpecification(extractorID, status,
                        patternsList, _getExtractorNotices(elemExtractor)));

                extractors.put(lang, langExtractors);
                extractorClasses.put(lang, langExtractorClasses);
            }

//            LiveExtractionConfigLoader.convertExtractorListToScalaList(extractorClasses);
            System.out.println(extractors);

        }
        catch(Exception exp){
            logger.error(exp.getMessage(), exp);
        }

    }

    /**
     * Loads the generic match patterns for some extractors e.g. SkosCategoriesExtractor, because those extractors
     * need a specific pattern for languages specific category
     * @param lang  The required languages
     * @param extractorID   The ID of the required extractor
     * @return  The match pattern suitable for the passed extractor
     */

    private static MatchPattern _addGenerics(Language lang, String extractorID) {
        MatchPattern pattern = null;
        if(extractorID.equals(SkosCategoriesExtractor.class.toString())){
            pattern = new MatchPattern(MatchType.STARTSWITH, "", Constants.SKOS_BROADER,
                    Namespaces.names(lang).get(Namespace.Category()).toString(), true);

        }
        else if(extractorID.equals(ArticleCategoriesExtractor.class.toString())){
            pattern = new MatchPattern(MatchType.STARTSWITH, "", Constants.SKOS_SUBJECT,
                    Namespaces.names(lang).get(Namespace.Category()).toString() , true);
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
     * @param lang  The required languages for which the extractors should be returned
     * @param   requiredStatus  The status of the extractors
     * @return  A list containing the extractors of the passed status 
     */
    public static List<Class> getExtractors(Language lang, ExtractorStatus requiredStatus){

        List<Class> extractorsList = extractorClasses.get(lang);
        Map<String, ExtractorSpecification> specs = extractors.get(lang);
        for(Object value : specs.values()){
            ExtractorSpecification spec = (ExtractorSpecification) value;
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

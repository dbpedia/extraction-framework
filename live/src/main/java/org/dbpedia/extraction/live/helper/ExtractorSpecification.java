package org.dbpedia.extraction.live.helper;

import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 30, 2010
 * Time: 12:52:10 PM
 * This class holds an extractor along with its details e.g. status, and patterns produced by that extractor 
 */
public class ExtractorSpecification {

    public String extractorID;
    public ExtractorStatus status;
    public ArrayList<String> notices;
    public ArrayList<MatchPattern> generatedTriplePatterns;

    public ExtractorSpecification(String ID, ExtractorStatus extractorStatus, ArrayList <MatchPattern> patternsList,
                                  ArrayList <String> noticeList){
        extractorID = ID;
        status = extractorStatus;
        generatedTriplePatterns = patternsList;
        notices = noticeList;
    }

    public ExtractorSpecification(String ID, ExtractorStatus extractorStatus, ArrayList <MatchPattern> patternsList){
        this(ID, extractorStatus, patternsList, null);
    }

    public ExtractorSpecification(String ID, ExtractorStatus extractorStatus){
        this(ID, extractorStatus, null, null);
    }


    /**
     * Provides equality capability to that class so we can search for an extractor by ID
     * @param passedExtractorSpec   Passed extractor specification object  
     * @return  Whether the current object is equal to the passed object or not 
     */
    public boolean equals(Object passedExtractorSpec){
        if(!(passedExtractorSpec instanceof ExtractorSpecification))
            return false;

        ExtractorSpecification requiredExtractorSpec = (ExtractorSpecification) passedExtractorSpec;
        return this.extractorID.equals(requiredExtractorSpec.extractorID);
    }

}

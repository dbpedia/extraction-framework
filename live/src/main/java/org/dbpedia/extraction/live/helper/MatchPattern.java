package org.dbpedia.extraction.live.helper;

import org.dbpedia.extraction.destinations.Quad;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 30, 2010
 * Time: 4:50:23 PM
 * This class represents the match pattern used for every extractor
 */
public class MatchPattern {
    public MatchType type;
    public String subject;
    public String predicate;
    public String object;
    public boolean pexact;

    public MatchPattern(MatchType Type, String Subject, String Predicate, String Object){
        type = Type;
        subject = Subject;
        predicate = Predicate;
        object = Object;
    }

    public MatchPattern(MatchType Type, String Subject, String Predicate, String Object, boolean PExact){
        type = Type;
        subject = Subject;
        predicate = Predicate;
        object = Object;
        pexact = PExact;
    }

    public boolean accept(Quad quad){
        //TODO implement pattern check (Not supported by previous framework)
        return true;
    }
    
    public String toString(){
        return "Match type = " + type.toString() + ", subject = " + subject +
                ", predicate = " + predicate +", object = " + object ;
    }
}

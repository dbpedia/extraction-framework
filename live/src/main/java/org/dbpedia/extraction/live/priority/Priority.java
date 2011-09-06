package org.dbpedia.extraction.live.priority;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: May 15, 2011
 * Time: 5:24:57 AM
 * Different priority values allowed for a page.
 */
public enum Priority {
    LivePriority, MappingPriority, UnmodifiedPagePriority;
    @Override public String toString() {
        String outputString ="";
        switch (this){
            case LivePriority:
                outputString = "Live Priority";
            break;
            case MappingPriority:
                outputString = "Mapping Priority";
            break;
            case UnmodifiedPagePriority:
                outputString = "Unmodified Priority";
            break;
            default:
                outputString = "";
        }
        return outputString;
    }
}

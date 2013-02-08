package org.dbpedia.extraction.live.queue;

/**
 * Created by IntelliJ IDEA. User: Mohamed Morsey Date: May 15, 2011 Time:
 * 5:24:57 AM Different priority values allowed for a page.
 */
public enum LiveQueuePriority {
    LivePriority, ManualPriority, MappingPriority, OntologyPriority, UnmodifiedPagePriority;

    @Override
    public String toString() {
        switch (this) {
            case LivePriority:
                return "Live LiveQueuePriority";
            case ManualPriority:
                return "Manual LiveQueuePriority";
            case MappingPriority:
                return "Mapping LiveQueuePriority";
            case OntologyPriority:
                return "Ontology LiveQueuePriority";
            case UnmodifiedPagePriority:
                return "Unmodified LiveQueuePriority";
            default:
                return "";
        }
    }
}

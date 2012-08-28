package org.dbpedia.extraction.live.priority;

/**
 * Created by IntelliJ IDEA. User: Mohamed Morsey Date: May 15, 2011 Time:
 * 5:24:57 AM Different priority values allowed for a page.
 */
public enum Priority {
	LivePriority, MappingPriority, OntologyPriority, UnmodifiedPagePriority;

	@Override
	public String toString() {
		switch (this) {
		case LivePriority:
			return "Live Priority";
		case MappingPriority:
			return "Mapping Priority";
		case OntologyPriority:
			return "Ontology Priority";
		case UnmodifiedPagePriority:
			return "Unmodified Priority";
		default:
			return "";
		}
	}
}

package org.dbpedia.extraction.live.publisher;

import java.util.HashSet;

/**
 * Holds a diff for a new extraction (called from PublisherDiffDestination)
 */
public class DiffData {

    public long pageID = 0;
    public HashSet<String> toAdd = null;
    public HashSet<String> toDelete = null;

    public DiffData(long id, HashSet<String> add, HashSet<String> delete){
        pageID = id;
        toAdd = new HashSet<String>(add);
        toDelete = new HashSet<String>(delete);
    }
}


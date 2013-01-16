package org.dbpedia.extraction.live.publisher;

import java.util.HashSet;

/**
 * Holds a diff for a new extraction (called from PublisherDiffDestination)
 */
public class DiffData {

    public HashSet<String> toAdd = null;
    public HashSet<String> toDelete = null;

    public DiffData(HashSet<String> add, HashSet<String> delete){
        toAdd = new HashSet<String>(add);
        toDelete = new HashSet<String>(delete);
    }
}


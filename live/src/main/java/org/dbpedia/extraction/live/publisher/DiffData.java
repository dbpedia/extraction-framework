package org.dbpedia.extraction.live.publisher;

import org.dbpedia.extraction.destinations.Quad;

import java.util.HashSet;

/**
 * Holds a diff for a new extraction (called from PublisherDiffDestination)
 */
public class DiffData {

    public final long pageID;
    public final HashSet<Quad> toAdd;
    public final HashSet<Quad> toDelete;

    public DiffData(long id, HashSet<Quad> add, HashSet<Quad> delete){
        pageID = id;
        toAdd = new HashSet<>(add);
        toDelete = new HashSet<>(delete);
    }
}


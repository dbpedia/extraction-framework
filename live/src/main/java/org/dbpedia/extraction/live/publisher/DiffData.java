package org.dbpedia.extraction.live.publisher;

import org.dbpedia.extraction.transform.Quad;

import java.util.HashSet;
import java.util.Set;

/**
 * Holds a diff for a new extraction (called from PublisherDiffDestination)
 */
public class DiffData {

    public final long pageID;
    public final Set<Quad> toAdd;
    public final Set<Quad> toDelete;
    public final Set<Quad> toReInsert;
    public final Set<Quad> subjects;

    public DiffData(long id, final Set<Quad> add, final Set<Quad> delete, final Set<Quad> toReInsert, final Set<Quad> subjects){
        pageID = id;
        this.toAdd = new HashSet<>(add);
        this.toDelete = new HashSet<>(delete);
        this.toReInsert = new HashSet<>(toReInsert);
        this.subjects = new HashSet<>(subjects);
    }
}


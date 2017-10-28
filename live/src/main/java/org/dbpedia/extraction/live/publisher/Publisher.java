package org.dbpedia.extraction.live.publisher;

import org.dbpedia.extraction.transform.Quad;
import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * Date: Oct 31, 2010
 * Time: 10:59:53 AM
 * This class publishes the triples (added and deleted) to files in order to enable synchronizing our live end-point with
 * other end-points
 * It is originally developed by Claus Stadler
 */

public class Publisher extends Thread{

    private static final Logger logger = LoggerFactory.getLogger(Publisher.class);

    private final static int MAX_QUEUE_SIZE = 5000;
    private final static int MAX_CHANGE_SETS = 400;

    private volatile HashSet<Quad> addedTriples = new HashSet<>();
    private volatile HashSet<Quad> deletedTriples = new HashSet<>();
    private volatile HashSet<Quad> reInsertedTriples = new HashSet<>();
    private volatile HashSet<Quad> subjectsClear = new HashSet<>();

    private final String publishDiffBaseName = LiveOptions.options.get("publishDiffRepoPath");

    public Publisher(String name, int priority){
        this.setPriority(priority);
        this.setName(name);
        start();
    }

    public Publisher(String name){
        this(name, Thread.NORM_PRIORITY);
    }

    public Publisher(){
        this("Publisher", Thread.NORM_PRIORITY);
    }

    public void run()  {

        final HashSet<Long> pageCache = new HashSet<Long>();

        while(true) {
            try {
                // Block until next pubData
                DiffData pubData = Main.publishingDataQueue.take();

                // flush if
                // 1) we get the same page again (possible conflict in diff order
                // 2) we have more than MAX_CHANGE_SETS changesets in queue
                // 3) the diff exceeds a triple limit  MAX_QUEUE_SIZE
                if (pageCache.contains(pubData.pageID) ||
                        pageCache.size() > MAX_CHANGE_SETS ||
                        addedTriples.size() > MAX_QUEUE_SIZE ||
                        deletedTriples.size() > MAX_QUEUE_SIZE ||
                        reInsertedTriples.size() > MAX_QUEUE_SIZE ||
                        subjectsClear.size() > MAX_QUEUE_SIZE) {

                    pageCache.clear();
                    flush();
                }
                bufferDiff(pubData);
                pageCache.add(pubData.pageID);
            } catch(Throwable t) {
                logger.error("An exception was encountered in the Publisher update loop", t);
            }

        }
    }

    private void bufferDiff(DiffData pubData) {
        if(pubData != null){
            addedTriples.addAll(pubData.toAdd);
            deletedTriples.addAll(pubData.toDelete);
            reInsertedTriples.addAll(pubData.toReInsert);
            subjectsClear.addAll(pubData.subjects);
        }
    }

    //TODO possible concurrency issues when main exits but look minor for now
    public void flush() throws IOException  {

        if (addedTriples.isEmpty() && deletedTriples.isEmpty() && reInsertedTriples.isEmpty() && subjectsClear.isEmpty() ) {
            return;
        }


        String fileName = publishDiffBaseName + "/" + PublisherService.getNextPublishPath();
        File parent = new File(fileName).getParentFile();

        if(parent != null)
            parent.mkdirs();

        if (! addedTriples.isEmpty()) {
            RDFDiffWriter.writeAsTurtle(addedTriples, fileName + ".added.nt.gz");
            addedTriples.clear();
        }

        if (! deletedTriples.isEmpty()) {
            RDFDiffWriter.writeAsTurtle(deletedTriples, fileName + ".removed.nt.gz");
            deletedTriples.clear();
        }

        if (! reInsertedTriples.isEmpty()) {
            RDFDiffWriter.writeAsTurtle(reInsertedTriples, fileName + ".reinserted.nt.gz");
            reInsertedTriples.clear();
        }

        if (! subjectsClear.isEmpty()) {
            RDFDiffWriter.writeAsTurtle(subjectsClear, fileName + ".clear.nt.gz");
            subjectsClear.clear();
        }
    }
}

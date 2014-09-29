package org.dbpedia.extraction.live.publisher;

import org.dbpedia.extraction.destinations.Quad;
import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.main.Main;
import org.dbpedia.extraction.live.publisher.RDFDiffWriter;
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

    private HashSet<Quad> addedTriples = new HashSet<>();
    private HashSet<Quad> deletedTriples = new HashSet<>();

    private long counter = 0;
    private HashSet<Long> pageCache = new HashSet<Long>();

    private String publishDiffBaseName = LiveOptions.options.get("publishDiffRepoPath");

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

        counter = 1;
        while(true) {
            try {
                // Block until next pubData
                DiffData pubData = Main.publishingDataQueue.take();

                // flush if
                // 1) we get the same page again (possible conflict in diff order
                // 2) we have more than 300 changesets in queue
                // 3) the diff exceeds a triple limit
                if (pageCache.contains(pubData.pageID) || counter % 300 == 0 || addedTriples.size() > 1500 || deletedTriples.size() > 1500) {
                    flush();
                    counter = 0;
                }
                bufferDiff(pubData);
                counter++;
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
        }
    }

    //TODO possible concurrency issues but look minor for now
    public void flush() throws IOException  {

        pageCache.clear();
        counter = 1;
        String fileName = publishDiffBaseName + "/" + PublisherService.getNextPublishPath();
        File parent = new File(fileName).getParentFile();

        if(parent != null)
            parent.mkdirs();

        RDFDiffWriter.writeAsTurtle(addedTriples, true, fileName, true);
        addedTriples.clear();

        RDFDiffWriter.writeAsTurtle(deletedTriples, false, fileName, true);
        deletedTriples.clear();
    }
}

package org.dbpedia.extraction.live.publisher;

import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private HashSet<String> addedTriples = new HashSet<String>();
    private HashSet<String> deletedTriples = new HashSet<String>();

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

                if (pageCache.contains(pubData.pageID) || counter % 300 == 0) {
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
        StringBuilder addString = new StringBuilder();
        for (String s: addedTriples ) {
            addString.append(s);
        }
        RDFDiffWriter.write(addString.toString(), true, fileName, true);
        addedTriples.clear();

        StringBuilder delString = new StringBuilder();
        for (String s: deletedTriples ) {
            delString.append(s);
        }
        RDFDiffWriter.write(delString.toString(), false, fileName, true);
        deletedTriples.clear();
    }
}

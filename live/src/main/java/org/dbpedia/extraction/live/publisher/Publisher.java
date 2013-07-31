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

    HashSet<String> addedTriples = new HashSet<String>();
    HashSet<String> deletedTriples = new HashSet<String>();

//    protected static Options cliOptions;

    private static long fileNumber = 0;


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

    public void run()
    {
        while(true) {
            try {
                // Block until next pubData
                DiffData pubData = Main.publishingDataQueue.take();
                publishDiff(pubData);
            } catch(Throwable t) {
                logger.error("An exception was encountered in the Publisher update loop", t);
            }

        }
    }

    // TODO delay write to reduce I/O but make sure the same page is not included in a batch
    private void publishDiff(DiffData pubData)
            throws IOException
    {

        String fileName = publishDiffBaseName + "/" + PublisherService.getNextPublishPath();

        File parent = new File(fileName).getParentFile();

        if(parent != null)
            parent.mkdirs();

        if(pubData != null){
            addedTriples.addAll(pubData.toAdd);
            deletedTriples.addAll(pubData.toDelete);
        }


        StringBuilder addString = new StringBuilder();
        for (String s: addedTriples ) {
            addString.append(s);
            addString.append('\n');
        }
        RDFDiffWriter.write(addString.toString(), true, fileName, true);
        addedTriples.clear();

        StringBuilder delString = new StringBuilder();
        for (String s: deletedTriples ) {
            delString.append(s);
            delString.append('\n');
        }
        RDFDiffWriter.write(delString.toString(), false, fileName, true);
        deletedTriples.clear();

    }

}

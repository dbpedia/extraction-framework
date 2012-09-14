package org.dbpedia.extraction.live.main;

import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.feeder.OAIFeeder;
import org.dbpedia.extraction.live.feeder.OAIFeederMappings;
import org.dbpedia.extraction.live.priority.PagePriority;
import org.dbpedia.extraction.live.priority.Priority;
import org.dbpedia.extraction.live.processor.PageProcessor;
import org.dbpedia.extraction.live.publisher.PublishedDataCompressor;
import org.dbpedia.extraction.live.publisher.Publisher;
import org.dbpedia.extraction.live.publisher.PublishingData;
import org.dbpedia.extraction.live.statistics.Statistics;
import org.dbpedia.extraction.live.util.DateUtil;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.dbpedia.extraction.live.util.Files;

import java.io.File;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;


public class Main {
    private static final Logger logger = Logger.getLogger(Main.class);

    private static final int NUMBER_OF_RECENTLY_UPDATED_INSTANCES = 20;

    //This queue is the queue in which MappingUpdateFeeder and LiveUpdateFeeder will place the pages that should
    //be processed, and PageProcessor will take the pages from it and process them afterwards
    public static PriorityBlockingQueue<PagePriority> pageQueue = new PriorityBlockingQueue<PagePriority>(1000);

    //Used for publishing triples to files
//    public static Queue<PublishingData> publishingDataQueue = new LinkedList<PublishingData>();

    public static BlockingQueue<PublishingData> publishingDataQueue = new LinkedBlockingDeque<PublishingData>();

    //This tree is used to avoid processing same page more than once, as it will exist in it only once,
    //so if it exists in it it should be processed and removed from it, os if it is encountered fro another time, it will not exist in that tree, so it
    //it will be just deleted immediately without any further processing.
    public static TreeMap<Long, Boolean> existingPagesTree = new TreeMap<Long, Boolean>();

    // TODO make these non-static
    private volatile static OAIFeederMappings feederMappings = null;
    private volatile static OAIFeeder feederLive = null;
    private volatile static OAIFeeder feederUnmodified = null;
    private volatile static Statistics statistics = null;

    public static void authenticate(final String username, final String password) {
        Authenticator.setDefault(new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username,
                        password.toCharArray());
            }
        });
    }

    public static void initLive() {
        feederMappings = new OAIFeederMappings("FeederMappings", Thread.MIN_PRIORITY, Priority.MappingPriority,
                LiveOptions.options.get("mappingsOAIUri"), LiveOptions.options.get("mappingsBaseWikiUri"), LiveOptions.options.get("mappingsOaiPrefix"),
                2000, 1000, LiveOptions.options.get("uploaded_dump_date"), 0,
                LiveOptions.options.get("working_directory"));


        feederLive = new OAIFeeder("FeederLive", Thread.NORM_PRIORITY, Priority.LivePriority,
                LiveOptions.options.get("oaiUri"), LiveOptions.options.get("baseWikiUri"), LiveOptions.options.get("oaiPrefix"),
                3000, 1000, LiveOptions.options.get("uploaded_dump_date"), 0,
                LiveOptions.options.get("working_directory"));

        feederUnmodified = new OAIFeeder("FeederUnmodified", Thread.MIN_PRIORITY, Priority.UnmodifiedPagePriority,
                LiveOptions.options.get("oaiUri"), LiveOptions.options.get("baseWikiUri"), LiveOptions.options.get("oaiPrefix"),
                30000, 1000, LiveOptions.options.get("uploaded_dump_date"), DateUtil.getDuration1MonthMillis(),
                LiveOptions.options.get("working_directory"));

        statistics = new Statistics(LiveOptions.options.get("statisticsFilePath"), NUMBER_OF_RECENTLY_UPDATED_INSTANCES,
                DateUtil.getDuration1MinMillis(), 2 * DateUtil.getDuration1MinMillis());


    }

    public static void startLive() {
        try {

            feederMappings.startFeeder();
            feederLive.startFeeder();
            feederUnmodified.startFeeder();

            PageProcessor processor = new PageProcessor("Page processing thread", 8);

            Publisher publisher = new Publisher("Publisher", 4);
            PublishedDataCompressor compressor = new PublishedDataCompressor("PublishedDataCompressor", Thread.MIN_PRIORITY);

            statistics.startStatistics();

            logger.info("DBpedia-Live components started");
        } catch (Exception exp) {
            logger.error(ExceptionUtil.toString(exp));
            stopLive();
        }
    }


    public static void stopLive() {
        try {
            logger.warn("Stopping DBpedia Live components");
            // Feeders
            if (feederLive != null) feederLive.stopFeeder();
            if (feederUnmodified != null) feederUnmodified.stopFeeder();
            if (feederMappings != null) feederMappings.stopFeeder();
            // Statistics
            if (statistics != null) statistics.stopStatistics();
            // Publisher
            // TODO
            // Page Processor
            // TODO

        } catch (Exception exp) {
            logger.error(ExceptionUtil.toString(exp));
        }
    }

    public static void main(String[] args)
            throws Exception {

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    stopLive();
                } catch (Exception exp) {

                }
            }
        });

        authenticate("dbpedia", Files.readFile(new File("pw.txt")).trim());

        initLive();
        startLive();
    }
}

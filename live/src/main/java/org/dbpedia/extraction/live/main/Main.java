package org.dbpedia.extraction.live.main;

import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.feeder.Feeder;
import org.dbpedia.extraction.live.feeder.OAIFeeder;
import org.dbpedia.extraction.live.feeder.OAIFeederMappings;
import org.dbpedia.extraction.live.feeder.UnmodifiedFeeder;
import org.dbpedia.extraction.live.publisher.DiffData;
import org.dbpedia.extraction.live.queue.LiveQueue;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.dbpedia.extraction.live.processor.PageProcessor;
import org.dbpedia.extraction.live.publisher.PublishedDataCompressor;
import org.dbpedia.extraction.live.publisher.Publisher;
import org.dbpedia.extraction.live.statistics.Statistics;
import org.dbpedia.extraction.live.util.DateUtil;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.dbpedia.extraction.live.util.Files;

import java.io.File;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;


public class Main {
    private static final Logger logger = Logger.getLogger(Main.class);

    //Used for publishing triples to files
    public static BlockingQueue<DiffData> publishingDataQueue = new LinkedBlockingDeque<DiffData>();

    // TODO make these non-static

    private volatile static Statistics statistics = null;

    private volatile static List<Feeder> feeders = new ArrayList<Feeder>(5);
    private volatile static List<PageProcessor> processors = new ArrayList<PageProcessor>(10);

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

        feeders .add( new OAIFeederMappings("FeederMappings", LiveQueuePriority.MappingPriority,
                LiveOptions.options.get("mappingsOAIUri"), LiveOptions.options.get("mappingsBaseWikiUri"), LiveOptions.options.get("mappingsOaiPrefix"),
                2000, 1000, LiveOptions.options.get("uploaded_dump_date"),
                LiveOptions.options.get("working_directory")));


        feeders .add( new OAIFeeder("FeederLive", LiveQueuePriority.LivePriority,
                LiveOptions.options.get("oaiUri"), LiveOptions.options.get("baseWikiUri"), LiveOptions.options.get("oaiPrefix"),
                3000, 1000, LiveOptions.options.get("uploaded_dump_date"),
                LiveOptions.options.get("working_directory")));

        feeders .add( new UnmodifiedFeeder("FeederUnmodified", LiveQueuePriority.UnmodifiedPagePriority,
                30, 5000,500,30000,
                LiveOptions.options.get("uploaded_dump_date"), LiveOptions.options.get("working_directory")));

        int threads = Integer.parseInt(LiveOptions.options.get("ProcessingThreads"));
        for (int i=0; i < threads ; i++){
            processors.add( new PageProcessor("N" + (i+1)));
        }

        statistics = new Statistics(LiveOptions.options.get("statisticsFilePath"), 20,
                DateUtil.getDuration1MinMillis(), 2 * DateUtil.getDuration1MinMillis());


    }

    public static void startLive() {
        try {

            for (Feeder f: feeders)
                f.startFeeder();

            for (PageProcessor p: processors)
                p.startProcessor();

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

            for (PageProcessor p: processors)
                p.stopProcessor();

            for (Feeder f: feeders)
                // Stop the feeders, taking the most recent date form the queue
                f.stopFeeder(LiveQueue.getPriorityDate(f.getQueuePriority()));

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

package org.dbpedia.extraction.live.main;

import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.feeder.OAIFeeder;
import org.dbpedia.extraction.live.feeder.OAIFeederMappings;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;


public class Main {
    private static final Logger logger = Logger.getLogger(Main.class);

    //Used for publishing triples to files
//    public static Queue<PublishingData> publishingDataQueue = new LinkedList<PublishingData>();

    public static BlockingQueue<PublishingData> publishingDataQueue = new LinkedBlockingDeque<PublishingData>();

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
        feederMappings = new OAIFeederMappings("FeederMappings", Thread.MIN_PRIORITY, LiveQueuePriority.MappingPriority,
                LiveOptions.options.get("mappingsOAIUri"), LiveOptions.options.get("mappingsBaseWikiUri"), LiveOptions.options.get("mappingsOaiPrefix"),
                2000, 1000, LiveOptions.options.get("uploaded_dump_date"), 0,
                LiveOptions.options.get("working_directory"));


        feederLive = new OAIFeeder("FeederLive", Thread.NORM_PRIORITY, LiveQueuePriority.LivePriority,
                LiveOptions.options.get("oaiUri"), LiveOptions.options.get("baseWikiUri"), LiveOptions.options.get("oaiPrefix"),
                3000, 1000, LiveOptions.options.get("uploaded_dump_date"), 0,
                LiveOptions.options.get("working_directory"));

        feederUnmodified = new OAIFeeder("FeederUnmodified", Thread.MIN_PRIORITY, LiveQueuePriority.UnmodifiedPagePriority,
                LiveOptions.options.get("oaiUri"), LiveOptions.options.get("baseWikiUri"), LiveOptions.options.get("oaiPrefix"),
                30000, 1000, LiveOptions.options.get("uploaded_dump_date"), DateUtil.getDuration1MonthMillis(),
                LiveOptions.options.get("working_directory"));

        statistics = new Statistics(LiveOptions.options.get("statisticsFilePath"), 20,
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

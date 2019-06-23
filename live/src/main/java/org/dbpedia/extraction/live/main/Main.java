package org.dbpedia.extraction.live.main;


import org.dbpedia.extraction.destinations.LDStats;
import org.dbpedia.extraction.destinations.LoggerDestination;
import org.dbpedia.extraction.live.config.LiveOptions;
import org.dbpedia.extraction.live.feeder.*;
import org.dbpedia.extraction.live.processor.PageProcessor;
import org.dbpedia.extraction.live.publisher.DiffData;
import org.dbpedia.extraction.live.publisher.Publisher;
import org.dbpedia.extraction.live.queue.LiveQueue;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.dbpedia.extraction.live.storage.JDBCPoolConnection;
import org.dbpedia.extraction.live.storage.JDBCUtil;
import org.dbpedia.extraction.live.util.DateUtil;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;

import static java.lang.Thread.sleep;


public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    //Used for publishing triples to files
    public static BlockingQueue<DiffData> publishingDataQueue = new LinkedBlockingDeque<DiffData>(1000);

    // TODO make these non-static

    //private volatile static Statistics statistics = null;

    private volatile static List<Feeder> feeders = new ArrayList<Feeder>(5);
    private volatile static List<PageProcessor> processors = new ArrayList<PageProcessor>(10);
    private volatile static Publisher publisher;

    // DEBUGGING  STATS
    private static Boolean debugSkipProcessors = false;
    private static int nrOfProcesses = 0;

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

        JDBCUtil.execSQL("SET names utf8mb4");

        String uploaded_dump_date = "";
        if (LiveOptions.options.get("uploaded_dump_date").equalsIgnoreCase("now")) {
            uploaded_dump_date = DateUtil.transformToUTC(System.currentTimeMillis());
            logger.info("uploaded_dump_date was 'now', changed to " + uploaded_dump_date);
        } else {
            uploaded_dump_date = LiveOptions.options.get("uploaded_dump_date");
        }


        if (Boolean.parseBoolean(LiveOptions.options.get("feeder.rcstream.enabled"))) {
            feeders.add(new RCStreamFeeder("RCStreamFeeder", LiveQueuePriority.LivePriority,
                    uploaded_dump_date, LiveOptions.options.get("working_directory"),
                    LiveOptions.options.get("feeder.rcstream.room")));
        }

        if (Boolean.parseBoolean(LiveOptions.options.get("feeder.allpages.enabled"))) {
            feeders.add(new AllPagesFeeder("AllPagesFeeder", LiveQueuePriority.LivePriority,
                    uploaded_dump_date, LiveOptions.options.get("working_directory")));
        }

        if (Boolean.parseBoolean(LiveOptions.options.get("feeder.mappings.enabled"))) {
            long pollInterval = Long.parseLong(LiveOptions.options.get("feeder.mappings.pollInterval"));
            long sleepInterval = Long.parseLong(LiveOptions.options.get("feeder.mappings.sleepInterval"));
            feeders.add(new OAIFeederMappings("FeederMappings", LiveQueuePriority.MappingPriority,
                    LiveOptions.options.get("mappingsOAIUri"), LiveOptions.options.get("mappingsBaseWikiUri"), LiveOptions.options.get("mappingsOaiPrefix"),
                    pollInterval, sleepInterval, uploaded_dump_date,
                    LiveOptions.options.get("working_directory")));
        }

        if (Boolean.parseBoolean(LiveOptions.options.get("feeder.live.enabled"))) {
            long pollInterval = Long.parseLong(LiveOptions.options.get("feeder.live.pollInterval"));
            long sleepInterval = Long.parseLong(LiveOptions.options.get("feeder.live.sleepInterval"));
            feeders.add(new OAIFeeder("FeederLive", LiveQueuePriority.LivePriority,
                    LiveOptions.options.get("oaiUri"), LiveOptions.options.get("baseWikiUri"), LiveOptions.options.get("oaiPrefix"),
                    pollInterval, sleepInterval, uploaded_dump_date,
                    LiveOptions.options.get("working_directory")));
        }

        if (Boolean.parseBoolean(LiveOptions.options.get("feeder.unmodified.enabled"))) {
            int minDaysAgo = Integer.parseInt(LiveOptions.options.get("feeder.unmodified.minDaysAgo"));
            int chunk = Integer.parseInt(LiveOptions.options.get("feeder.unmodified.chunk"));
            int threshold = Integer.parseInt(LiveOptions.options.get("feeder.unmodified.threshold"));
            long sleepTime = Long.parseLong(LiveOptions.options.get("feeder.unmodified.sleepTime"));
            feeders.add(new UnmodifiedFeeder("UnmodifiedFeeder", LiveQueuePriority.UnmodifiedPagePriority,
                    minDaysAgo, chunk, threshold, sleepTime,
                    uploaded_dump_date, LiveOptions.options.get("working_directory")));
        }

        if (Boolean.parseBoolean(LiveOptions.options.get("feeder.eventstreams.enabled"))) {
            feeders.add(new EventStreamsFeeder("EventStreamsFeeder", LiveQueuePriority.EventStreamsPriority,
                    uploaded_dump_date, LiveOptions.options.get("working_directory")));
        }


        int threads = Integer.parseInt(LiveOptions.options.get("ProcessingThreads"));
        nrOfProcesses = threads;
        for (int i = 0; i < threads; i++) {
            processors.add(new PageProcessor("N" + (i + 1)));
        }

        //statistics = new Statistics(LiveOptions.options.get("statisticsFilePath"), 20,
        //        DateUtil.getDuration1MinMillis(), 2 * DateUtil.getDuration1MinMillis());

        //DEBUGOPTIONS

        if (Boolean.parseBoolean(LiveOptions.options.get("debugExitBeforeInit"))) {
            System.exit(0);//checked
        }
        debugSkipProcessors = Boolean.parseBoolean(LiveOptions.options.get("debugSkipProcessors"));


    }

    public static void startLive() {
        try {

            for (Feeder f : feeders) {
                f.startFeeder();

            }
            for (PageProcessor p : processors) {
                if (!debugSkipProcessors) {
                    p.startProcessor();
                }
            }

            publisher = new Publisher("Publisher", 4);

            //statistics.startStatistics();

            logger.info("DBpedia-Live components started");
        } catch (Exception exp) {
            logger.error(ExceptionUtil.toString(exp), exp);
            stopLive();
        }
    }


    public static void stopLive() {
        try {

            for (PageProcessor p : processors) {
                p.stopProcessor();
            }

            for (Feeder f : feeders) {
                logger.info("Stopping feeder: " + f.getName());
                // Stop the feeders, taking the most recent date form the queue
                f.stopFeeder(LiveQueue.getPriorityDate(f.getQueuePriority()));
            }
            // Statistics
            //if (statistics != null) statistics.stopStatistics();

            // Publisher
            publisher.flush();

        } catch (Exception exp) {
            exp.printStackTrace();
            logger.error(ExceptionUtil.toString(exp));
        }
    }

    public static void main(String[] args)
            throws Exception {


      /*  Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                } catch (Exception exp) {
                    exp.printStackTrace();
                    logger.error("???", exp);
                }
            }
        });*/

        //authenticate("dbpedia", Files.readFile(new File("pw.txt")).trim());

        initLive();
        startLive();

        System.out.println("Listening to user input on command line, type q to quit");
        Scanner s = new Scanner(System.in);
        String next = "";
        boolean more = true;
        while (more) {
            next = s.next();
            if (next.equalsIgnoreCase("q")) {
                System.out.println("received shutdown command");
                more = false;
                stopLive();
            } else if (next.equalsIgnoreCase("s")) {
                float articlespeed = LDStats.avg() / (float) nrOfProcesses;

                String msg = "\nEXTRACTION: " +
                        "\nCurrent queue: " + LiveQueue.getQueueSize() + "" +
                        "\nAVG time needed to extract one page: " + articlespeed + " ms or " + (1000 / articlespeed) + " per second" +
                        "\n " +
                        "\nDATABASE: " +
                        "\ntotal statements executed: " + JDBCPoolConnection.getStats().getStatementsExecuted() +
                        "\ntime to prepare avg: " + JDBCPoolConnection.getStats().getStatementPrepareTimeAvg() +
                        "\nprep statements avg: " + JDBCPoolConnection.getStats().getStatementExecuteTimeAvg() +
                        "\nTotal connections: " + JDBCPoolConnection.getStats().getTotalCreatedConnections();
                System.out.println(msg);
            } else {
                System.out.println("received nothing meaningful: '" + next + "'\n" +
                        "Commands:\n" +
                        "  [q] to quit and shutdown\n" +
                        "  [s] to show status");
            }
        }

        //System exit is called here to interrupt all remaining threads.
        sleep(2000);
        System.exit(0);
    }
}

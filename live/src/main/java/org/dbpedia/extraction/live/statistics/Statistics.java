package org.dbpedia.extraction.live.statistics;

import org.slf4j.Logger;
import org.dbpedia.extraction.live.main.Main;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created with IntelliJ IDEA.
 * User: Dimitris Kontokostas
 * Date: 8/3/12
 * Time: 11:30 AM
 * Handles all statistics
 */
public class Statistics {
    private static Logger logger = LoggerFactory.getLogger(Statistics.class);
    // File to read/write statistics
    private final String statisticsFileName;
    // Number of detailed statistics instances to keep
    private final int statisticsDetailedInstances;
    // Update interval in miliseconds
    private final long statisticsUpdateInterval;
    // Initial delay on application startup
    private final long statisticsInitialDelay;
    private Timer timer = new Timer("DBpedia-Live Statistics Timer");

    public Statistics(String fileName, int detailedInstances, long updateInterval, long initialDelay) {
        this.statisticsFileName = fileName;
        this.statisticsDetailedInstances = detailedInstances;
        this.statisticsUpdateInterval = updateInterval;
        this.statisticsInitialDelay = initialDelay;
    }

    public void stopStatistics() {
        // TODO wait to finish ?
        timer.cancel();
    }


    public void startStatistics() {
        // TODO read old statistics
        // TODO write Statistics dir / file if not exists
        // TODO init StatisticsData variables

        timer.schedule(new TimerTask() {
            public void run() {
                try {
                    logger.info(StatisticsData.generateStatistics());
                } catch (Exception exp) {
                    logger.error("DBpedia-live Statistics: I/O Error: " + exp.getMessage(), exp);
                }
            }
        }, statisticsInitialDelay, statisticsUpdateInterval); //One-Minute

    }
}

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

        //initStatistics();
    }

    private void initStatistics() {

        FileReader reader = null;
        try {
            File file = new File(statisticsFileName);

            if (!file.exists()) {
                file.mkdirs();
                file.createNewFile();
            } else {
                reader = new FileReader(statisticsFileName);
                LineNumberReader line = new LineNumberReader(reader);//Used for reading line by line from file

                // Update statistics data
                long t1m = Integer.parseInt(line.readLine());
                long t5m = Integer.parseInt(line.readLine());
                long t1h = Integer.parseInt(line.readLine());
                long t1d = Integer.parseInt(line.readLine());
                long tat = Integer.parseInt(line.readLine());

                // TODO find cleaner way to calculate All time stats
                StatisticsData.setAllStats(t1m,t5m,t1h,t1d,tat-t1d);
            }

        } catch (Exception exp) {
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException exp) {
            }
        }
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
                Writer writer = null;
                try {
                    writer = new FileWriter(statisticsFileName);
                    writer.write(StatisticsData.generateStatistics(statisticsDetailedInstances));
                } catch (IOException exp) {
                    logger.error("DBpedia-live Statistics: Failed to generate statistics: " + exp.getMessage(), exp);
                } catch (Exception exp) {
                    logger.error("DBpedia-live Statistics: I/O Error: " + exp.getMessage(), exp);
                } finally {
                    try {
                        if (writer != null)
                            writer.close();
                    } catch (IOException exp) {
                        logger.error("DBpedia-live Statistics: I/O Error: " + exp.getMessage(), exp);
                    }
                }
            }
        }, statisticsInitialDelay, statisticsUpdateInterval); //One-Minute

    }
}

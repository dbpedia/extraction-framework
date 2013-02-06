package org.dbpedia.extraction.live.statistics;

import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.main.Main;

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
    private final String statisticsFileName;
    private final int statisticsDetailedInstances;
    private final long statisticsUpdateInterval;
    private final long statisticsInitialDelay;
    private Timer timer = new Timer("DBpedia Live Statistics Timer");

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

                StatisticsData.setStats1m(t1m);
                StatisticsData.setStats5m(t5m);
                StatisticsData.setStats1h(t1h);
                StatisticsData.setStats1d(t1d);
                StatisticsData.setStatsAll(tat - t1d);
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

                Logger logger = Logger.getLogger(Main.class);
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

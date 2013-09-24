package org.dbpedia.extraction.live.publisher;

import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.util.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Calendar;
import java.io.File;


/**
 Keeps data about the current diff publishing status
 */
public class PublisherService {

    private static final Logger logger = LoggerFactory.getLogger(PublisherService.class);

    private static long sequence = -2;
    private static int year = -2;
    private static int month = -2;
    private static int day = -2;
    private static int hour = -2;

    protected PublisherService() {
    }

    public static synchronized String getNextPublishPath() {
        if (sequence == -2)
            init();

        Calendar curDate = Calendar.getInstance();

        if  (   (curDate.get(Calendar.HOUR_OF_DAY) == hour) &&
                (curDate.get(Calendar.DAY_OF_MONTH) + 1 == day) &&
                (curDate.get(Calendar.MONTH) == month) &&
                (curDate.get(Calendar.YEAR) == year)
            ){

            sequence++;
        }
        else {
            sequence = 0;
            hour = curDate.get(Calendar.HOUR_OF_DAY);
            day =  curDate.get(Calendar.DAY_OF_MONTH);
            month =curDate.get(Calendar.MONTH) + 1;
            year = curDate.get(Calendar.YEAR);
        }

        // TODO write latest path not from here and not so aften (too much I/O)
        writeLastPublishPath();

        return getCurrentPublishPath();

    }

    // When called as standalone function race conditions might produce false data but not important for now
    public static void writeLastPublishPath(){
        if (year < 0 && month < 0 && day < 0 && hour < 0 && sequence < 0)
            return;
        File f = new File(LiveOptions.options.get("publishDiffRepoPath") + "/lastPublishedFile.txt");
        // replace / -> - for backwards compatibility with dbpintegrator
        Files.createFile(f, getCurrentPublishPath().replace('/','-'));
    }

    private static String readLastPublishPath(){
        File f = new File(LiveOptions.options.get("publishDiffRepoPath") + "/lastPublishedFile.txt");
        try {
            return Files.readFile(f);
        } catch (IOException exp) {
            logger.warn("Last publish date cannot be read due to " + exp.getMessage());
            logger.info("Assuming the last publish date is now");
            return "";
        }
    }

    private static String getCurrentPublishPath() {
        return String.format("%04d/%02d/%02d/%02d/%06d", year, month, day, hour, sequence);
    }

    private static void init() {

        String lastPath = readLastPublishPath();

        if (lastPath.isEmpty()) {
            Calendar curDate = Calendar.getInstance();
            sequence = -1;  // will increase on  getNextPublishPath
            hour = curDate.get(Calendar.HOUR_OF_DAY);
            day =  curDate.get(Calendar.DAY_OF_MONTH);
            month =curDate.get(Calendar.MONTH) + 1;
            year = curDate.get(Calendar.YEAR);
        }
        else {
            String []parts = lastPath.split("-");
            year = Integer.parseInt(parts[0].trim());
            month = Integer.parseInt(parts[1].trim());
            day = Integer.parseInt(parts[2].trim());
            hour = Integer.parseInt(parts[3].trim());
            sequence = Long.parseLong(parts[4].trim());
        }

    }
}

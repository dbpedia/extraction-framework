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
import org.dbpedia.extraction.live.statistics.RecentlyUpdatedInstance;
import org.dbpedia.extraction.live.util.DateUtil;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.dbpedia.extraction.live.util.Files;

import java.io.*;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;

//import org.dbpedia.extraction.wikiparser.*;


public class Main
{
    private static final int NUMBER_OF_RECENTLY_UPDATED_INSTANCES = 20;

    //Members used for writing statics about DBpedia-live
    private static int _instancesUpdatedInMinute = 0;
    public static int instancesUpdatedInMinute = 0;

    private static int _instancesUpdatedIn5Minutes = 0;
    public static int instancesUpdatedIn5Minutes = 0;

    private static int _instancesUpdatedInHour = 0;
    public static int instancesUpdatedInHour = 0;

    private static int _instancesUpdatedInDay = 0;
    public static int instancesUpdatedInDay = 0;

    public static int totalNumberOfUpdatedInstances = 0;

    //This array will contain a list of the most recently updated instances, those instances will be displayed
    //in the statistics web page
    public static RecentlyUpdatedInstance [] recentlyUpdatedInstances = new RecentlyUpdatedInstance[NUMBER_OF_RECENTLY_UPDATED_INSTANCES];

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

	public static void authenticate(final String username, final String password)
	{
		Authenticator.setDefault(new Authenticator() {
		    @Override
			protected PasswordAuthentication getPasswordAuthentication() {
		        return new PasswordAuthentication(username,
		        								  password.toCharArray());
		    }
		});
	}

	public static void main(String[] args)
		throws Exception
	{

        readOldStatistics();

        for(int i = 0; i < recentlyUpdatedInstances.length; i++)
                recentlyUpdatedInstances[i] = new RecentlyUpdatedInstance();

        //Timers required for statistics
        Timer oneMinuteTimer = new Timer("One-Minute timer");
        Timer fiveMinuteTimer = new Timer("Five-Minute timer");
        Timer oneHourTimer = new Timer("One-Hour timer");
        Timer oneDayTimer = new Timer("One-Day timer");
        
        //Activating timers

        oneMinuteTimer.schedule(new TimerAction(1), 0, 60*1000); //One-Minute
        fiveMinuteTimer.schedule(new TimerAction(5), 0, 5*60*1000); //Five-Minutes
        oneHourTimer.schedule(new TimerAction(60), 0, 60*60*1000); //One-Hour
        oneDayTimer.schedule(new TimerAction(1440), 0, 1440*60*1000); //One-Day

        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        authenticate("dbpedia", Files.readFile(new File("pw.txt")).trim());
        Publisher publisher = new Publisher("Publisher", 4);

        //All feeders, one for live update, one for mapping affected pages, and the last one is for unmodified pages
        OAIFeederMappings feederMappings = new OAIFeederMappings("FeederMappings", Thread.MIN_PRIORITY, Priority.MappingPriority,
                LiveOptions.options.get("mappingsOAIUri"), LiveOptions.options.get("mappingsBaseWikiUri"), LiveOptions.options.get("mappingsOaiPrefix"),
                2000, 1000, LiveOptions.options.get("uploaded_dump_date"), 0,
                LiveOptions.options.get("working_directory"));
        feederMappings.startFeeder();


        OAIFeeder feederLive = new OAIFeeder("FeederLive", Thread.NORM_PRIORITY, Priority.LivePriority,
                LiveOptions.options.get("oaiUri"), LiveOptions.options.get("baseWikiUri"), LiveOptions.options.get("oaiPrefix"),
                3000, 1000, LiveOptions.options.get("uploaded_dump_date"), 0,
                LiveOptions.options.get("working_directory"));
        feederLive.startFeeder();

        OAIFeeder feederUnmodified = new OAIFeeder("FeederUnmodified", Thread.MIN_PRIORITY, Priority.UnmodifiedPagePriority,
                LiveOptions.options.get("oaiUri"), LiveOptions.options.get("baseWikiUri"), LiveOptions.options.get("oaiPrefix"),
                30000, 1000, LiveOptions.options.get("uploaded_dump_date"), DateUtil.getDuration1MonthMillis(),
                LiveOptions.options.get("working_directory"));
        feederUnmodified.startFeeder();

        PageProcessor processor = new PageProcessor("Page processing thread", 8);

        PublishedDataCompressor compressor = new PublishedDataCompressor("PublishedDataCompressor", Thread.MIN_PRIORITY);

	}

    private static String getLastResponseDate(String strFileName)
    {
        Logger logger = Logger.getLogger(Main.class);
        String strLastResponseDate = null;

        try{
            FileInputStream fsLastResponseDateFile = new FileInputStream(strFileName);

            int ch;
            strLastResponseDate="";
            while( (ch = fsLastResponseDateFile.read()) != -1)
                strLastResponseDate += (char)ch;

        }
        catch(Exception exp){
           logger.error(ExceptionUtil.toString(exp)); 
        }

        return strLastResponseDate;

    }

    /**
     * Reads the old values of statistics, in order not to initialize all statistics counters to 0
     * @return  True if the read process was successful, and false otherwise
     */
    private static boolean readOldStatistics(){

        try{
            File instancesFile = new File(LiveOptions.options.get("statisticsFilePath"));
            FileReader statsReader = new FileReader(instancesFile);
            LineNumberReader statsLineReader = new LineNumberReader(statsReader);//Used for reading line by line from file

            //The order of those items in file is "Instance updated in 1 min, 5 min, 1 Hr, 1 day, for start of database
            instancesUpdatedInMinute = Integer.parseInt(statsLineReader.readLine());
            instancesUpdatedIn5Minutes = Integer.parseInt(statsLineReader.readLine());
            instancesUpdatedInHour = Integer.parseInt(statsLineReader.readLine());
            instancesUpdatedInDay = Integer.parseInt(statsLineReader.readLine());
            totalNumberOfUpdatedInstances = Integer.parseInt(statsLineReader.readLine());
            statsReader.close();
            return true;
        }
        catch (Exception exp){
            return false;
        }
    }

    private static void writeLastResponseDate(String strFileName, String strLastResponseDate)
    {
        Logger logger = Logger.getLogger(Main.class);
        try{
            FileOutputStream fsLastResponseDateFile = new FileOutputStream(strFileName);
            OutputStreamWriter osWriter = new OutputStreamWriter(fsLastResponseDateFile);
            osWriter.write(strLastResponseDate);
            osWriter.close();

        }
        catch(Exception exp){
           logger.error(ExceptionUtil.toString(exp));
        }
    }

    static class TimerAction extends TimerTask{
        private int _duration;

        public TimerAction(int duration){
            this._duration = duration; 
        }

        public void run(){
            Logger logger = Logger.getLogger(Main.class);

            try{
                switch(_duration){
                    case 1:
                        
                        _instancesUpdatedInMinute = instancesUpdatedInMinute;
                        instancesUpdatedInMinute = 0;

                        //Write statistics to the file
                        File instancesFile = new File(LiveOptions.options.get("statisticsFilePath"));
                        FileWriter writer = new FileWriter(instancesFile);
                        writer.write(_instancesUpdatedInMinute + "\r\n");

                        //We should write _instancesUpdatedIn5Minutes if it's not 0, otherwise we write instancesUpdatedIn5Minutes
                        //as the application may have started running less than 5 minutes ago
//                        int val = _instancesUpdatedIn5Minutes > 0? _instancesUpdatedIn5Minutes : instancesUpdatedIn5Minutes;
                        int val = _instancesUpdatedIn5Minutes > _instancesUpdatedInMinute ? _instancesUpdatedIn5Minutes : _instancesUpdatedInMinute;
                        writer.write(val + "\r\n");

                        //We should write _instancesUpdatedInHour if it's not 0, otherwise we write instancesUpdatedInHour
                        //as the application may have started running less than an hour ago
//                        val = _instancesUpdatedInHour > 0? _instancesUpdatedInHour : instancesUpdatedInHour;
                        val = _instancesUpdatedInHour > _instancesUpdatedIn5Minutes ? _instancesUpdatedInHour : _instancesUpdatedIn5Minutes;
                        writer.write(val + "\r\n");

                        //We should write _instancesUpdatedInDay if it's not 0, otherwise we write instancesUpdatedInDay
                        //as the application may have started running less than a day ago
//                        val = _instancesUpdatedInDay > 0? _instancesUpdatedInDay : instancesUpdatedInDay;
                        val = _instancesUpdatedInDay > _instancesUpdatedInHour ? _instancesUpdatedInDay : _instancesUpdatedInHour;
                        writer.write(val + "\r\n");

                        writer.write(totalNumberOfUpdatedInstances + "\r\n");

                        RecentlyUpdatedInstance [] sortedInstances = new RecentlyUpdatedInstance[NUMBER_OF_RECENTLY_UPDATED_INSTANCES];

                        System.arraycopy(recentlyUpdatedInstances, 0, sortedInstances, 0, NUMBER_OF_RECENTLY_UPDATED_INSTANCES);

                        //If the first element of the array is NULL, then we cannot sort the array
                        if(sortedInstances[0] != null)
                            Arrays.sort(sortedInstances);

                        for(int i = 0; i< sortedInstances.length; i++){
                            writer.write( sortedInstances[i] + "\r\n");    
                        }

                        writer.close();

                        logger.info("DBpedia-live statistics are successfully written\n");
                        break;
                    case 5:
                        _instancesUpdatedIn5Minutes = instancesUpdatedIn5Minutes;
                        instancesUpdatedIn5Minutes = 0;
                        break;
                    case 60:
                        _instancesUpdatedInHour = instancesUpdatedInHour;
                        instancesUpdatedInHour = 0;
                        break;
                    case 1440:
                        _instancesUpdatedInDay = instancesUpdatedInDay;
                        instancesUpdatedInDay = 0;
                        break;
                    default:
                        logger.error("Invalid timer value for instance-processing");
                        break;
                }

            }
            catch(Exception exp){
                logger.error("DBpedia-live statistics cannot be written due to " + exp.getMessage(), exp);
            }
        }
    }

}

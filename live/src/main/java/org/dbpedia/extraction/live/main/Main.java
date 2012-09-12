package org.dbpedia.extraction.live.main;


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
import org.dbpedia.extraction.live.util.Files;

import java.io.*;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.PriorityBlockingQueue;

//import org.dbpedia.extraction.wikiparser.*;


public class Main
{
    private static final int NUMBER_OF_RECENTLY_UPDATED_INSTANCES = 20;


    //This array will contain a list of the most recently updated instances, those instances will be displayed
    //in the statistics web page

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

        Statistics statistics = new Statistics(LiveOptions.options.get("statisticsFilePath"),NUMBER_OF_RECENTLY_UPDATED_INSTANCES,
                DateUtil.getDuration1MinMillis(),2*DateUtil.getDuration1MinMillis());
        statistics.startStatistics();

	}

}

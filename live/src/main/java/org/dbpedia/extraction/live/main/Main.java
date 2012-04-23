package org.dbpedia.extraction.live.main;


import org.apache.log4j.Logger;
import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.feeder.LiveUpdateFeeder;
import org.dbpedia.extraction.live.feeder.MappingUpdateFeeder;
import org.dbpedia.extraction.live.feeder.UnmodifiedPagesUpdateFeeder;
import org.dbpedia.extraction.live.priority.PagePriority;
import org.dbpedia.extraction.live.processor.PageProcessor;
import org.dbpedia.extraction.live.publisher.PublishedDataCompressor;
import org.dbpedia.extraction.live.publisher.Publisher;
import org.dbpedia.extraction.live.publisher.PublishingData;
import org.dbpedia.extraction.live.statistics.RecentlyUpdatedInstance;
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
//        TestJDBCConnection con = new TestJDBCConnection();
//        //con.Connect();
//        //con.convertUnicode();
//        TestJDBCConnection.Connect();
//        //con.Connect();
//        URIImpl uri = new URIImpl("http://www.w3.org/2001/XMLSchema#date");
//        //System.out.println(con.toNTriples(new LiteralImpl("Hello World",new URIImpl("http://www.w3.org/2001/XMLSchema#date"))));
//        System.out.println(uri.toString());
        //Mapping update testing
        
//        String strDate = "2010-07-25T18:55:52Z";
//        String[] arr = strDate.split("-|:|T|Z");

//        System.out.println(Util.getDBpediaCategoryPrefix("en"));


        /*
        authenticate("dbpedia", Files.readFile(new File("pw.txt")).trim());
        Iterator<Document> myTestIterator = new OAIUnmodifiedRecordIterator(
                "http://en.wikipedia.org/wiki/Special:OAIRepository", "2011-02-19T14:20:53Z", "2011-02-19T14:29:53Z");
        while(myTestIterator.hasNext()){
            Document doc = myTestIterator.next();

            //Extract the page identifier from the XML returned. It is available in a tag <identifier>
            NodeList nodes = doc.getElementsByTagName("identifier");
            String strFullPageIdentifier = nodes.item(0).getChildNodes().item(0).getNodeValue();
            String startDate = XMLUtil.getPageModificationDate(doc);

            System.out.println(strFullPageIdentifier + "        " + startDate);

        } */

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
        MappingUpdateFeeder mappingFeeder = new MappingUpdateFeeder("Mapping feeder thread", 4);
        LiveUpdateFeeder liveFeeder = new LiveUpdateFeeder("Live feeder thread", 6);
        UnmodifiedPagesUpdateFeeder unmodifiedFeeder = new UnmodifiedPagesUpdateFeeder("Unmodified pages update feeder",
                Thread.MIN_PRIORITY);

        PageProcessor processor = new PageProcessor("Page processing thread", 8);


        PublishedDataCompressor compressor = new PublishedDataCompressor("PublishedDataCompressor", Thread.MIN_PRIORITY);


        /*

        Calendar calendar = new GregorianCalendar();
        calendar.set(2011, 04, 01, 22, 0, 0);
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        String str = dateFormatter.format(new Date());


        Iterator<Document> myTestIterator = new OAIUnmodifiedRecordIterator(
                "http://live.dbpedia.org/syncwiki/Special:OAIRepository", "2011-02-19T14:20:00Z", "2011-02-19T14:29:59Z");
        while(myTestIterator.hasNext()){
            Document doc = myTestIterator.next();
            NodeList nodes = doc.getElementsByTagName("identifier");

            for(int i=0; i < nodes.getLength(); i++){
                String strFullPageIdentifier = nodes.item(i).getChildNodes().item(0).getNodeValue();
                String startDate = XMLUtil.getPageModificationDate(doc);

                System.out.println("Start date = " + startDate);
                int colonPos = strFullPageIdentifier.lastIndexOf(":");
                String strPageID = strFullPageIdentifier.substring(colonPos+1);

                long pageID = new Long(strPageID);
                System.out.println("Page ID = " + pageID);

            }


        }

        */

//        System.out.println(Util.getDBpediaCategoryPrefix("en"));

//        MappingsUpdate update = new MappingsUpdate(4,"High Priority process");
//        //MyThread thr1 = new MyThread(3, "Low Priority");
//        LiveFeeder feeder = new LiveFeeder();
//        PageQueueProcessor processor = new PageQueueProcessor();

        //update.startThread();
        //update.startThread();


        //update.getMappingsUpdate();

//        update.testPriorityQueue();
        //TestMappingPages.GetMappingPages();

//        Assert.assertEquals(5,5);
//
//		authenticate("dbpedia", Files.readFile(new File("pw.txt")).trim());
//
//		String oaiUri = "http://en.wikipedia.org/wiki/Special:OAIRepository";
//
//		Calendar calendar = new GregorianCalendar();
//		calendar.set(2010, 06, 10, 22, 0, 0);
//
//		//Date startDate = calendar.getTime();
//		String startDate = "2010-06-01T15:00:00Z";
//
//		int pollInterval = 30;
//		int sleepInterval = 5;
//		String lastResponseDateFile = "./live/lastResponseDate.dat";
//
//        String propsedStartDate = getLastResponseDate(lastResponseDateFile);
//        if(propsedStartDate != null)
//            startDate = propsedStartDate;
//
//
//		int articleDelay = 0;
//		boolean articleRenewal = false;
//		String baseWikiUri = "http://en.wikipedia.org/wiki/";
//		String oaiPrefix = "oai:en.wikipedia.org:enwiki:";
//
//
//		 // Create an iterator which keeps polling the OAIRepository
//		Iterator<Document> recordIterator =
//			OAIUtil.createEndlessRecordIterator(oaiUri, startDate, pollInterval * 1000, sleepInterval * 1000);
//
//        //int NumberOfDocument = 1;
//        //Caller.Call();
//        //Caller MyCaller = new Caller();
//        //System.out.println(recordIterator.getClass());
//        while(recordIterator.hasNext())
//        {
//            try
//            {
//                Document xml = recordIterator.next();
//                NodeToRecordTransformer transformer = new NodeToRecordTransformer(baseWikiUri, oaiUri, oaiPrefix);
//                Record currentNodeRec = (Record)transformer.transform(xml);
//
//                scala.xml.Node element = XML.loadString(XMLUtil.toString(xml));
//    //            System.out.println("//////////////////////////////////////////////////////////");
//                String lastResponseDate = XMLUtil.getPageModificationDate(xml);
//
//    //            System.out.println(lastResponseDate);
//    //            System.out.println(XMLUtil.toString(xml));
//    //            System.out.println("//////////////////////////////////////////////////////////");
//                org.dbpedia.extraction.sources.Source wikiPageSource = XMLSource.fromXML((Elem)element);
//
//                LiveExtractionManager.extractFromPage((Elem)element);
//                writeLastResponseDate(lastResponseDateFile, lastResponseDate);
//            }
//            catch(Exception exp)
//            {
//                Logger logger = Logger.getLogger(Main.class);
//                logger.error(ExceptionUtil.toString(exp));
//            }
//
//        }
//
//		// Optional: Create an iterator which does not return elements unless
//		// they have reached a certain age.
//		// Concretely for Wikipedia articles this means:
//		// Only return articles that have not been edited for at least x seconds
//		TimeWindowIterator timeWindowIterator = null;
//		if(articleDelay != 0) {
//			timeWindowIterator = new TimeWindowIterator(recordIterator, articleDelay, false, articleRenewal);
//			recordIterator = timeWindowIterator;
//		}
//
//		// Transform the XML fragments to Java domain objects
//        //This iterator is not needed any more
//		Iterator<IRecord> iterator = new TransformIterator<Document, IRecord>(recordIterator, new NodeToRecordTransformer(baseWikiUri, oaiUri, oaiPrefix));
//
//
//    		while(iterator.hasNext()) {
//			IRecord record = iterator.next();
//
//
//			if(record instanceof Record) {
//
//                /*
//                ///////////////////////////Start of my code//////////////////////////
//                //System.out.println(((Record)record).getMetadata());
//                //org.dbpedia.extraction.wikiparser.WikiTitle t= new org.dbpedia.extraction.wikiparser.WikiTitle("Template:Automobile");
//                WikiTitle t = LiveExtractionManager.ConstructWikiTitle();
//
//                WikiTitle TestTitle = WikiTitle.parse(((Record) record).getMetadata().getTitle().getFullTitle(),
//                        org.dbpedia.extraction.util.Language.Default());
//
//                //System.out.println(p.toXML());
//                System.out.println("////////////////META DATA//////////////");
//                //System.out.println(((Record) record).getMetadata());
//                String strOAIID = ((Record) record).getMetadata().getOaiId();
//                String strRevision = ((Record)record).getMetadata().getRevision();
//                String []Parts = strOAIID.split(":");
//                System.out.println("Page Title =" + TestTitle + ", Page ID = " + Parts[Parts.length-1] + ", Revision = " + strRevision);
//                //System.out.println(((Record) record).getMetadata());
//                System.out.println("////////////////CONTENT//////////////");
//                //System.out.println(((Record) record).getContent().getXml());
//
//                WikiPage p = new WikiPage(TestTitle,Long.parseLong(Parts[Parts.length-1]) ,Long.parseLong(strRevision),
//                        ((Record) record).getContent().getText());
//                //System.out.println(p.toXML());
//                SimpleWikiParser parser = new SimpleWikiParser();
//                PageNode pageNode = parser.apply(p);
//
//                //System.out.println(Namespace);
//                //AbstractExtractor extra = new AbstractExtractor(null);
//                //extra.apply(parser.apply(p));
//                //new AbstractExtractor(parser.apply(p));
//                //OntologyReader rdr = new OntologyReader();
//
//                LiveExtractionManager.extractFromPage(p.toXML());
//                //XMLSource.fromXML(
//                System.out.println(p.toXML().getClass());
//
//                //ExtractionContext ec = new ExtractionContext();
//
//
//                //System.out.println(pageNode);
//                //PageNode n = new PageNode(TestTitle, p.id, p.revision, false, false, )
//                System.out.println("//////////////////////////////");
//                return;
//                ///////////////////////////End of my code//////////////////////////
//                */
//			}
//			else if(record instanceof DeletionRecord) {
//				DeletionRecord x = (DeletionRecord)record;
//				System.out.println(x.getOaiId());
//			}
//			else {
//				throw new RuntimeException("Should not happen");
//			}
//		}


		//IHandler<DeletionRecord> deletionWorkflow = getDeletionWorkflow(ini);
		//IHandler<Record> workflow = getWorkflow(ini);


		// Set up the filter
		//IFilter<RecordMetadata> metadataFilter = new DefaultDbpediaMetadataFilter();

		//StatisticWrapperFilter<RecordMetadata> filterStats = new StatisticWrapperFilter<RecordMetadata>(metadataFilter);
		//metadataFilter = filterStats;


		/*
		if(throughputEnabled) {
			metadataFilter =
				new StatisticWriterFilter<RecordMetadata>(
						metadataFilter,
						new FileWriter(
								throughputFile,
								throughputAppend), throughputInterval);
		}
		*/
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
                        int val = _instancesUpdatedIn5Minutes > 0? _instancesUpdatedIn5Minutes : instancesUpdatedIn5Minutes;
                        writer.write(val + "\r\n");

                        //We should write _instancesUpdatedInHour if it's not 0, otherwise we write instancesUpdatedInHour
                        //as the application may have started running less than an hour ago
                        val = _instancesUpdatedInHour > 0? _instancesUpdatedInHour : instancesUpdatedInHour;
                        writer.write(val + "\r\n");

                        //We should write _instancesUpdatedInDay if it's not 0, otherwise we write instancesUpdatedInDay
                        //as the application may have started running less than a day ago
                        val = _instancesUpdatedInDay > 0? _instancesUpdatedInDay : instancesUpdatedInDay;
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

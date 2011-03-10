package org.dbpedia.extraction.live.publisher;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import org.dbpedia.extraction.live.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.SAXParser;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by IntelliJ IDEA.
 * Date: Oct 31, 2010
 * Time: 10:59:53 AM
 * This class publishes the triples (added and deleted) to files in order to enable synchronizing our live end-point with
 * other end-points
 * It is originally developed by Claus Stadler   
 */

public class Publisher extends Thread{

	private static final Logger logger = LoggerFactory.getLogger(Publisher.class);

//    protected static Options cliOptions;

	private Map<String, String> config;

//	private IUpdateStrategy diffStrategy;


	private long sequenceNumber = 0;

    //This member is used to determine whether we have advanced to another hour, so we should reset sequenceNumber
    private int hourNumber = -1;

	private String graphName;

	private String publishDiffBaseName;

	private SAXParser parser = createParser();

    Publisher liveSync;

//	private ISparulExecutor graphDAO;
//
//	private ChangeSink workFlow;
//
//	private NodePositionDAO nodePositionDao;



	//private RDFDiffWriter rdfDiffWriter;

    public Publisher(String name, int priority){
        this.setPriority(priority);
        this.setName(name);
        initSync();
        start();
    }

    public Publisher(String name){
        this(name, Thread.NORM_PRIORITY);
    }

    public Publisher(){
        this("Publisher", Thread.NORM_PRIORITY);
    }

	public static Map<String, String> loadIniFile(File file)
		throws IOException
	{
		Map<String, String> config = new HashMap<String, String>();

		loadIniFile(file, config);

		return config;
	}

	public static void loadIniFile(File file, Map<String, String> out)
		throws IOException
	{
		loadIniFile(new BufferedReader(new FileReader(file)), out);
	}

	public static void loadIniFile(BufferedReader reader, Map<String, String> out)
		throws IOException
	{
		String SOURCE = "source";
		Pattern pattern = Pattern.compile("\\s*([^=]*)\\s*=\\s*(.*)\\s*");

		String line;
		List<String> loadFileNames = new ArrayList<String>();

		String tmp = "";

		while((line = reader.readLine()) != null) {
			line.trim();
			if(line.startsWith(SOURCE)) {
				String fileName = line.substring(SOURCE.length()).trim();

				loadFileNames.add(fileName);

			} else {
				Matcher m = pattern.matcher(line);
				if(m.find()) {
					String key = m.group(1);
					String value = m.group(2);

//					value = StringUtil.strip(value, "\"").trim();

					out.put(key, value);
				}
			}
		}

		System.out.println(tmp);
		System.out.println(loadFileNames);

		for(String loadFileName : loadFileNames) {
			File file = new File(loadFileName);
			loadIniFile(file, out);
		}
	}


	public Publisher(Map<String, String> config)
		throws Exception
	{
		this.config = config;
//
		publishDiffBaseName = config.get("publishDiffRepoPath");
//
//		//LiveRDFDeltaPluginFactory factory.create();
//		Connection conn = VirtuosoUtils.connect(
//				config.get("rdfStore_hostName"),
//				config.get("rdfStore_userName"),
//				config.get("rdfStore_passWord"));
//
//		graphName = config.get("rdfStore_graphName");
//
//		graphDAO =
//			new VirtuosoJdbcSparulExecutor(conn, graphName);
//
//
//		Connection nodeConn = PostGISUtil.connectPostGIS(
//				config.get("osmDb_hostName"),
//				config.get("osmDb_dataBaseName"),
//				config.get("osmDb_userName"),
//				config.get("osmDb_passWord"));
//
//		//RDFDiffWriter rdfDiffWriter = new RDFDiffWriter(outputBaseName);
//		//rdfDiffWriter = new RDFDiffWriter();
//
//		InMemoryTagMapper tagMapper = new InMemoryTagMapper();
//
//		tagMapper.load(new File(config.get("tagMappings")));
//
//		boolean loadTagMappingsFromDb = false;
//		if(loadTagMappingsFromDb) {
//			Session session = TagMappingDB.getSession();
//			Transaction tx = session.beginTransaction();
//
//			for(Object o : session.createCriteria(AbstractTagMapperState.class).list()) {
//				IOneOneTagMapper item = TagMapperInstantiator.getInstance().instantiate((IEntity)o);
//
//				tagMapper.add(item);
//			}
//
//			tx.commit();
//		}
//
//		//File diffRepo = new File("/tmp/lgddiff");
//		//diffRepo.mkdirs();
//
//		//RDFDiffWriter rdfDiffWriter = new RDFDiffWriter(diffRepo, 0);
//
//		ILGDVocab vocab = new LGDVocab();
//		ITransformer<Entity, Model> entityTransformer =
//			new OSMEntityToRDFTransformer(tagMapper, vocab);
//
//		nodePositionDao = new NodePositionDAO("node_position");
//		nodePositionDao.setConnection(nodeConn);
//
//
//		GeoRSSNodeMapper nodeMapper = new GeoRSSNodeMapper(vocab);
//		RDFNodePositionDAO rdfNodePositionDao = new RDFNodePositionDAO(nodePositionDao, vocab, nodeMapper);
//
//
//		diffStrategy = new IgnoreModifyDeleteDiffUpdateStrategy(
//				vocab, entityTransformer, graphDAO, graphName, rdfNodePositionDao);
//
//		// Load the entity tag filter
//		TagFilter entityTagFilter = new TagFilter();
//		entityTagFilter.load(new File(config.get("entityFilter")));
//
//		EntityFilterPlugin entityFilterPlugin = new EntityFilterPlugin(new EntityFilter(entityTagFilter));
//
//		TagFilter tagFilter = new TagFilter();
//		tagFilter.load(new File(config.get("tagFilter")));
//
//		TagFilterPlugin tagFilterPlugin = new TagFilterPlugin(tagFilter);
//
//		tagFilterPlugin.setChangeSink(diffStrategy);
//		entityFilterPlugin.setChangeSink(tagFilterPlugin);
//
//		workFlow = entityFilterPlugin;
	}

    public void initSync(){
        try{
                String configFileName  = "./live/dbpedia_default.ini";
                File configFile = new File(configFileName);

                Map<String, String> config = loadIniFile(configFile);
        //        Map<String, String> config = new HashMap<String, String>();

//                File osmConfigFile = new File(config.get("osmReplicationConfigPath") + "/configuration.txt");

        //        File osmConfigFile = new File("D:\\Leipzig University\\DBpediaExtraction\\live\\config.ini");
//                loadIniFile(osmConfigFile, config);


//                System.out.println(config);

                liveSync = new Publisher(config);
//                run();

            }
            catch (Exception exp){
                logger.error("Publisher cannot be initialized ");
            }
    
    }


	public void run()
	{
//		PropertyConfigurator.configure("log4j.properties");

//		initCLIOptions();

//		CommandLineParser cliParser = new GnuParser();
//		CommandLine commandLine = cliParser.parse(cliOptions, args);
//
//		String configFileName = commandLine.getOptionValue("c", "config.ini");


		long stepCount = 0;
		long totalStartTime = System.nanoTime();
		while(true) {
			try {
                if(!Main.publishingDataQueue.isEmpty()){
                
                    long stepStartTime = System.nanoTime();
                    System.out.println("Inside Publisher");
    
                    ++stepCount;
                    liveSync.step();

                    long now = System.nanoTime();
                    double stepDuration = (now - stepStartTime) / 1000000000.0;
                    double totalDuration = (now - totalStartTime) / 1000000000.0;
                    double avgStepDuration = totalDuration / stepCount;
                    logger.info("Step #" + stepCount + " took " + stepDuration + "sec; Average step duration is " + avgStepDuration + "sec.");
                }

			} catch(Throwable t) {
				logger.error("An exception was encountered in the Publisher update loop", t);
			}

		}
	}


	private SAXParser createParser() {
//		try {
//			return SAXParserFactory.newInstance().newSAXParser();
//
//		} catch (ParserConfigurationException e) {
//			throw new OsmosisRuntimeException("Unable to create SAX Parser.", e);
//		} catch (SAXException e) {
//			throw new OsmosisRuntimeException("Unable to create SAX Parser.", e);
//		}
        return null;
	}

	private void step()
		throws Exception
	{
		// Load the state config
        String filename = config.get("osmReplicationConfigPath") + "/state.txt";
//        String filename = "./live/state.txt";
		File osmStateFile = new File(filename);
//        File osmStateFile = new File("D:/Leipzig University/DBpediaExtraction/live/state.txt");
		loadIniFile(osmStateFile, config);
//		sequenceNumber = Long.parseLong(config.get("sequenceNumber"));

//		logger.info("Processing: " + sequenceNumber);

//		DiffResult diff = computeDiff(sequenceNumber);


//		logger.info("Applying main diff (added/removed) = " + diff.getMainDiff().getAdded().size() + "/" + diff.getMainDiff().getRemoved().size());
//		applyDiff(diff.getMainDiff());
//
//		logger.info("Applying node diff (added/removed) = " + diff.getNodeDiff().getAdded().size() + "/" + diff.getNodeDiff().getRemoved().size());
//		applyNodeDiff(diff.getNodeDiff());
//
//
//		logger.info("Publishing diff");

//		publishDiff(sequenceNumber, diff.getMainDiff());
        publishDiff(sequenceNumber);


		logger.info("Downloading new state");
//		advance(sequenceNumber + 1);
        sequenceNumber++;
	}

	private void advance(long id)
		throws IOException
	{
		URL sourceURL = new URL(config.get("baseUrl") + "/" + getFragment(id) + ".state.txt");
//        URL sourceURL = new URL("http://" + ".state.txt");
//        URL sourceURL = new URL("http://localhost");
		File targetFile = new File(config.get("osmReplicationConfigPath") + "/state.txt");
//        File targetFile = new File("D:/Leipzig University/DBpediaExtraction/live/state.txt");
		URIUtil.download(sourceURL, targetFile);
	}


//	private void applyDiff(IDiff<Model> diff)
//		throws Exception
//	{
//		graphDAO.remove(diff.getRemoved(), graphName);
//		graphDAO.insert(diff.getAdded(), graphName);
//	}


//	public static Map<Long, Point2D> getNodeToPositionMap(Iterable<Node> nodes)
//	{
//		Map<Long, Point2D> result = new TreeMap<Long, Point2D>();
//		for(Node node : nodes) {
//			result.put(node.getId(), new Point2D.Double(node.getLongitude(), node.getLatitude()));
//		}
//		return result;
//	}

//	private void applyNodeDiff(TreeSetDiff<Node> diff)
//		throws SQLException
//	{
//		nodePositionDao.remove(getNodeToPositionMap(diff.getRemoved()).keySet());
//		nodePositionDao.updateOrInsert(getNodeToPositionMap(diff.getAdded()));
//	}


	private void publishDiff(long id)//, IDiff<Model> diff)
		throws IOException
	{
        Calendar  currentDateCalendar= Calendar.getInstance();

//		String fileName = publishDiffBaseName + "/" + getFragment(id);
        //If we advance to another hour, then we should reset sequenceNumber
        if(hourNumber != currentDateCalendar.get(Calendar.HOUR_OF_DAY))
        {
            hourNumber = currentDateCalendar.get(Calendar.HOUR_OF_DAY);
            sequenceNumber = id = 0;
        }
        String fileName = publishDiffBaseName + "/" + currentDateCalendar.get(Calendar.YEAR) + "/"
                + (currentDateCalendar.get(Calendar.MONTH)+1) + "/" + currentDateCalendar.get(Calendar.DAY_OF_MONTH) + "/"
                + currentDateCalendar.get(Calendar.HOUR_OF_DAY) +  "/"
                 + format(sequenceNumber);
		File parent = new File(fileName).getParentFile();
//        File requiredFolders = new File(fileName);
		if(parent != null)
			parent.mkdirs();

		RDFDiffWriter rdfDiffWriter = new RDFDiffWriter(fileName);
        Model addedTriplesModel = ModelFactory.createDefaultModel();

        Main.publishingDataQueue.remove(null);

        PublishingData pubData = Main.publishingDataQueue.poll();
        /////////////////////////////////////////////////////////////////////////////////////
//        String personURI    = "http://somewhere/JohnSmith";
//        String givenName    = "John";
//        String familyName   = "Smith";
//        String fullName     = givenName + " " + familyName;
//
//        // create the resource
////   and add the properties cascading style
//Resource johnSmith
//  = addedTriplesModel.createResource(personURI)
//         .addProperty(VCARD.FN, fullName)
//         .addProperty(VCARD.N,
//                      addedTriplesModel.createResource()
//                           .addProperty(VCARD.Given, givenName)
//                           .addProperty(VCARD.Family, familyName));

        /////////////////////////////////////////////////////////////////////////////////////
//        RDFDiffWriter.write(addedTriplesModel, true, fileName, true);
//        if((pubData.triplesModel == null) || (pubData.triplesModel.size() == 0))
        if(pubData!=null){
            RDFDiffWriter.write(pubData.triplesString, false, fileName, true);
//        else
            RDFDiffWriter.write(pubData.triplesModel, true, fileName, true);
        }
//		rdfDiffWriter.write(diff);
		//RDFDiffWriter.writ
	}

	/*
	private void getCurrentState() {
		//ReplicationState repState = new ReplicationState()
	}
	*/

	private String format(long value) {
//		String result = Long.toString(value);
        String result = String.format("%06d", value);
        /*
        if(value < 1000)
            result = "0" + result;

		if(value < 100)
			result = "0" + result;

		if(value < 10)
			result = "0" + result;
          */

		return result;
	}

	String getFragment(long id)
	{
		List<Long> parts = RDFDiffWriter.chunkValue(id, 1000, 1000);

		String fragment = ""; //Long.toString(parts.get(0));
		for(Long part : parts) {
			fragment += "/" + format(part);
		}

		return fragment;
	}

	InputStream getChangeSetStream(long id)
		throws IOException
	{
		URL url = getChangeSetURL(id);
		return url.openStream();
	}

	File getChangeFile(long id)
		throws IOException
	{
		URL url = getChangeSetURL(id);
		File file = new File(config.get("tmpPath") + ".diff.osc.gz");

//		URIUtil.download(url, file);

		return file;
	}

	URL getStateURL(long id)
		throws MalformedURLException
	{
		return new URL(getBaseURL(id) + ".state.txt");
	}

	URL getChangeSetURL(long id)
		throws MalformedURLException
	{
		return new URL(getBaseURL(id) + ".osc.gz");
	}

	String getBaseURL(long id)
	{

		String urlStr = config.get("baseUrl") + "/" + getFragment(id);

		return urlStr;
	}


	/*
	private void downloadChangeSet()
	{
		//URIUtil.download(
	}*/

//	private DiffResult computeDiff(long id)
//		throws SAXException, IOException
//	{
//		InputStream inputStream = getChangeSetStream(id);
//		inputStream = new CompressionActivator(CompressionMethod.GZip).createCompressionInputStream(inputStream);
//
//		parser.parse(inputStream, new OsmChangeHandler(workFlow, true));
//
//		//diffStrategy.complete();
//		workFlow.complete();
//
//		IDiff<Model> diff = diffStrategy.getMainGraphDiff();
//		TreeSetDiff<Node> nodeDiff = diffStrategy.getNodeDiff();
//
//		//diffStrategy.release();
//		workFlow.release();
//
//		return new DiffResult(diff, nodeDiff);
//	}

	/*************************************************************************/
	/* Init                                                                  */
	/*************************************************************************/
//	private static void initCLIOptions()
//	{
//		cliOptions = new Options();
//
//		cliOptions.addOption("c", "config", true, "Config filename");
//	}

}

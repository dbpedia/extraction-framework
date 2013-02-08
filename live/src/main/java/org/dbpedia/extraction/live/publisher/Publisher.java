package org.dbpedia.extraction.live.publisher;

import org.dbpedia.extraction.live.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    HashSet<String> addedTriples = new HashSet<String>();
    HashSet<String> deletedTriples = new HashSet<String>();

//    protected static Options cliOptions;

    private Map<String, String> config;

//	private IUpdateStrategy diffStrategy;


    private long sequenceNumber = 0;
    private static long fileNumber = 0;

    //This member is used to determine whether we have advanced to another hour, so we should reset sequenceNumber
    private static int hourNumber = -1;

    private String graphName;

    private String publishDiffBaseName;

//	private SAXParser parser = createParser();

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
        BufferedReader rdr = new BufferedReader(new FileReader(file));
        loadIniFile(rdr, out);
        rdr.close();
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

        for(String loadFileName : loadFileNames) {
            File file = new File(loadFileName);
            loadIniFile(file, out);
        }
    }


    public static String getLastPublishDate(String strFileName)
    {
        String strLastResponseDate = null;
        FileInputStream fsLastPublishDateFile = null;

        try{
            fsLastPublishDateFile = new FileInputStream(strFileName);

            // Get the object of DataInputStream
            DataInputStream in = new DataInputStream(fsLastPublishDateFile);
            BufferedReader rdr = new BufferedReader(new InputStreamReader(in));


            int ch;
            strLastResponseDate = "";
            if ((strLastResponseDate = rdr.readLine()) != null)   {
                // Print the content on the console
                logger.info(strLastResponseDate);
            }


        }
        catch(Exception exp){
            logger.warn("Last publish date cannot be read due to " + exp.getMessage());
            logger.info("Assuming the last publish date is now");
            exp.printStackTrace();
        }
        finally {
            try{
                if(fsLastPublishDateFile != null)
                    fsLastPublishDateFile.close();

            }
            catch (Exception exp){
                logger.warn("File " + strFileName + " cannot be closed due to " + exp.getMessage());
            }

        }

        return strLastResponseDate;

    }


    public Publisher(Map<String, String> config)
            throws Exception
    {
        this.config = config;
        publishDiffBaseName = config.get("publishDiffRepoPath");
    }

    public void initSync(){
        try{
            String configFileName  = "./live.ini";
            File configFile = new File(configFileName);

            Map<String, String> config = loadIniFile(configFile);

            liveSync = new Publisher(config);
            String lastPublishedFilename = getLastPublishDate(liveSync.publishDiffBaseName + "/lastPublishedFile.txt");
            setCorrectFileNumber(lastPublishedFilename);

        }
        catch (Exception exp){
            logger.error("Publisher cannot be initialized ");
        }

    }


    private void setCorrectFileNumber(String lastPublishedFilename){

        if(lastPublishedFilename.compareTo("") == 0)
            sequenceNumber = 0;

        //Instantiate a calender for dat comparison
        Calendar  currentDateCalendar= Calendar.getInstance();
        String []parts = lastPublishedFilename.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]);
        int day = Integer.parseInt(parts[2]);
        int hour = Integer.parseInt(parts[3]);

        /*
        publishDiffBaseName + "/" + currentDateCalendar.get(Calendar.YEAR) + "/"
                + String.format("%02d", (currentDateCalendar.get(Calendar.MONTH)+1)) +
                "/" + String.format("%02d",currentDateCalendar.get(Calendar.DAY_OF_MONTH)) + "/"
                + String.format("%02d",currentDateCalendar.get(Calendar.HOUR_OF_DAY)) +  "/"
         */

        //If the date and hour are equal, then the application should start at the last point at which it was stopped.
        if((currentDateCalendar.get(Calendar.HOUR_OF_DAY) == hour) && (currentDateCalendar.get(Calendar.DAY_OF_MONTH) == day)
                && (currentDateCalendar.get(Calendar.MONTH) + 1 == month) && (currentDateCalendar.get(Calendar.YEAR) == year)){

            fileNumber = Integer.parseInt(parts[4]) + 1;
            hourNumber = hour;
        }
        else{
            hourNumber = -1;
            fileNumber = 0;
        }

    }

    public void run()
    {
        while(true) {
            try {
                // Block until next pubData
                DiffData pubData = Main.publishingDataQueue.take();
                liveSync.step(pubData);
            } catch(Throwable t) {
                logger.error("An exception was encountered in the Publisher update loop", t);
            }

        }
    }

    private void step(DiffData pubData)
            throws Exception
    {
        // Load the state config
        String filename = config.get("osmReplicationConfigPath") + "/state.txt";
        File osmStateFile = new File(filename);
        loadIniFile(osmStateFile, config);

        publishDiff(pubData, sequenceNumber);

        //logger.info("Downloading new state");
        sequenceNumber++;
    }

    private void advance(long id)
            throws IOException
    {
        URL sourceURL = new URL(config.get("baseUrl") + "/" + getFragment(id) + ".state.txt");
        File targetFile = new File(config.get("osmReplicationConfigPath") + "/state.txt");

        URIUtil.download(sourceURL, targetFile);
    }


    private void publishDiff(DiffData pubData, long id)//, IDiff<Model> diff)
            throws IOException
    {
        Calendar  currentDateCalendar= Calendar.getInstance();

        //If we advance to another hour, then we should reset sequenceNumber
        if(hourNumber != currentDateCalendar.get(Calendar.HOUR_OF_DAY))
        {
            hourNumber = currentDateCalendar.get(Calendar.HOUR_OF_DAY);
            sequenceNumber = id = 0;
            fileNumber = 0;
        }

        String fileName = publishDiffBaseName + "/" + currentDateCalendar.get(Calendar.YEAR) + "/"
                + String.format("%02d", (currentDateCalendar.get(Calendar.MONTH)+1)) +
                "/" + String.format("%02d",currentDateCalendar.get(Calendar.DAY_OF_MONTH)) + "/"
                + String.format("%02d",currentDateCalendar.get(Calendar.HOUR_OF_DAY)) +  "/"
                + format(fileNumber);
        //logger.info("Publishing data path = " + fileName);


        File parent = new File(fileName).getParentFile();

        if(parent != null)
            parent.mkdirs();

        RDFDiffWriter rdfDiffWriter = new RDFDiffWriter(fileName);





        if(pubData != null){
            addedTriples.addAll(pubData.toAdd);
            deletedTriples.addAll(pubData.toDelete);
        }

        if(sequenceNumber % 300 == 0){

            StringBuilder addString = new StringBuilder();
            for (String s: addedTriples ) {
                addString.append(s);
                addString.append('\n');
            }
            RDFDiffWriter.write(addString.toString(), true, fileName, true);
            addedTriples.clear();

            StringBuilder delString = new StringBuilder();
            for (String s: deletedTriples ) {
                delString.append(s);
                delString.append('\n');
            }
            RDFDiffWriter.write(delString.toString(), false, fileName, true);
            deletedTriples.clear();

            fileNumber++;
            writeLastPublishedFileSequence();
        }

    }

    /**
     * Writes the publication date in the format Year-Month-Day-Hour-Counter, in a file called lastPublishedFile.txt
     * near to lastProcessingDate.dat file in order to ease the process of pulling updates and synchronizing another store
     */
    private void writeLastPublishedFileSequence(){

        Calendar  currentDateCalendar= Calendar.getInstance();

        String lastPublishedFile =  currentDateCalendar.get(Calendar.YEAR) + "-"
                + String.format("%02d", (currentDateCalendar.get(Calendar.MONTH)+1)) +
                "-" + String.format("%02d",currentDateCalendar.get(Calendar.DAY_OF_MONTH)) + "-"
                + String.format("%02d",currentDateCalendar.get(Calendar.HOUR_OF_DAY)) +  "-"
                + format(fileNumber);

        FileOutputStream fsLastProcessingDateFile = null;
        OutputStreamWriter osWriter = null;
        try{
            fsLastProcessingDateFile = new FileOutputStream(publishDiffBaseName + "/lastPublishedFile.txt");
            osWriter = new OutputStreamWriter(fsLastProcessingDateFile);
            osWriter.write(lastPublishedFile);
            osWriter.close();

        }
        catch (Exception exp){
            logger.warn("The date of last Processing process cannot be written to file.\n");
        }
        finally {
            try{
                if(osWriter != null)
                    osWriter.close();

                if(fsLastProcessingDateFile != null)
                    fsLastProcessingDateFile.close();
            }
            catch (Exception exp){
                logger.error("File lastPublishedFile.txt cannot be closed due to " + exp.getMessage());
            }
        }
    }


    private String format(long value) {
        String result = String.format("%06d", value);

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

}

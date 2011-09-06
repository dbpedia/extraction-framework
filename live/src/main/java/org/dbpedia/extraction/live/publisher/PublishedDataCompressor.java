package org.dbpedia.extraction.live.publisher;

import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.log4j.Logger;
import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarOutputStream;
import org.dbpedia.extraction.live.core.LiveOptions;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: 4/6/11
 * Time: 8:54 PM
 * Compresses the files generated for publishing updates on sequential order, i.e. when an hour passes, it compresses the folder containing the updates
 * published during the previous hour and then moves up to also compress the folder of te whole day, and moves up to compress folder of the whole
 * month if necessary and so on.
 */
public class PublishedDataCompressor extends Thread{
    Logger logger = Logger.getLogger(PublishedDataCompressor.class);

    private Date lastProcessingDate = new Date();
    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-HH");

    //This thread should have a low priority, in order not interfere with or slow down the main thread, which is responsible for handling live updates.
    public PublishedDataCompressor(String name, int priority){

        /*writeLastProcessingDateFromFile("/home/mohamed/LeipzigUniversity/dbpedia_publish/lastProcessingDate.dat",
                new Date());*/

//        compressPublishData(new Date());

        this.setPriority(priority);
        this.setName(name);
        this.start();
    }

    public PublishedDataCompressor(String name){
        this(name, Thread.MIN_PRIORITY);
    }

    public PublishedDataCompressor(){
        this("PublishedDataCompressor", Thread.MIN_PRIORITY);
    }

    public void run() {
        //Run should work every hour only and sleep the rest of time, as it is not needed

        try{

//            Map<String, String> config = Publisher.loadIniFile(new File("./live/dbpedia_default.ini"));
//            String publishBaseName = config.get("publishDiffRepoPath");
//            compressYear("2010-04-08-11");

            String publishBaseName = LiveOptions.options.get("publishDiffRepoPath");
            String lastProcessingDateFilename = publishBaseName + "/lastProcessingDate.dat";
            lastProcessingDate = _readLastProcessingDateFromFile(lastProcessingDateFilename);

            while(true){

                try{
                //Prepare year, month, day, adn hour for last compression date, and current date
                CompressionDate lastCompressionDate = new CompressionDate(lastProcessingDate);
                CompressionDate currentCompressionDate = new CompressionDate(new Date());

                if(currentCompressionDate.getYear() != lastCompressionDate.getYear())
                    compressYear(dateFormatter.format(lastProcessingDate));
                else if(currentCompressionDate.getMonth() != lastCompressionDate.getMonth())
                    compressMonth(dateFormatter.format(lastProcessingDate));
                else if(currentCompressionDate.getDay() != lastCompressionDate.getDay())
                    compressDay(dateFormatter.format(lastProcessingDate));
                else if(currentCompressionDate.getHour() != lastCompressionDate.getHour())
                    compressHour(dateFormatter.format(lastProcessingDate));
                else //No difference, so we should do nothing
                     continue;
//                compressPublishData(lastProcessingDate);
                _writeLastProcessingDateToFile(lastProcessingDateFilename);
                lastProcessingDate = _readLastProcessingDateFromFile(lastProcessingDateFilename);
                }
                catch (Exception compressionException){

                }
            }

        }
        catch(Exception exp){
            logger.error("Published data cannot be compressed due to " + exp.getMessage());
        }
        finally {
            try{
//                TimeUnit.MINUTES.sleep(60);
            }
            catch(Exception exp){
                logger.error("Thread cannot be stopped for 1 hour");
            }
        }
    }

    /**
     * Performs the actual Processing process after checking the dates to make sure of the folders that should be compressed
     * @param lastProcessing   The date of last Processing process.
     */
    private void compressPublishData(Date lastProcessing) {

        Date currentDate = new Date();
//        int year = Calendar.getInstance().get(Calendar.YEAR);
        String strCurrentDate = dateFormatter.format(currentDate);


        //Split the two dates, to get year, month, day, and hour individually
        String []currentDateParts = strCurrentDate.split("-");
        int currentYear = Integer.parseInt(currentDateParts[0]);
        int currentMonth = Integer.parseInt(currentDateParts[1]);
        int currentDay = Integer.parseInt(currentDateParts[2]);
        int currentHour = Integer.parseInt(currentDateParts[3]);

        String []lastProcessingDateParts = dateFormatter.format(lastProcessing).split("-");
        int lastProcessingYear = Integer.parseInt(lastProcessingDateParts[0]);
        int lastProcessingMonth = Integer.parseInt(lastProcessingDateParts[1]);
        int lastProcessingDay = Integer.parseInt(lastProcessingDateParts[2]);
        int lastProcessingHour = Integer.parseInt(lastProcessingDateParts[3]);


        if(currentYear != lastProcessingYear){
            compressYear(dateFormatter.format(lastProcessing));
        }
        else if(currentMonth != lastProcessingMonth){
            compressMonth(dateFormatter.format(lastProcessing));
        }
        else if(currentDay != lastProcessingDay){
            compressDay(dateFormatter.format(lastProcessing));
        }
        else if(currentHour != lastProcessingHour){
            compressHour(dateFormatter.format(lastProcessing));
        }



    }
    /**
     * Reads the date of last Processing process performed by the system  from a specific file
     * @param filename  The name of file from which we read the date
     * @return The date of last Processing read from file
     */
    private Date _readLastProcessingDateFromFile(String filename){
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-HH");
        BufferedReader inputReader = null;

        try{
            inputReader = new BufferedReader(new FileReader(filename));
            String line = inputReader.readLine();
            inputReader.close();
            return dateFormatter.parse(line);
        }
        catch (Exception exp){
            logger.warn("The date of last Processing process cannot be read from file.\n" +
                    "Assuming it is the current date");
            return new Date();

        }
        finally {
            try{
                if(inputReader != null)
                    inputReader.close();

            }
            catch (Exception exp){
                logger.error("File " + filename + " cannot be closed due to " + exp.getMessage());
            }
        }


    }

    /**
     * Reads the date of last Processing process performed by the system  from a specific file
     * @param filename  The name of file from which we read the date
     * @return The date of last Processing read from file
     */
    private void _writeLastProcessingDateToFile(String filename){
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-HH");

        FileOutputStream fsLastProcessingDateFile = null;
        OutputStreamWriter osWriter = null;
        try{
            fsLastProcessingDateFile = new FileOutputStream(filename);
            osWriter = new OutputStreamWriter(fsLastProcessingDateFile);
            osWriter.write(dateFormatter.format(new Date()));
            osWriter.close();

//            BufferedReader inputReader = new BufferedReader(new FileReader(filename));
//            String line = inputReader.readLine();
//            inputReader.close();
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
                logger.error("File " + filename + " cannot be closed due to " + exp.getMessage());
            }
        }


    }


    /**
     * writes the date of last Processing process performed by the system to a specific file
     * @param filename  The name of file from which we write the date
     * @param  lastProcessing  The date of last Processing that will be written to the file
     */
    private void writeLastProcessingDateFromFile(String filename, Date lastProcessing){
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-HH");

        FileWriter writer = null;

        try{
            writer = new FileWriter(filename);
            writer.write(dateFormatter.format(lastProcessing));
            writer.flush();
            writer.close();

        }
        catch (Exception exp){
            logger.warn("The date of last Processing process cannot be written to file.");
        }
        finally {
            try{
                if(writer != null)
                    writer.close();

            }
            catch (Exception exp){
                logger.error("File " + filename + " cannot be closed due to " + exp.getMessage());
            }

        }
    }

    /**
     * Compresses  year folder.
     * @param lastProcessing   The date of last Processing
     */
    private void compressYear(String lastProcessing){

        //If we want to compress a  month, then we should compress month, day, and hour folders, as definitely they should differ
        compressMonth(lastProcessing);
        compressDay(lastProcessing);
        compressHour(lastProcessing);


        String []lastProcessingParts = lastProcessing.split("-");

        //The path of the required hour is year/month
        String yearPath = LiveOptions.options.get("publishDiffRepoPath") + "/" + lastProcessingParts[0];

        //The output filename is year.tar.gz
        String tarOutputFilename = lastProcessingParts[0] + ".tar";

        tarOutputFilename =  LiveOptions.options.get("publishDiffRepoPath") + "/" + tarOutputFilename;

        try{
            //Convert it into TAR first
            /*convertToTar(new File(yearPath), new TarOutputStream(
                                        new FileOutputStream(new File(tarOutputFilename))));*/

            TarOutputStream osTarYear =  new TarOutputStream(new FileOutputStream(new File(tarOutputFilename)));
            convertToTar(new File(yearPath), osTarYear);
            osTarYear.close();

            compressFileUsingGZip(tarOutputFilename);

        }
        catch(IOException exp){
            logger.error("TAR output file for " + yearPath + " cannot be created. Compression process cannot continue");
            return;
        }
    }

    private void compressMonth(String lastProcessing){

       //If we want to compress a  month, then we should compress month, day, and hour folders, as definitely they should differ
        compressDay(lastProcessing);
        compressHour(lastProcessing);


        String []lastProcessingParts = lastProcessing.split("-");

        //The path of the required hour is year/month
        String monthPath = LiveOptions.options.get("publishDiffRepoPath") + "/" + lastProcessingParts[0]
                + "/" + lastProcessingParts[1] ;

        //The output filename is year-month.tar.gz
        String tarOutputFilename = lastProcessingParts[0] + "-" + lastProcessingParts[1] + ".tar";

        tarOutputFilename =  LiveOptions.options.get("publishDiffRepoPath") + "/" + lastProcessingParts[0]
                + "/" + tarOutputFilename;

        try{
            //Convert it into TAR first
            TarOutputStream osTarMonth =  new TarOutputStream(new FileOutputStream(new File(tarOutputFilename)));
            convertToTar(new File(monthPath), osTarMonth);
            osTarMonth.close();

            /*convertToTar(new File(monthPath), new TarOutputStream(
                                        new FileOutputStream(new File(tarOutputFilename))));*/
            compressFileUsingGZip(tarOutputFilename);

        }
        catch(IOException exp){
            logger.error("TAR output file for " + monthPath + " cannot be created. Compression process cannot continue");
            return;
        }

    }

    private void compressDay(String lastProcessing){
        //If we want to compress a  day, then we should compress day, and hour folders, as definitely they should differ
        compressHour(lastProcessing);

        String []lastProcessingParts = lastProcessing.split("-");

        //The path of the required hour is year/month/day
        String dayPath = LiveOptions.options.get("publishDiffRepoPath") + "/" + lastProcessingParts[0]
                + "/" + lastProcessingParts[1] + "/" + lastProcessingParts[2] ;

        //The output filename is year-month-day.tar.gz
        String tarOutputFilename = lastProcessingParts[0] + "-" + lastProcessingParts[1] + "-" + lastProcessingParts[2] +
                ".tar";

        tarOutputFilename =  LiveOptions.options.get("publishDiffRepoPath") + "/" + lastProcessingParts[0]
                + "/" + lastProcessingParts[1] + "/" +  tarOutputFilename;

        try{
            //Convert it into TAR first
            TarOutputStream osTarDay =  new TarOutputStream(new FileOutputStream(new File(tarOutputFilename)));
            convertToTar(new File(dayPath), osTarDay);
            osTarDay.close();
            /*convertToTar(new File(dayPath), new TarOutputStream(
                                        new FileOutputStream(new File(tarOutputFilename))));*/
            compressFileUsingGZip(tarOutputFilename);

        }
        catch(IOException exp){
            logger.error("TAR output file for " + dayPath + " cannot be created. Compression process cannot continue");
            return;
        }


    }

    private void compressHour(String lastProcessing){
        String []lastProcessingParts = lastProcessing.split("-");

        //The path of the required hour is year/month/day/hour
        String hourPath = LiveOptions.options.get("publishDiffRepoPath") + "/" + lastProcessingParts[0]
                + "/" + lastProcessingParts[1] + "/" + lastProcessingParts[2] + "/" + lastProcessingParts[3];

        //The output filename is year-month-day-hour.tar.gz
        String tarOutputFilename = lastProcessingParts[0] + "-" + lastProcessingParts[1] + "-" + lastProcessingParts[2] +
                "-" + lastProcessingParts[3]+".tar";
        tarOutputFilename =  LiveOptions.options.get("publishDiffRepoPath") + "/" + lastProcessingParts[0]
                + "/" + lastProcessingParts[1] + "/" + lastProcessingParts[2] + "/" + tarOutputFilename;

        try{
            //Convert it into TAR first

            TarOutputStream osTarHour =  new TarOutputStream(new FileOutputStream(new File(tarOutputFilename)));
            convertToTar(new File(hourPath), osTarHour);
            osTarHour.close();

            compressFileUsingGZip(tarOutputFilename);

        }
        catch(IOException exp){
            logger.error("TAR output file for " + hourPath + " cannot be created. Compression process cannot continue");
            return;
        }
    }


    /**
     * Converts a folder into a single TAR file in order to enable compression, as GZip can only compress single file not a whole folder
     * @param dir   The directory that should be converted into TAR
     * @param tos   The output stream that will contain TAR
     * @throws IOException  If the folder not found.
     */
    private static void convertToTar(File dir,TarOutputStream tos) throws IOException {
        File[] flist = dir.listFiles();

        int buffersize = 1024;
        byte[] buf = new byte[buffersize];
        for(int i=0; i<flist.length; i++)
        {
           if(flist[i].isDirectory())
           {
               convertToTar(flist[i], tos);
               continue;
           }

            String abs = dir.getAbsolutePath();
            String fabs = flist[i].getAbsolutePath();
            if(fabs.startsWith(abs))
               fabs = fabs.substring(abs.length());
          FileInputStream fis = new FileInputStream(flist[i]);
                TarEntry te = new TarEntry(fabs);
          te.setSize(flist[i].length());
               tos.setLongFileMode(TarOutputStream.LONGFILE_GNU);
          tos.putNextEntry(te);
          int count = 0;
          while((count = fis.read(buf,0,buffersize)) != -1)
          {
            tos.write(buf,0,count);
          }
          tos.closeEntry();
          fis.close();
       }

    }


    /**
     * Compresses a file using GZip library
     * @param filename  The file the should be compressed.
     */
    private void compressFileUsingGZip(String filename){

         FileInputStream in = null;

         File outputFile = null;
         OutputStream osCompressedFinal = null;
         OutputStream out = null;

        try{
            //Prepare required streams

            in = new FileInputStream(filename);

            outputFile = new File(filename + ".gz");
		    osCompressedFinal = new FileOutputStream(outputFile);
            out = new GzipCompressorOutputStream(osCompressedFinal);

            // Transfer bytes from the input file to the GZIP output stream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();

            // Complete the GZIP file
            out.flush();
            out.close();

            //delete TAR file as it is not need any more
            in.close();
            File tarFileToDelete = new File(filename);
            tarFileToDelete.delete();

            //out.write(new FileInputStream(tarOutputFilename).)
        }
        catch(IOException exp){
            logger.error("File: " + filename + " cannot be compressed");
        }

    }

    /**
     * This class represents
     */
    private class CompressionDate{
        private int _year;
        private int _month;
        private int _day;
        private int _hour;

        public int getYear(){
            return _year;
        }

        public int getMonth(){
            return _month;
        }

        public int getDay(){
            return _day;
        }

        public int getHour(){
            return _hour;
        }

        public CompressionDate(String requiredDate){
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-HH");
    //        int year = Calendar.getInstance().get(Calendar.YEAR);
//            String strCurrentDate = dateFormatter.format(requiredDate);


            //Split the two dates, to get year, month, day, and hour individually
            String []requiredDateParts = requiredDate.split("-");
            _year = Integer.parseInt(requiredDateParts[0]);
            _month = Integer.parseInt(requiredDateParts[1]);
            _day = Integer.parseInt(requiredDateParts[2]);
            _hour = Integer.parseInt(requiredDateParts[3]);
        }

         public CompressionDate(Date requiredDate){
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-HH");
    //        int year = Calendar.getInstance().get(Calendar.YEAR);
             String []requiredDateParts = dateFormatter.format(requiredDate).split("-");


            //Split the two dates, to get year, month, day, and hour individually
//            String []requiredDateParts = strCurrentDate.split("-");
            _year = Integer.parseInt(requiredDateParts[0]);
            _month = Integer.parseInt(requiredDateParts[1]);
            _day = Integer.parseInt(requiredDateParts[2]);
            _hour = Integer.parseInt(requiredDateParts[3]);
        }

        @Override public String toString(){
            return "Year = " + _year + ", Month = " + _month + ", Day = " + _day + ", Hour = " + _hour;
        }

    }
    /*
    public File zipFolder(File inFolder, File outFile)

     {
          try
          {

               //compress outfile stream

               GzipCompressorOutputStream out = new ZipOutputStream(

                                             new BufferedOutputStream(

                                                  new FileOutputStream(outFile)));



               //writting stream

               BufferedInputStream in = null;



               byte[] data    = new byte[BUFFER];

               String files[] = inFolder.list();



               for (int i=0; i<files.length; i++)

               {

                    //System.out.println("Adding: " + files[i]);

                    in = new BufferedInputStream(new FileInputStream(inFolder.getPath() + "/" + files[i]), BUFFER);



                    out.putNextEntry(new ZipEntry(files[i])); //write data header (name, size, etc)

                    int count;

                    while((count = in.read(data,0,BUFFER)) != -1)

                    {

                         out.write(data, 0, count);

                    }

                    out.closeEntry(); //close each entry

               }

               cleanUp(out);

               cleanUp(in);

          }

          catch(Exception e)

          {

               e.printStackTrace();

          }

          return new File(outFile + ".zip");

     }



     private void cleanUp(InputStream in) throws Exception

     {

          in.close();

     }



     private void cleanUp(OutputStream out) throws Exception

     {

          out.flush();

          out.close();

     }
     */
}

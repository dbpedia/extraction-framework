package org.dbpedia.extraction.live.core;

import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.*;


//import static org.hamcrest.CoreMatchers.not;
//import static org.hamcrest.core.Is.is;


/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jun 25, 2010
 * Time: 10:38:38 AM
 * This class is a timer for hashing
 */
public class Timer{
    
    //Initializing the Logger
    private static Logger logger;

    public static HashMap time = new HashMap();
    public static  HashMap start = new HashMap();
    public static long startingTime;

    static
    {
        try
        {
            logger = Logger.getLogger(Class.forName("org.dbpedia.extraction.live.core.Timer").getName());
            //Assert.assertNotNull("The logger of Timer class cannot be null", logger);
        }
        catch (Exception exp){

        }
    }

    //Initializes the starting time of the timer  
    static public void init()
    {
        try
        {
        //logger = Logger.getLogger(Class.forName("Timer").getName());
            
	    startingTime = System.nanoTime();


        //TODO this was originally microtime and we converted it to nanoTime but still not sure that it is correct or not
        }
        catch(Exception exp)
        {

        }
	}

    //@return   the period elapsed since the timer was started
    static public long getElapsedSeconds(){

        return System.nanoTime() - startingTime;
	}

    //TODO the following function cannot be transformed
    static public String timerLabel(String component,Object obj, String rest){
        return component.toString()+ "." + obj.getClass().toString() + "." + rest;
	}

    //Starts the timer with the passed name
    //@param    TimerName   The name of the timer
    public static void start(String TimerName)
    {
        try
        {
            //Assert.assertNotNull("Timer name cannot be null, in function start", TimerName);
            if(start.get(TimerName) != null)
            {
               logger.warn("Timer: "+ TimerName +" already started. overwriting, and thread id is " +
                       Thread.currentThread().getId() + " thread name = " + Thread.currentThread().getName());
            }
            start.put(TimerName, System.nanoTime());
        }
        catch(Exception exp)
        {
            
        }
    }

    //TODO this code must be converted to java valid code
    static public void staticTimer(String name, long timeToAdd)
    {
        //Assert.assertNotNull("Timer name cannot be null, in function staticTimer", name);
        //Assert.assertTrue("timeToAdd must be greateer than 0", timeToAdd != 0);
        check(name);

        HashMap hmTotal = (HashMap)time.get(name);
        long CurrentTotal = Long.parseLong(hmTotal.get("total").toString());
        hmTotal.put("total", CurrentTotal + timeToAdd);

        int CurrentHits = Integer.parseInt(hmTotal.get("hits").toString());
        hmTotal.put("hits", CurrentHits + 1);

    }

    //Stops the timer with the passed name
    //@param    TimerName   The name of the timer
    static public long stop(String TimerName)
    {
        //Assert.assertNotNull("Timer name cannot be null, in function stop", TimerName);
        if(start.get(TimerName) == null)
        {
            logger.warn("Timer: " + TimerName + " was never started. ignoring");
            return -1;
        }
        long before = Long.parseLong(start.get(TimerName).toString());
        //unset(self::$start[$name]);
        //Assert.assertTrue("before time of the timer must be greater than 0", before>0);
        start.remove(TimerName);
        long TimeNeeded = System.nanoTime() - before;

        //TODO this code must be converted to java valid code
        check(TimerName);
        HashMap CurrentMap = (HashMap)time.get(TimerName);

        long CurrentTotal = Long.parseLong(CurrentMap.get("total").toString());
        CurrentMap.put("total", CurrentTotal + TimeNeeded);

        int CurrentHits = Integer.parseInt(CurrentMap.get("hits").toString());
        CurrentMap.put("hits", CurrentHits + 1);

        return TimeNeeded;
    }

    static public String stopAsString(String TimerName)
    {
        long TimeElapsed = stop(TimerName);
        //TODO this rounding may need some revise
        return ", needed: " + Math.round(TimeElapsed*1000) + " ms" ;
	}

	static private void check(String TimerName)
    {
        //Assert.assertNotNull("Timer name cannot be null, in function check", TimerName);
        if(time.get(TimerName) == null)
        {
            time.put(TimerName, new HashMap());
            HashMap CurrentMap = (HashMap)time.get(TimerName);
            CurrentMap.put("total", 0);
            CurrentMap.put("hits", 0);
        }
	}

    //Writes the statistics data to a file    
    static public void writeTimeToFile(String statisticsDirectory) throws SecurityException
    {
        try
        {
            //Assert.assertNotNull("statisticsDirectory cannot be null, in function writeTimeToFile", statisticsDirectory);
            HashMap Overall = new HashMap();
            Overall.put("startingtime", startingTime);
            Overall.put("lasttime", System.nanoTime());

            boolean bDirectoryCreated = (new File(statisticsDirectory)).mkdirs();
            if(!bDirectoryCreated)
                throw new SecurityException("Program unable to create the specified directory");

            if(bDirectoryCreated)
            {
                FileOutputStream File1 = new FileOutputStream(statisticsDirectory + "/time.ser");
                FileOutputStream File2 = new FileOutputStream(statisticsDirectory + "/timeOverall.ser");

                ObjectOutputStream Serializer1 = new ObjectOutputStream(File1);
                ObjectOutputStream Serializer2 = new ObjectOutputStream(File2);

                Serializer1.writeObject(time);
                Serializer2.writeObject(Overall);
                
                File1.close();
                File2.close();
            }
            else
            {
                logger.warn("Statistic directory cannot be created");
            }
        }
        catch(Exception exp)
        {
            logger.warn(exp.getMessage());
        }

    }

    //Precision should be 2 by default
    static public void printTime()
    {
        printTime(2);
    }

    static public void printTime(int precision)
    {
        //Assert.assertTrue("precision cannot be negative, in function printTime", precision>=0);
        String Message = getTimeAsString(precision);

        if(start.size()>0)
        {
            logger.warn("Timer: Unfinished timers:");

            Set KeySet = start.keySet();
            Iterator KeySetIterator  = KeySet.iterator();
            while(KeySetIterator.hasNext())
            {
                logger.warn("Timer: " + KeySetIterator.next());
            }
        }
        logger.info(Message);
	}

    static String getTimeAsString(){
        return getTimeAsString(2);
    }

    static String getTimeAsString(int precision)
    {
        //Assert.assertTrue("precision cannot be negative, in function getTimeAsString", precision>=0);
        String Message = "";

        //Sort the time Hashmap
        TreeMap SortedMap = new TreeMap(time);
        //ksort(self::$time);

        Set keys = SortedMap.keySet();

        for (Iterator i = keys.iterator(); i.hasNext();)
        {
            Integer key = (Integer) i.next();
            HashMap value = (HashMap) SortedMap.get(key);

            //Assert.assertNotNull("Error while sorting time Hashmap, in function getTimeAsString",value);

            String tempString = "";

            //TODO this rounding may need some revise
            long TotalAsLong = Long.parseLong(value.get("total").toString());
            long total = Math.round(TotalAsLong);
            String  percent = getPercentage(TotalAsLong);

            Integer hits = Integer.parseInt(value.get("hits").toString());
            
            Double avg = (double) (Math.round(TotalAsLong*1000)/hits);

            //TODO this rounding may need some revise
            tempString += Util.deck(Math.round(total) +" sec");
            tempString += Util.deck("" + percent + "");

            tempString += Util.deck(hits.toString());
            tempString += Util.deck(avg.toString());

            tempString += Util.deck(key.toString());

            Message += Util.row(tempString,0);
          /*Message += "total: " + total+ " sec\t(" + percent + ")\thits: " + hits +
                    "\tavg: " + avg + "\t" + key +"\n";*/
        }

        return Message;
	}

    private static String getPercentage(long componentTime)
    {
        long total = System.nanoTime() - startingTime ;
        //Assert.assertTrue("Total time cannot be less than or equal to 0, in function getPercentage", total>0);
        String result = Math.round((componentTime / total)*100) + "%";

        return result;
    }

}

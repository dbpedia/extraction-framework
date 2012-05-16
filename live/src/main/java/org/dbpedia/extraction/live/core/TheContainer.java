package org.dbpedia.extraction.live.core;


import org.apache.log4j.Logger;

import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Jul 7, 2010
 * Time: 4:10:44 PM.
 */
public class TheContainer {

    //private static Logger logger = Logger.getLogger(Class.forName("Hash").getName());
    static Logger logger = null;
    static{
        logger = Logger.getLogger(TheContainer.class);
//        Assert.assertNotNull("Logger cannot be null", logger);
    }

    private static HashMap container = new HashMap();

 	public static void set(String label, String content){
         assert (label != null && label != "") : "container label cannot be null or empty";
         //Assert.assertTrue("container content cannot be null or empty", (content != null && content != ""));

         container.put(label, content);
 	}
 	public static boolean wasSet(String label){
 	    return (container.get(label) != null);
 	}

 	public static String get(String label){
         try{

             if(!wasSet(label)){

                logger.error("TheContainer: access to " + label + " failed, not set previously");
             }
             return container.get(label).toString() ;
         }
         catch (Exception exp){

         }
         return "";
 	}             
}

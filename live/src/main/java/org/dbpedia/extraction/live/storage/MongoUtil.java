package org.dbpedia.extraction.live.storage;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.*;

/**
 * Created by Andre Pereira on 02/06/2015.
 */
public class MongoUtil {
    //Initializing the Logger
    private static Logger logger = LoggerFactory.getLogger(MongoUtil.class);
    //Initializing the MongoDB client
    private static MongoClient client = new MongoClient();
    //Get the test database
    private static MongoDatabase database = client.getDatabase("test");
    //Get the cache and revisions collection from the database
    private static MongoCollection<Document> cache = database.getCollection("dbplCache");
    private static MongoCollection<Document> revisions = database.getCollection("dbplRevisions");

    /*
    * Method that updates the item with pageID from the cache
    */
    public static boolean update(long pageID, String title, String times, String json, String subjects, String diff){
        try{
            Document sets = new Document("title", title)
                            .append("timesUpdated", times)
                            .append("json", json)
                            .append("subjects", subjects)
                            .append("updated", now())
                            .append("diff", diff);
            cache.updateOne(eq("pageID", pageID), new Document("$set", sets));
        }catch (Exception e){
            logger.warn(e.getMessage());
            return false;
        }
        return true;
    }

    /*
    * Method that sets the pageID item from the cache as updated but doesn't modify any of the other values
    */
    public static boolean updateUnmodified(long pageID, String times){
        try{
            Document sets = new Document("timesUpdated", times)
                            .append("updated", now());
            cache.updateOne(eq("pageID", pageID), new Document("$set", sets));
        }catch (Exception e){
            logger.warn(e.getMessage());
            return false;
        }
        return true;
    }

    /*
    * Method that sets the pageID item from the cache as updated and adds an error
    */
    public static boolean updateError(long pageID, String error){
        try{
            Document doc = cache.find(eq("pageID", pageID)).first();
            int times = 0;
            if(doc != null)
                times = Integer.parseInt(doc.getString("timesUpdated"));

            Document sets = new Document("timesUpdated", "" + (times + 1))
                                .append("error", error)
                                .append("updated", now());
            cache.updateOne(eq("pageID", pageID), new Document("$set", sets));
        }catch (Exception e){
            logger.warn(e.getMessage());
            return false;
        }
        return true;
    }

    /*
    * This method inserts a new item in cache.
    * Sets the updated field as 0/0/0 00:00:00
    */
    public static boolean insert(long pageID, String title, String times, String json, String subjects, String diff){
        try{
            delete(pageID); //ensure that the document is unique in the database

            Document document = new Document("pageID", pageID)
                                .append("title", title)
                                .append("timesUpdated", times)
                                .append("json", json)
                                .append("subjects", subjects)
                                .append("updated", "0/0/0 00:00:00")
                                .append("diff", diff);
            cache.insertOne(document);
        }catch (Exception e){
            logger.warn(e.getMessage());
            return false;
        }
        return true;
    }

    /*
    * This method inserts a new item in cache with every possible field set
    */
    public static boolean full_insert(long pageID, String title, String updated, String times, String json, String subjects, String diff, int error){
        try{
            delete(pageID); //ensure that the document is unique in the database

            Document document = new Document("pageID", pageID)
                                .append("title", title)
                                .append("timesUpdated", times)
                                .append("json", json)
                                .append("subjects", subjects)
                                .append("updated", updated)
                                .append("diff", diff)
                                .append("error", error);
            cache.insertOne(document);
        }catch (Exception e){
            logger.warn(e.getMessage());
            return false;
        }
        return true;
    }

    /*
    * Method that deletes the item with pageID from cache
    */
    public static boolean delete(long pageID) {
        try{
            cache.deleteMany(eq("pageID", pageID));
        }catch (Exception e){
            logger.warn(e.getMessage());
            return false;
        }
        return true;
    }

    /*
    * This method returns a List<String> with every json field from every item in cache.
    */
    public static List<String> getAll(){
        ArrayList<String> result = new ArrayList<>();

        MongoCursor<Document> cursor = cache.find().iterator();
        try {
            while (cursor.hasNext()) {
                result.add(cursor.next().getString("json"));
            }
        } catch (Exception e){
            logger.warn(e.getMessage());
            result = null;
        } finally {
            cursor.close();
        }
        return result;
    }

    /*
    * Method that returns a JSONCacheItem with information of the item with pageID from the cache
    */
    public static JSONCacheItem getItem(long pageID){
        try {
            Document doc = cache.find(eq("pageID", pageID)).first();
            if(doc != null) {
                int timesUpdated = Integer.parseInt(doc.getString("timesUpdated"));
                String json = doc.getString("json");

                String subjects = doc.getString("subjects");
                Set<String> subjectSet = new HashSet<>();
                for (String item : subjects.split("\n")) {
                    String subject = item.trim();
                    if (!subject.isEmpty())
                        subjectSet.add(org.apache.commons.lang.StringEscapeUtils.unescapeJava(subject));
                }
                return new JSONCacheItem(pageID, timesUpdated, json, subjectSet);
            }
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }
        return null;
    }

    /*
    * This method returns an ArrayList<LiveQueueItem> with up to 'limit' elements.
    * These elements are the items from cache that haven't been updated in 'daysAgo' days.
    */
    public static ArrayList<LiveQueueItem> getUnmodified(int daysAgo, int limit){
        try {
            ArrayList<LiveQueueItem> items = new ArrayList<>(limit);
            FindIterable<Document> docs = cache.find(lt("updated", nowMinusDays(daysAgo)))
                                            .sort(ascending("updated"))
                                            .limit(limit);
            for (Document d: docs){
                    long pageID = d.getLong("pageID");
                    String t = d.getString("updated");
                    DateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    Date date = format.parse(t);
                    String timestamp = DateUtil.transformToUTC(date.getTime());
                    items.add(new LiveQueueItem(pageID, timestamp));
            }
            return items;
        } catch (Exception e) {
            logger.warn(e.getMessage());
            return null;
        }
    }

    /*
    * Method that returns a String with a timestamp regarding a date of now() minus the number of 'days'
    * Format: yyyy/MM/dd HH:mm:ss
    */
    private static String nowMinusDays(int days){
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1 * days);
        return dateFormat.format(cal.getTime());
    }

    /*
    * Method that returns a String with a timestamp regarding a date of now()
    * Format: yyyy/MM/dd HH:mm:ss
    */
    private static String now(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        return dateFormat.format(cal.getTime());
    }

    /*
    * This method inserts a new item in the revisions collection.
    */
    public static boolean rev_insert(long pageID, String timestamp, String additions, String deletions){
        try{
            Document document = new Document("pageID", pageID)
                                .append("timestamp", timestamp)
                                .append("additions", additions)
                                .append("deletions", deletions);
            revisions.insertOne(document);
        }catch (Exception e){
            logger.warn(e.getMessage());
            return false;
        }
        return true;
    }

    public static void closeClient(){
        client.close();
    }

}

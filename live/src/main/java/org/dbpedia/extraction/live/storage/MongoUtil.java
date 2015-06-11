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

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Sorts.*;

/**
 * Created by Andre Pereira on 02/06/2015.
 */
public class MongoUtil {
    //Initializing the Logger
    private static Logger logger = LoggerFactory.getLogger(JDBCUtil.class);
    //Initializing the MongoDB client
    private static MongoClient client = new MongoClient();
    //Get the test database
    private static MongoDatabase database = client.getDatabase("test");
    //Get the cache collection from the database
    private static MongoCollection<Document> cache = database.getCollection("dbplCache");

    public static void test(){
        logger.warn("------ Begin Tests --------");


        logger.warn("------ End Tests --------");
    }

    public static boolean update(long pageID, String title, int times, String json, String subjects, String diff){
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

    public static boolean updateError(long pageID, String times, String error){
        try{
            Document sets = new Document("timesUpdated", times)
                            .append("error", error)
                            .append("updated", now());
            cache.updateOne(eq("pageID", pageID), new Document("$set", sets));
        }catch (Exception e){
            logger.warn(e.getMessage());
            return false;
        }
        return true;
    }

    public static boolean insert(long pageID, String title, int times, String json, String subjects, String diff){
        try{
            delete(pageID); //ensure that the document is unique in the database

            Document document = new Document("pageID", pageID)
                                .append("title", title)
                                .append("timesUpdated", times)
                                .append("json", json)
                                .append("subjects", subjects)
                                .append("updated", "0/0/0 0:0:0")
                    .append("diff", diff);
            cache.insertOne(document);
        }catch (Exception e){
            logger.warn(e.getMessage());
            return false;
        }
        return true;
    }

    public static boolean delete(long pageID) {
        try{
            cache.deleteMany(eq("pageID", pageID));
        }catch (Exception e){
            logger.warn(e.getMessage());
            return false;
        }
        return true;
    }

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

    public static JSONCacheItem getItem(long pageID){
        try {
            Document doc = cache.find(eq("pageID", pageID)).first();
            if(doc != null) {
                int timesUpdated = doc.getInteger("timesUpdated");
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

    private static String nowMinusDays(int days){
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1 * days);
        return dateFormat.format(cal.getTime());
    }

    private static String now(){
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Calendar cal = Calendar.getInstance();
        return dateFormat.format(cal.getTime());
    }

    public static void closeClient(){
        client.close();
    }

}

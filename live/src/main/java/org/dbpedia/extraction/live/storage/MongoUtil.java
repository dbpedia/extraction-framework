package org.dbpedia.extraction.live.storage;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

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
        // find documents
        //List<Document> foundDocument = cache.find().into(new ArrayList<Document>());
        //for(Document d: foundDocument)
        //    logger.warn(d.toString());
        List<String> aux = getJSONCacheSelectAll();
        for(String s: aux)
            logger.warn(s);
    }

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

    public static boolean insert(long pageID, String title, String times, String json, String subjects, String diff){
        try{
            delete(pageID); //ensure that the document is unique in the database

            Document document = new Document("pageID", pageID)
                                .append("title", title)
                                .append("timesUpdated", times)
                                .append("json", json)
                                .append("subjects", subjects)
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

    public static List<String> getJSONCacheSelectAll(){
        ArrayList<String> result = new ArrayList<String>();

        MongoCursor<Document> cursor = cache.find().iterator();
        try {
            while (cursor.hasNext()) {
                result.add(cursor.next().getString("json"));
            }
        } finally {
            cursor.close();
        }
        return result;
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

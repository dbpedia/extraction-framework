package org.dbpedia.extraction.live.storage;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
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
        Document document = new Document("pageID", 445)
                            .append("title", "Teste 1")
                            .append("timesUpdated", "0")
                            .append("json", "{teste: {abc: 'add'}}")
                            .append("subjects", "bananas, testes e coisos");
        cache.insertOne(document);

        // find documents
        List<Document> foundDocument = cache.find().into(new ArrayList<Document>());
        for(Document d: foundDocument)
            logger.warn(d.toString());

        //update(444, "Teste 2", "1", "{teste2: 'coisos'}", "sem banas e com testes 2", "diferença grande");

        delete(445);

        // find documents
        foundDocument = cache.find().into(new ArrayList<Document>());
        for(Document d: foundDocument)
            logger.warn(d.toString());
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

    public static boolean delete(long pageID){
        try{
            cache.deleteOne(eq("pageID", pageID));
        }catch (Exception e){
            logger.warn(e.getMessage());
            return false;
        }
        return true;
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

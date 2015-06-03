package org.dbpedia.extraction.live.storage;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.*;

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
        Document document = new Document("pageID", 444)
                            .append("pageTitle", "Teste 1")
                            .append("timesUpdated", "0")
                            .append("json", "{teste: {abc: 'add'}}")
                            .append("subjects", "bananas, testes e coisos");
        cache.insertOne(document);

        // find documents
        List<Document> foundDocument = cache.find().into(new ArrayList<Document>());
        for(Document d: foundDocument)
            logger.warn(d.toString());

        update(444, "Teste 2", "1", "{teste2: 'coisos'}", "sem banas e com testes 2", "diferença grande");

        // find documents
        foundDocument = cache.find().into(new ArrayList<Document>());
        for(Document d: foundDocument)
            logger.warn(d.toString());
    }

    public static boolean update(long pageID, String pageTitle, String timesUpdated, String json, String subjects, String diff){
        try{
            Document sets = new Document("pageTitle", pageTitle)
                                .append("timesUpdated", timesUpdated)
                                .append("json", json)
                                .append("subjects", subjects)
                                .append("diff", diff);
            cache.updateOne(eq("pageID", pageID), new Document("$set", sets));
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

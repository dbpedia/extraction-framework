package org.dbpedia.extraction.live.feeder;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;
import org.dbpedia.extraction.live.main.Main;
import org.dbpedia.extraction.live.queue.LiveQueue;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;

import io.socket.IOAcknowledge;
import org.dbpedia.extraction.live.util.DateUtil;
import org.dbpedia.extraction.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.nio.cs.StreamDecoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Scanner;

/**
 * @author Lukas Faber, Stephan Haarmann, Sebastian Serth
 * date 07.05.2016.
 */
public class RCStreamFeeder extends Feeder implements IOCallback {

    private SocketIO socket;
    private String room;
    private Collection<LiveQueueItem> events;
    private Gson gson;

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public RCStreamFeeder(String feederName, LiveQueuePriority queuePriority, String defaultStartTime,
                          String folderBasePath, String room) {
        super(feederName, queuePriority, defaultStartTime, folderBasePath);
        this.room = room;
        try {
            connect();
        } catch(MalformedURLException e){

        }
        events = new ArrayList<LiveQueueItem>();
        gson = new Gson();
    }

    @Override
    protected void initFeeder() {
        // do nothing
    }

    protected void connect() throws MalformedURLException {
        socket = new SocketIO("http://stream.wikimedia.org/rc");
    }

    @Override
    protected Collection<LiveQueueItem> getNextItems() {
        synchronized (this){
            Collection<LiveQueueItem> returnValue = events;
            events = new ArrayList<LiveQueueItem>();
            return returnValue;
        }
    }

    @Override
    public void onDisconnect() {
        try {
            connect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnect() {
        socket.emit("subscribe", room);
    }

    @Override
    public void onMessage(String data, IOAcknowledge ack) {

    }

    @Override
    public void onMessage(com.google.gson.JsonElement json, IOAcknowledge ack) {

    }

    private long getIDForTitle(String title) {
        String apiURL = "https://en.wikipedia.org/w/api.php?action=query&titles="+
                title + "&prop=pageimages&format=json&pithumbsize=350";
        URL url = null;
        try {
            url = new URL(apiURL);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            String json = new Scanner(connection.getInputStream()).useDelimiter("\\A").next();
            JsonObject object = gson.fromJson(json, JsonObject.class);
            return Long.parseLong(object.getAsJsonObject("query").getAsJsonObject("pages").entrySet()
                    .iterator().next().getKey());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 35507L;
    }

    @Override
    public void on(String event, IOAcknowledge ack, com.google.gson.JsonElement... args) {
        JsonObject jsonobject = gson.fromJson(args[0], JsonObject.class);
        String title = jsonobject.get("title").getAsString();
        Long timestamp = jsonobject.get("timestamp").getAsLong();
        synchronized (this){
            long pageid = getIDForTitle(title);
            String eventTimestamp = DateUtil.transformToUTC(timestamp * 1000L);
            events.add(new LiveQueueItem(pageid, eventTimestamp));
            logger.info("Registered event for page " + pageid + " at " + eventTimestamp);
        }
    }

    @Override
    public void onError(SocketIOException socketIOException) {

    }
}

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class extends the default Feeder for RCStreem handling.
 * It registers at given socket and listens messages.
 * Those messages indicate changes in a specified mediawiki.
 *
 * @author Lukas Faber, Stephan Haarmann, Sebastian Serth
 * date 07.05.2016.
 */
public class RCStreamFeeder extends Feeder implements IOCallback {

    /** The Socket used for receiving the RCStream */
    private SocketIO socket;
    /** The room describes the wiki, which RCStream will be processed e.G. https://en.wikipedia.org */
    private String room;
    private Collection<LiveQueueItem> events;
    private Gson gson;

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public RCStreamFeeder(String feederName, LiveQueuePriority queuePriority, String defaultStartTime,
                          String folderBasePath, String room) {
        super(feederName, queuePriority, "2016-05-05T14:03:00Z", folderBasePath);
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

    /**
     * Connects to a mediawiki RCstream (e.G. http://stream.wikimedia.org/rc).
     * A MalformedURLException is raised if the URL is wrong.
     *
     * @throws MalformedURLException Connection to socket could not be established.
     */
    protected void connect() throws MalformedURLException {
        socket = new SocketIO("http://stream.wikimedia.org/rc");
        socket.connect(this);
    }

    @Override
    protected Collection<LiveQueueItem> getNextItems() {
        Collection<LiveQueueItem> returnValue;
        synchronized (this){
            returnValue = events;
            events = new ArrayList<LiveQueueItem>();
        }
        return returnValue;
    }

    @Override
    public void onDisconnect() {
//        try {
//            connect();
//        } catch (MalformedURLException e) {
//            e.printStackTrace();
//        }
    }

    @Override
    public void onConnect() {
        socket.emit("subscribe", room);
    }

    @Override
    public void onMessage(String data, IOAcknowledge ack) {
        logger.debug("Message: " + data);
    }

    @Override
    public void onMessage(com.google.gson.JsonElement json, IOAcknowledge ack) {
        logger.debug("Message: " + json.toString());
    }

    private long getIDForTitle(String title) {
        String apiURL = "https://en.wikipedia.org/w/api.php?action=query&titles="+
                title + "&prop=pageimages&format=json&pithumbsize=350";
        URL url = null;
        try {
            url = new URL(apiURL.replace(" ", "%20"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            String json = new Scanner(connection.getInputStream()).useDelimiter("\\A").next();
            Pattern pattern = Pattern.compile("\\d+");
            Matcher matcher = pattern.matcher(json);
            matcher.find();
            return Long.parseLong(matcher.group(0));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 35507L;
    }

    @Override
    public void on(String event, IOAcknowledge ack, com.google.gson.JsonElement... args) {
        logger.info("message received");
        JsonObject jsonObject = (JsonObject) args[0];
        String title = jsonObject.get("title").getAsString();
        Long timestamp = jsonObject.get("timestamp").getAsLong();
        // long pageid = getIDForTitle(title);
        String eventTimestamp = DateUtil.transformToUTC(timestamp * 1000L);
        synchronized (this){
            events.add(new LiveQueueItem(-1, title, eventTimestamp, false, ""));
            logger.info("Registered event for page " + title + " at " + eventTimestamp);
        }
    }

    /**
     * Logs exceptions, that occur while listing to the RCStream.
     *
     * @param socketIOException The exception thrown by the socket connection.
     */
    @Override
    public void onError(SocketIOException socketIOException) {
        logger.error("An error in the RCStream connection occured: " + socketIOException.getMessage());
    }
}

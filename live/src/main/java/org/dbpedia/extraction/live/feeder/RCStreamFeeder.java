package org.dbpedia.extraction.live.feeder;

import com.google.gson.JsonObject;
import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;
import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.main.Main;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.dbpedia.extraction.live.util.DateUtil;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.dbpedia.extraction.util.Language;
import org.dbpedia.extraction.wikiparser.Namespace;
import org.dbpedia.extraction.wikiparser.WikiTitle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;

/**
 * This class extends the default Feeder for RCStreem handling.
 * It registers at given socket and listens messages.
 * Those messages indicate changes in a specified mediawiki.
 *
 * @author Lukas Faber, Stephan Haarmann, Sebastian Serth
 * date 07.05.2016.
 */
public class RCStreamFeeder extends Feeder implements IOCallback {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static String WIKIMEDIA_RCSTREAM_URL = "http://stream.wikimedia.org/rc";
    /** The Socket used for receiving the RCStream */
    private SocketIO socket;
    /** The room describes the wiki, which RCStream will be processed e.G. https://en.wikipedia.org */
    private String room;
    /** The array including all allowed namespaces to be added to the LiveQueue */
    private ArrayList<Integer> allowedNamespaces = new ArrayList<>();
    private Collection<LiveQueueItem> events;

    public RCStreamFeeder(String feederName, LiveQueuePriority queuePriority, String defaultStartTime,
                          String folderBasePath, String room) {
        super(feederName, queuePriority, defaultStartTime, folderBasePath);
        // use baseURI without protocol as room to subscribe to
        this.room = room;

        // parse allowedNamespaces to Array
        for (String namespace : LiveOptions.options.get("feeder.rcstream.allowedNamespaces").split("\\s*,\\s*")) {
            allowedNamespaces.add(Integer.parseInt(namespace));
        }

        // Set Logger preferences for Socket.io
        java.util.logging.Logger sioLogger = java.util.logging.Logger.getLogger("io.socket");
        sioLogger.setLevel(Level.WARNING);

        try {
            connect();
        } catch(MalformedURLException exp){
            logger.error(ExceptionUtil.toString(exp), exp);
        }
        events = new ArrayList<LiveQueueItem>();
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
        socket = new SocketIO(WIKIMEDIA_RCSTREAM_URL);
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
        try {
            connect();
        } catch (MalformedURLException exp) {
            logger.error(ExceptionUtil.toString(exp), exp);
        }
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

    @Override
    public void on(String event, IOAcknowledge ack, com.google.gson.JsonElement... args) {
        JsonObject jsonObject = (JsonObject) args[0];
        if (jsonObject.get("type").getAsString().matches("(categorize|log)")) {
            // Don't handle log or categorize events, they don't indicate a change of wikipages
            return;
        }
        String title = jsonObject.get("title").getAsString();
        int namespaceCode = jsonObject.get("namespace").getAsInt();
        if(allowedNamespaces.contains(namespaceCode)) {
            Long timestamp = jsonObject.get("timestamp").getAsLong();
            String eventTimestamp = DateUtil.transformToUTC(timestamp * 1000L);
            synchronized (this) {
                events.add(new LiveQueueItem(-1, title, eventTimestamp, false, ""));
                logger.debug("Registered event for page " + title + " at " + eventTimestamp);
            }
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

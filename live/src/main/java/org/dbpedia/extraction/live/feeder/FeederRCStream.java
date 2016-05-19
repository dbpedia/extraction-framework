package org.dbpedia.extraction.live.feeder;


import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.dbpedia.extraction.live.storage.JDBCUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.Iterator;
import java.util.List;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import org.dbpedia.extraction.live.queue.LiveQueueItem;




/**
 * Created with IntlliJ IDEA.
 * User: Dimitris Kontokostas
 * Date: 9/6/12
 * Time: 1:07 PM
 * This is a general purpose OAI Feeder. It can be used as is for simple operations (live feeder, unmodified pages feeder)
 * or subclass and override "handleFeedItem" function
 */
public class FeederRCStream extends Feeder implements IOCallback {

    private SocketIO socket;

    protected final String oaiUri;
    protected final String oaiPrefix;
    protected final String baseWikiUri;

    protected final long pollInterval;              // in miliseconds
    protected final long sleepInterval;             // in miliseconds
    protected String serverUrl;
    protected String serverSubs;

    protected Set<LiveQueueItem> RCStreamRecordSet;

    public FeederRCStream(String feederName, LiveQueuePriority queuePriority,
                          String oaiUri, String oaiPrefix, String baseWikiUri,
                          long pollInterval, long sleepInterval, String defaultStartTime,
                          String folderBasePath, String serverUrl, String serverSubs) {
        super(feederName,queuePriority,defaultStartTime,folderBasePath);

        this.oaiUri = oaiUri;
        this.oaiPrefix = oaiPrefix;
        this.baseWikiUri = baseWikiUri;
        this.serverUrl = serverUrl;
        this.serverSubs = serverSubs;

        this.pollInterval = pollInterval;
        this.sleepInterval = sleepInterval;
    }


    @Override
    protected void initFeeder() {
        RCStreamRecordSet = JDBCUtil.getCacheUnmodified(30, 5000);
        try {
            socket = new SocketIO();
            socket.connect(this.serverUrl, this);
        } catch (Exception e) {
            e.printStackTrace();
        }



    }


    @Override
    public void onMessage(String data, IOAcknowledge ack) {}

    @Override
    public void onMessage(JsonElement jsonElement, IOAcknowledge ioAcknowledge) {

    }

    @Override
    public void onError(SocketIOException socketIOException) {}

    @Override
    public void onDisconnect() {}

    @Override
    public void onConnect() {
        socket.emit("subscribe", this.serverSubs);
    }

    @Override
    public void on(String event, IOAcknowledge ack, JsonElement... args) {
        try {
            JsonObject json = (JsonObject)(args[0]);
            if (json.get("id").getAsString() != "null") {
                RCStreamRecordSet.add(new LiveQueueItem(new Integer(json.getAsJsonObject("revision").get("new").getAsString()), json.get("timestamp").getAsString()));
            }
        }catch (Exception e2) {

        }
    }

    @Override
    protected Collection<LiveQueueItem> getNextItems() {
        Set<LiveQueueItem> ret = RCStreamRecordSet;
        RCStreamRecordSet = JDBCUtil.getCacheUnmodified(30, 5000);
        return ret;
    }


}
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

import socketio.IOAcknowledge;
import socketio.IOCallback;
import socketio.SocketIO;
import socketio.SocketIOException;

import org.json.JSONException;
import org.json.JSONObject;
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

    protected Set<LiveQueueItem> RCStreamRecordSet;

    public FeederRCStream(String feederName, LiveQueuePriority queuePriority,
                     String oaiUri, String oaiPrefix, String baseWikiUri,
                     long pollInterval, long sleepInterval, String defaultStartTime,
                     String folderBasePath) {
        super(feederName,queuePriority,defaultStartTime,folderBasePath);

        this.oaiUri = oaiUri;
        this.oaiPrefix = oaiPrefix;
        this.baseWikiUri = baseWikiUri;

        this.pollInterval = pollInterval;
        this.sleepInterval = sleepInterval;
    }


    @Override
    protected void initFeeder() {
        RCStreamRecordSet = JDBCUtil.getCacheUnmodified(30, 5000);
        try {System.out.println("hello");
            socket = new SocketIO();
            socket.connect("http://stream.wikimedia.org/rc", this);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onMessage(JSONObject json, IOAcknowledge ack) {}

    @Override
    public void onMessage(String data, IOAcknowledge ack) {}

    @Override
    public void onError(SocketIOException socketIOException) {}

    @Override
    public void onDisconnect() {}

    @Override
    public void onConnect() {
        System.out.println("Server connected: ");
        socket.emit("subscribe", "fr.wikipedia.org");
    }

    @Override
    public void on(String event, IOAcknowledge ack, Object... args) {
        //System.out.println("Server on: ");
        /*try {
            JSONObject json = (JSONObject) (args[0]);
            if(json.getJSONObject("revision") != null) {
                //System.out.println("ok :: "+(JSONObject) (args[0]));
                //System.out.println("Server said:" + json.getString("id") + "    " + json.getJSONObject("revision").getString("new"));
                RCStreamRecordSet.add(new LiveQueueItem( new Integer( json.getJSONObject("revision").getString("new") ), json.getString("timestamp")) );
            }
        } catch (Exception e) {System.out.println("ok2 :: " + (JSONObject) (args[0]));

        }*/
        try {
            JSONObject json = (JSONObject) (args[0]);
            if (json.getString("id") != "null") {
                //System.out.println("ok2 :: " + (JSONObject) (args[0]));
                //System.out.println("Server said:" + json.getString("id") + "    " + json.getJSONObject("revision").getString("new"));
                RCStreamRecordSet.add(new LiveQueueItem(new Integer(json.getString("id")), json.getString("timestamp")));
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

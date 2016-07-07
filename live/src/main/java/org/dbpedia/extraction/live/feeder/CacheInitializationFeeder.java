package org.dbpedia.extraction.live.feeder;

import com.google.gson.JsonObject;
import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.queue.LiveQueue;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.dbpedia.extraction.live.util.DateUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

/**
 * Created by Lukas on 02.07.2016.
 */
public class CacheInitializationFeeder extends Feeder {

    private boolean isFinished = false;
    private String query_base =
            "?action=query&format=json&list=allpages&aplimit=500&apnamespace=0&continue=%s&apcontinue=%s";
    private String continueString = "-||";
    private String continueTitle = "";
    public CacheInitializationFeeder(String feederName, LiveQueuePriority queuePriority, String defaultStartTime, String folderBasePath) {
        super(feederName, queuePriority, defaultStartTime, folderBasePath);

    }

    @Override
    protected void initFeeder() {

    }

    @Override
    protected Collection<LiveQueueItem> getNextItems() {
        ArrayList<LiveQueueItem> queue = new ArrayList<LiveQueueItem>();
        if(!isFinished){
            String apiURL = LiveOptions.options.get("feeder.cache.wikiapi");
            try{
                URL url = new URL(apiURL + String.format(query_base, continueString, continueTitle));
                InputStream responseStream = url.openConnection().getInputStream();
                java.util.Scanner s = new java.util.Scanner(responseStream).useDelimiter("\\A");
                JSONObject response = new JSONObject(s.next());
                JSONArray pages = response.getJSONObject("query").getJSONArray("allpages");
                for(Object pageObject : pages){
                    JSONObject page = (JSONObject) pageObject;
                    queue.add(new LiveQueueItem(-1, page.getString("title"), DateUtil.transformToUTC(new Date()), false, ""));
                }
                isFinished = !response.has("continue");
                if(!isFinished){
                    continueString = response.getJSONObject("continue").getString("continue");
                    setContinueTitle(response.getJSONObject("continue").getString("apcontinue"));
                }
                System.out.println(continueTitle);
                /*JSONObject pages = response.getJSONObject("query").getJSONObject("pages");
                System.out.println(query_continue + "----------" + pages.keySet().size());
                for(String pageid: pages.keySet()){
                    String title = (pages.getJSONObject(pageid)).getString("title");
                    queue.add(new LiveQueueItem(-1, title, DateUtil.transformToUTC(new Date()), false, ""));
                }
                isFinished = !response.has("continue");
                if(!isFinished){
                    query_continue = "";
                    JSONObject continueObject = response.getJSONObject("continue");
                    for(String key : continueObject.keySet()){
                        query_continue += "&" + key + "=" + continueObject.getString(key);
                    }
                }*/

            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
        return queue;
    }

    public void setContinueTitle(String continueTitle){
        try {
            this.continueTitle = URLEncoder.encode(continueTitle, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}

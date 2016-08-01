package org.dbpedia.extraction.live.feeder;

import com.google.gson.JsonObject;
import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.queue.LiveQueue;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.dbpedia.extraction.live.storage.JDBCUtil;
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
 * @author Lukas Faber, Stephan Haarmann, Sebastian Serth
 *         date 02.07.2016.
 */
public class AllPagesFeeder extends Feeder {

    private boolean isFinished = false;
    private String query_base =
            "?action=query&format=json&list=allpages&aplimit=500&apnamespace=%d&continue=%s&apcontinue=%s";
    private String continueString = "-||";
    private String continueTitle = "";
    private ArrayList<Integer> allowedNamespaces = new ArrayList<>();
    private int currentNamespace = 0;

    public AllPagesFeeder(String feederName, LiveQueuePriority queuePriority, String defaultStartTime,
                          String folderBasePath) {
        super(feederName, queuePriority, defaultStartTime, folderBasePath);
        for (String namespace : LiveOptions.options.get("feeder.allpages.allowedNamespaces").split("\\s*,\\s*")) {
            allowedNamespaces.add(Integer.parseInt(namespace));
        }
    }

    @Override
    protected void initFeeder() {

    }

    @Override
    protected Collection<LiveQueueItem> getNextItems() {
        ArrayList<LiveQueueItem> queue = new ArrayList<LiveQueueItem>();
        if (!isFinished) {
            System.out.println(continueTitle);
            JSONObject response = queryAllPagesAPI();
            if (response != null) {
                JSONArray pages = response.getJSONObject("query").getJSONArray("allpages");
                for (Object pageObject : pages) {
                    JSONObject page = (JSONObject) pageObject;
                    //if(!pageIsInCache(page)) {
                    //    queue.add(new LiveQueueItem(-1, page.getString("title"), DateUtil.transformToUTC(new Date()), false, ""));
                    //}

                    //always hanndle each page in case of a restart
                    queue.add(new LiveQueueItem(page.getInt("pageid"), page.getString("title"), DateUtil.transformToUTC(new Date()), false, ""));
                }
                if (response.has("continue")) {
                    continueString = response.getJSONObject("continue").getString("continue");
                    setContinueTitle(response.getJSONObject("continue").getString("apcontinue"));
                } else {
                    goToNextNamespace();
                }
            }
        }
        return queue;
    }

    private boolean pageIsInCache(JSONObject page) {
        long pageID = page.getLong("pageid");
        String query = "SELECT * FROM dbpedialive_cache WHERE pageID = ?";
        boolean test = JDBCUtil.getCacheContent(query, pageID) != null;
        if (test) {
            System.out.println("already in cache");
        }
        return test;
    }

    private JSONObject queryAllPagesAPI() {
        String apiURL = LiveOptions.options.get("feeder.allpages.wikiapi");
        URL url = null;
        try {
            url = new URL(apiURL + String.format(query_base, allowedNamespaces.get(currentNamespace), continueString, continueTitle));
            InputStream responseStream = url.openConnection().getInputStream();
            java.util.Scanner s = new java.util.Scanner(responseStream).useDelimiter("\\A");
            return new JSONObject(s.next());
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        goToNextNamespace();
        return null;
    }

    private void goToNextNamespace() {
        currentNamespace++;
        if (currentNamespace == allowedNamespaces.size()) {
            isFinished = true;
            currentNamespace = 0;
        }
        continueString = "";
        continueTitle = "";
    }

    public void setContinueTitle(String continueTitle) {
        try {
            this.continueTitle = URLEncoder.encode(continueTitle, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }
}

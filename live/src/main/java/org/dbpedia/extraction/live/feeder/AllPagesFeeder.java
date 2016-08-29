package org.dbpedia.extraction.live.feeder;

import org.dbpedia.extraction.live.core.LiveOptions;
import org.dbpedia.extraction.live.queue.LiveQueueItem;
import org.dbpedia.extraction.live.queue.LiveQueuePriority;
import org.dbpedia.extraction.live.util.DateUtil;
import org.dbpedia.extraction.live.util.ExceptionUtil;
import org.dbpedia.extraction.util.Language;
import org.dbpedia.extraction.util.WikiApi;
import scala.Tuple3;
import scala.collection.immutable.Seq;
import scala.xml.Node;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author Lukas Faber, Stephan Haarmann, Sebastian Serth
 *         date 02.07.2016.
 */
public class AllPagesFeeder extends Feeder {

    private final Language language = Language.apply(LiveOptions.options.get("language"));
    private WikiApi api;
    private boolean isFinished = false;
    private String continueString = "-||";
    private String continueTitle = "";
    private ArrayList<Integer> allowedNamespaces = new ArrayList<>();
    private int currentNamespace = 0;

    public AllPagesFeeder(String feederName, LiveQueuePriority queuePriority, String defaultStartTime,
                          String folderBasePath) {
        super(feederName, queuePriority, defaultStartTime, folderBasePath);
        try {
            api = new WikiApi(new URL(language.apiUri()), language);
        } catch (MalformedURLException exp) {
            logger.error(ExceptionUtil.toString(exp), exp);
        }

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
            List<Node> pageList = queryAllPagesAPI();
            for (Node page : pageList) {
                //queue.add(new LiveQueueItem(Long.parseLong(page.$bslash$at("pageid")), page.$bslash$at("title"), DateUtil.transformToUTC(new Date()), false, ""));
            }
            if (continueTitle.isEmpty()) {
                goToNextNamespace();
            }
        }
        return queue;
    }

    private List<Node> queryAllPagesAPI() {
        Tuple3<String, String, Seq<Node>> result = api.retrieveAllPagesPerNamespace(allowedNamespaces.get(currentNamespace), continueString, continueTitle);
        setContinueTitle(result._1());
        setContinueString(result._2());
        return scala.collection.JavaConversions.seqAsJavaList(result._3());
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

    private void setContinueTitle(String continueTitle) {
        try {
            this.continueTitle = URLEncoder.encode(continueTitle, "UTF-8");
        } catch (UnsupportedEncodingException exp) {
            logger.error(ExceptionUtil.toString(exp), exp);
        }
    }

    private void setContinueString(String continueString) {
        this.continueString = continueString;
    }
}

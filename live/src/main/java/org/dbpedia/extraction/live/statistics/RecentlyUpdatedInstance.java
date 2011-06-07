package org.dbpedia.extraction.live.statistics;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Nov 15, 2010
 * Time: 11:23:10 AM
 * In the web page that contains statistics about DBpedia-Live, we should display a list of the 20 most recently
 * processed wikipedia pages 
 */
public class RecentlyUpdatedInstance implements Comparable<RecentlyUpdatedInstance>{
    private String _instanceName = "";
    private String _dbpediaPage = "";
    private String _wikipediaPage = "";
    private Date _updateTime = new Date();

    public Date getUpdatedSince() {
        return _updateTime;
    }



    /**
     * Constructor for the class
     * @param instanceName  The name of updated instance
     * @param dbpediaPage The title  of updated instance
     * @param wikipediaPage The link to the wikipediaPage
     * @param updateTime  How much time ago was the page processed
     */
    public RecentlyUpdatedInstance(String instanceName, String dbpediaPage, String wikipediaPage, Date updateTime){
        _instanceName = instanceName;
        _dbpediaPage = dbpediaPage;
        _wikipediaPage = wikipediaPage;
        _updateTime = updateTime;
    }

    /**
     * Constructor for the class
     * @param instanceName  The name of updated instance
     * @param dbpediaPage The title  of updated instance
     * @param wikipediaPage The link to the wikipediaPage
     */
    public RecentlyUpdatedInstance(String instanceName, String dbpediaPage, String wikipediaPage){
        this(instanceName, dbpediaPage, wikipediaPage, new Date());
    }

    /**
     * Constructor for the class
     * */
    public RecentlyUpdatedInstance(){
        this("", "", "", new Date());
    }

    public int compareTo(RecentlyUpdatedInstance otherInstance){
        //We should sort the instances descendingly
        return otherInstance.getUpdatedSince().compareTo(this._updateTime);
//            return -1;
//        else if(this._updateTime > otherInstance.getUpdatedSince())
//            return 1;
//        else
//            return 0;
    }

    public String toString(){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ");
        String strFormatted = _instanceName + "\t" + _dbpediaPage + "\t" + _wikipediaPage + "\t" +
                formatter.format(_updateTime);
        return strFormatted;
    }
}

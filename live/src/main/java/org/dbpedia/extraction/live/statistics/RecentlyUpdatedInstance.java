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
    private boolean _hasDelta = false;

    public Date getUpdatedSince() {
        return _updateTime;
    }

    public String getDBpediaPage() {
        return _dbpediaPage;
    }

    public boolean getHasDelta() {
        return _hasDelta;
    }

    public void setHasDelta(boolean  hasDelta) {
        _hasDelta = hasDelta;
    }


    /**
     * Constructor for the class
     * @param instanceName  The name of updated instance
     * @param dbpediaPage The link to the DBpedia page of the instance
     * @param wikipediaPage The link to the wikipediaPage
     * @param updateTime  How much time ago was the page processed
     */
    public RecentlyUpdatedInstance(String instanceName, String dbpediaPage, String wikipediaPage, Date updateTime){
        /*_instanceName = instanceName;
        _dbpediaPage = dbpediaPage;
        _wikipediaPage = wikipediaPage;
        _updateTime = updateTime;
        _hasDelta = false;*/
        this(instanceName, dbpediaPage, wikipediaPage, updateTime, false);
    }

    /**
     * Constructor for the class
     * @param instanceName  The name of updated instance
     * @param dbpediaPage The link to the DBpedia page of the instance
     * @param wikipediaPage The link to the wikipediaPage
     * @param updateTime  How much time ago was the page processed
     * @param hasDelta  Whether there is a delta(added, deleted, modified triples) for the passed instance
     */
    public RecentlyUpdatedInstance(String instanceName, String dbpediaPage, String wikipediaPage, Date updateTime,
                                   boolean hasDelta){
        _instanceName = instanceName;
        _dbpediaPage = dbpediaPage;
        _wikipediaPage = wikipediaPage;
        _updateTime = updateTime;
        _hasDelta = hasDelta;
    }

    /**
     * Constructor for the class
     * @param instanceName  The name of updated instance
     * @param dbpediaPage The link to the DBpedia page of the instance
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

    /**
     * Constructor for the class
     * @param dbpediaPage   The link to the DBpedia page of the instance
     * */
    public RecentlyUpdatedInstance(String dbpediaPage){
        this("", dbpediaPage, "", new Date(), false);
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
                formatter.format(_updateTime) + "\t" + _hasDelta;
        return strFormatted;
    }


    //This function is used mainly to determine whether a specific instance has value for delta stored in the database or not.

    /**
     *
     * @param objRecentlyUpdatedInst    The object with which we should compare the current object
     * @return  True if they are equal, false otherwise.
     */
    @Override
    public boolean equals(Object objRecentlyUpdatedInst){
        try{
            RecentlyUpdatedInstance passedInstance = (RecentlyUpdatedInstance)objRecentlyUpdatedInst;

            //First we should get the the resource name part without the rest of URI string
            //e.g. http://dbpedia.org/page/Linux => Linux only in order to compare it with same part of the passed instance
            int lastSlashPosOfThePassedInstance = passedInstance.getDBpediaPage().lastIndexOf('/');

            if(lastSlashPosOfThePassedInstance < 0)
                return false;
            String resourceNamePartOfThePassedInstance = passedInstance.getDBpediaPage().substring(lastSlashPosOfThePassedInstance+1);

            int lastSlashPosOfTheCurrentInstance = this._dbpediaPage.lastIndexOf('/');

            if(lastSlashPosOfTheCurrentInstance < 0)
                return false;
            String resourceNamePartOfTheCurrentInstance = this._dbpediaPage.substring(lastSlashPosOfThePassedInstance+1);

            return resourceNamePartOfTheCurrentInstance.equals(resourceNamePartOfThePassedInstance);


        }
        catch(Exception exp){
            return false;
        }

    }

}

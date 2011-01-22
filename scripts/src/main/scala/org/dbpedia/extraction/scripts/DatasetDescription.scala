package org.dbpedia.extraction.scripts

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: May 9, 2010
 * Time: 6:34:12 AM
 * This class holds the information describing a dataset that is stated in the configuration file
 */


class DatasetDescription
{
  
  var DatasetType = "";
  var DatasetTitle: String = "";
  var DatasetName: String = ""; //This is the name used in the voiD file 
  var DatasetDescription: String = "";
  var RootFileName : String = "";
  var DatasetFilesList: List[DatasetFileDescription] = List();

  override def toString = "Dataset type = "+ DatasetType + ", \ndataset title = " + DatasetTitle +
                          "\nRelated to file = " + RootFileName + ", \ndescription = " + DatasetDescription +
                          "\nDatasetfiles = "+ DatasetFilesList.size+"\n"; 
}
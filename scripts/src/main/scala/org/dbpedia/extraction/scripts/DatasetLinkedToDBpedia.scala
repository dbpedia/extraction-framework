package org.dbpedia.extraction.scripts

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: May 29, 2010
 * Time: 6:52:51 PM
 * This class represents the information required to describe a dataset to which DBpedia is related e.g. USCensus
 */


class DatasetLinkedToDBpedia()
{
  var DatasetName : String = "";
  var DatasetTitle : String = "";
  var Homepage : String = "";

  def this(datsetName : String, datasetTitle : String, homepage :String)=
    {
      this();
      DatasetName = datsetName;
      DatasetTitle = datasetTitle
      Homepage = homepage;
    }

  override def toString = "Dataset name = " + DatasetName + ",\nDataset title = " + DatasetTitle +
                          ",\nHompage = " + Homepage + "\n";
}
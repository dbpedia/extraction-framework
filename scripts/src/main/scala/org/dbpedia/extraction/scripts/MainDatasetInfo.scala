package org.dbpedia.extraction.scripts

import java.util.Date

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: May 30, 2010
 * Time: 5:45:28 PM
 * This class holds the information required to describe the main dataset of the voiD file which is DBpedia
 * this information include for example description, and list of contributors
 */

class MainDatasetInfo
{
  var DatasetName : String = "";
  var Title : String = "";
  var Description : String = "";
  var Homepage : String = "";
  var ModifiedDate : String = "";
  var SparqlEndPoint : String = "";
  var URIRegexPattern : String = "";
  var SourceList : List[String] = List();
  var ContributorList : List[String] = List();
  var ExampleResourceList : List[String] = List();


}
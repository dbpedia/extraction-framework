package org.dbpedia.extraction.scripts

import scala.collection.immutable._
import com.sun.org.apache.xpath.internal.operations.Bool;
import java.io._;
import scala.xml._;


/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Apr 23, 2010
 * Time: 1:07:44 PM
 * This object creates the Sitemap of DBpedia datasets.
 */

object SitemapGenerator
{
  //The names of the tags used in the XML configuration file
  val baseURLTagName = "baseURL";
  val baseDirectoryTagName = "baseDirectory";
  val datasetLabelTagName = "datasetLabel";
  val changeFreqTagName = "changefreq";
  val linkedDataPrefixTagName = "linkedDataPrefix";
  val sparqlEndpointLocationTagName = "sparqlEndpointLocation";
  val siteMapFileNameTagName = "siteMapFileName";
  val sparqlGraphNameTagName = "sparqlGraphName";
  val slicingAttributeName = "slicing";
  val datasetURITagName = "datasetURI";
  val lastmodTagName = "lastmod";
  val datasetTagName = "dataset";

  val includeLoadedDatasetsOnlyTagName = "includeLoadedDatasetsOnly"; //This tag exists only if the target is to include
                                                              //only the datasets that are loaded to the SPARQL endpoint
  val includeLoadedDatasetsOnlyAttributeName = "include"; //This attribute appears in includeLoadedDatasetsOnly tag to
                                      //indicate whether to include the listed datasets only or not

  //The vlaues of the of each item extracted from the XML configuration file
  var baseURL = "" ;
  var baseDirectory = "";
  var datasetLabel = "";
  var changeFreq = "";
  var linkedDataPrefix = "";
  var sparqlEndpointLocation = "";
  var siteMapFileName = "";
  var sparqlGraphName = "";
  var datasetURI = "";
  var lastmod = "";
  var slicing = "";
  var listFiles : List[String]=List();//List of files to be written to the Sitemap
  //if the option is set to include only loaded datasets, then this list will be populated in function readConfigurationData
  //otherwise it will be populated from the local directory

  //var includeLoadedDatasetsOnly = scala.xml.NodeSeq ;
  var includeLoadedDatasetsOnly = false;

  def main(args: Array[String])
    {
      require(args != null && args.length == 1, "xml config file name must be given, for example sitemap.config")
      val file = new File(args(0))
      
      val xmlHandlerObj = new XMLHandler();

      //Read configuration data to get all required parameters
      readConfigurationData(file);

      //The includeLoadedDatasetsOnly is false, then populate the listFiles from the local directory 
      if(!includeLoadedDatasetsOnly)
        {
          //Iterate through the directories to get the files within each directory
          val f = new File(baseDirectory);
          for(currentDir <- f.listFiles if currentDir.isDirectory)
              listFiles = listFiles ::: listDirectoryFiles(currentDir);
        }
      
      //Append all files acquired from the directories to the XML tree
      xmlHandlerObj.appendDirectoryFiles(listFiles);

      //Append the other nodes e.g. changeFreq to the XML tree
      //Manadatory nodes
      xmlHandlerObj.appendNode(datasetLabelTagName,datasetLabel, null, "sc");
      xmlHandlerObj.appendNode(linkedDataPrefixTagName,linkedDataPrefix, Map(slicingAttributeName -> slicing), "sc");
      xmlHandlerObj.appendNode(sparqlEndpointLocationTagName,sparqlEndpointLocation, null, "sc");

      //Optional nodes 
      if(changeFreq != "")
        xmlHandlerObj.appendNode(changeFreqTagName,changeFreq, null, null);

      if(sparqlGraphName != "")
        xmlHandlerObj.appendNode(sparqlGraphNameTagName,sparqlGraphName, null, "sc");

      if(datasetURI != "")
        xmlHandlerObj.appendNode(datasetURITagName,datasetURI, null, "sc");

      if(datasetLabel != "")
        xmlHandlerObj.appendNode(datasetLabelTagName,datasetLabel, null, "sc");

      if(lastmod != "")
        xmlHandlerObj.appendNode(lastmodTagName,lastmod, null, null);



      //Save the XML tree representing the SiteMap to a file
      xmlHandlerObj.saveFile(siteMapFileName);

    }

  //Returns a list of all files that reside in the passed folder
  def listDirectoryFiles(requiredDirectory : File): List[String]=
    {
      try
      {
        //Now the last item in the splitted array will contain the language folder
        val strLanguageDirectory = requiredDirectory.getName;


        var listDirectoryFiles:List[String]=List();
        for(file <- requiredDirectory.listFiles)
          {
            //Now the last item in the splitted array will contain the filename
            listDirectoryFiles = listDirectoryFiles ::: List(baseURL + "/" + strLanguageDirectory
                    + "/" + file.getName);
          }
          listDirectoryFiles;
      }
      catch
      {
        case exp => println("Unable to list directory files");
        return List();
      }
    }

  //Reads the information from the configuration file
  def readConfigurationData(f : File):Unit=
    {
      try
      {
        //Read the XML file
        val xmlConfigTree = XML.loadFile(f);
        
        //Extract the value of each tag and then assign this value to the appropriate variable, corresponding to each part
        var reqNode = xmlConfigTree \\ baseURLTagName;
        baseURL = reqNode.text;

        reqNode = xmlConfigTree \\ baseDirectoryTagName;
        baseDirectory = reqNode.text;

        reqNode = xmlConfigTree \\ linkedDataPrefixTagName;
        linkedDataPrefix = reqNode.text;

        //Read the sclicing attribute associated with linkedDataPrefix tag
        val strAttributeValueExtraction :String = "@"+slicingAttributeName;
        slicing = (reqNode \ strAttributeValueExtraction).text;

        reqNode = xmlConfigTree \\ sparqlEndpointLocationTagName;
        sparqlEndpointLocation = reqNode.text;

        reqNode = xmlConfigTree \\ siteMapFileNameTagName;
        siteMapFileName = reqNode.text;

        //Read the optional tags
        reqNode = xmlConfigTree \\ sparqlGraphNameTagName;
        sparqlGraphName = reqNode.text;

        reqNode = xmlConfigTree \\ datasetURITagName;
        datasetURI = reqNode.text;

        reqNode = xmlConfigTree \\ datasetLabelTagName;
        datasetLabel= reqNode.text;

        reqNode = xmlConfigTree \\ lastmodTagName;
        lastmod = reqNode.text;

        reqNode = xmlConfigTree \\ changeFreqTagName;
        changeFreq = reqNode.text;

        //Read the tag determining whether to include datasets on the SPARQL endpoint only or not
        includeLoadedDatasetsOnly = false;
        var includeLoadedDatasetsOnlyNode = xmlConfigTree \\ includeLoadedDatasetsOnlyTagName;
        if(includeLoadedDatasetsOnlyNode.size>0)//Node exists
          {
            //Read the include attribute associated with includeLoadedDatasetsOnly tag
            val strAttributeIncludeLoadedDatasetsOnly :String = "@"+includeLoadedDatasetsOnlyAttributeName;
            val strIncludeLoadedDatasetsValue = (includeLoadedDatasetsOnlyNode \ strAttributeIncludeLoadedDatasetsOnly).text;

            includeLoadedDatasetsOnly =  strIncludeLoadedDatasetsValue.toBoolean;

            //If the option is true then fill listFiles from the config file
            listFiles = List();
            if(includeLoadedDatasetsOnly)
              {
                val datasetSeq = includeLoadedDatasetsOnlyNode \\ datasetTagName;
                datasetSeq.foreach ( (datasetNode) =>
                  {
                    var nodeText = datasetNode.text;
                    nodeText = removeBlankSpacesFromString(nodeText);


                    //Since the dataset is not stored directly on baseURL, but in a subfolder with language name
                    //we must extract the language name (subfolder)
                    if(nodeText.indexOf("owl")<0)
                      {
                        val UnderscorePosition = nodeText.lastIndexOf("_");
                        val DotPosition = nodeText.indexOf(".", UnderscorePosition);

                        val FolderName = nodeText.substring(UnderscorePosition+1, DotPosition);

                        listFiles =listFiles ::: List(baseURL + "/" + FolderName + "/" + nodeText);
                      }
                    else//The OWL dataset does not contain a subfolder, so we must use its name along with base URl directly 
                      listFiles =listFiles ::: List(baseURL + "/" +  nodeText);
                      
                  }
                  );
              }
          }
        println("Configuartion information read successfully");
      }
      catch
      {
        case exp => println("Unable to read the configuration file");
      }
    }

  //This function is important because when we we get the node text of a dataset from the config file, it returns the actual
  //node text along with blank space, so we must remove those blank space, in order to get the real dataset name   
  def removeBlankSpacesFromString(inputString:String):String=
    {
      var processString:String = inputString;
      processString = processString.replace(" ","");
      processString = processString.replace("\n","");
      processString = processString.replace("\t","");
      processString;
    }
}
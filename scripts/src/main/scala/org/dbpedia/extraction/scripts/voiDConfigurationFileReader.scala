package org.dbpedia.extraction.scripts

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: May 5, 2010
 * Time: 11:51:43 AM
 * This class reads the configuration information required for building the voiD file.
 */

import scala.xml._;
import javax.xml._;
import scala.tools.nsc.io._;
import scala.collection.mutable._;
import java.util.Date;



class voiDConfigurationFileReader
{
  private val configFileName = "void.config";

  //The names of the tags used in the XML configuration file
  private val namespacesTagName = "namespaces";
  private val namespaceTagName = "namespace";
  private val namespacePrefixAttributeName = "prefix";
  private val voiDFileNameTagName = "voiDFileName";
  private val uriBaseTagName = "uriBase";
  private val datasetsTagName = "datasets";
  private val datasetTagName = "dataset";
  private val datasetTypeAttributeName = "type";
  private val datasetTitleTagName = "title";
  private val datasetDescriptionTagName = "description";
  private val dataDumpTagName = "dataDump";
  private val datasetNameTagName = "name";
  private val numberOftriplesTagName = "numberOfTriples";
  private val fileSizeTagName = "fileSize";
  private val downloadFileSizeTagName = "downloadFileSize";
  private val loadedDatasetsTagName = "loadedDatasets";
  private val mainDatasetNameTagName = "mainDatasetName";
  private val uriDumpBaseTagName = "uriDumpBase";
  private val titleLanguageTagName = "titleLanguage";
  private val linkedToDBpediaDatasetsTagName = "linkedToDBpediaDatasets";
  private val homepageTagName = "homepage";
  private val mainDatasetTagName = "mainDataset";
  private val modifiedTagName = "modified";
  private val uriRegexPatternTagName = "uriRegexPattern";
  private val sourcesTagName = "sources";
  private val sourceTagName = "source";
  private val contributorTagName = "contributor";
  private val contributorsTagName = "contributors";
  private val exampleResourcesTagName = "exampleResources";
  private val exampleResourceTagName = "exampleResource";
  private val dbpediaSparqlDatasetTagName = "dbpediaSparqlDataset"; 
  

  private val coreDatasetsDescriptionFileNameTagName = "coreDatasetsDescriptionFileName";
  private val extendedDatasetsDescriptionFileNameTagName = "extendedDatasetsDescriptionFileName";
  private val downloadPageCreatorFileNameTagName = "downloadPageCreatorFileName";
  private val coreDatasetsArrayNameTagName = "coreDatasetsArrayName";
  private val extendedDatasetsArrayNameTagName = "extendedDatasetsArrayName";
  private val sparqlEndpointTagName = "sparqlEndpoint";
  private val subjectDatasetTagName = "subjectDataset";
  private val objectDatasetTagName = "objectDataset";
  private val linkPredicateTagName = "linkPredicate";
  
  val prefixNameSpaceList = Map[String, String]();
  var voiDFileName :String = "";
  var coreDatasetsDescriptionFileName :String = "";
  var extendedDatasetsDescriptionFileName :String = "";
  var uriBase :String = "";
  var uriDumpBase : String = "";
  var downloadPageCreatorFileName :String = "";
  var coreDatasetsArrayName :String = "";
  var extendedDatasetsArrayName :String = "";
  var sparqlEndpoint :String = "";
  var mainDatasetName :String = "";
  var titleLanguage :String = "";
  private var LoadedDatasetsList : List[String] = List();
  
  var DatasetDescriptionList:List[DatasetDescription]=List();
  //var DatasetDescriptionList:List[DatasetDescription]=List();

  //Reads the information from the configuration file
  def readConfigurationData():Unit=
    {
      try
      {
        //Get the current directory in which the application resides because it also contains the configuration file
        val strDirectoryFullPath = Directory.Current.toList(0);

        val configFileFullPath = strDirectoryFullPath + "\\" + this.configFileName;

        //Read the XML file
        val xmlConfigTree = XML.loadFile(configFileFullPath);
        
        //Read the output filename
        var currentNode = xmlConfigTree \\ voiDFileNameTagName;
        voiDFileName = currentNode.text;

        //Extract the value of each tag and then assign this value to the appropriate variable, corresponding to each part
        currentNode = xmlConfigTree \\ namespacesTagName;
        val namespacesListNode = currentNode \\ namespaceTagName;

        if(namespacesListNode.size>0) //There are namespaces in the config file
          {
            //Extract each namespace along with its prefix, and place them in the Map
            namespacesListNode.foreach(namespaceNode =>
                    {
                      val nodeNamespace = namespaceNode.text;
                      val nodeNamespacePrefix = namespaceNode \ ("@"+namespacePrefixAttributeName);
                      prefixNameSpaceList += (nodeNamespacePrefix.text -> nodeNamespace);
                    }
              )
          }

        //Read the file names of CoreDatasets and ExtendedDatasets
        currentNode = xmlConfigTree \\ coreDatasetsDescriptionFileNameTagName;
        coreDatasetsDescriptionFileName = currentNode.text.trim;

        currentNode = xmlConfigTree \\ extendedDatasetsDescriptionFileNameTagName;
        extendedDatasetsDescriptionFileName = currentNode.text.trim;
        //extendedDatasetsDescriptionFileName = currentNode.text;

        currentNode = xmlConfigTree \\ uriBaseTagName;
        uriBase = currentNode.text.trim;

        currentNode = xmlConfigTree \\ uriDumpBaseTagName;
        uriDumpBase = currentNode.text.trim;

        currentNode = xmlConfigTree \\ downloadPageCreatorFileNameTagName;
        downloadPageCreatorFileName = currentNode.text.trim;

        currentNode = xmlConfigTree \\ coreDatasetsArrayNameTagName;
        coreDatasetsArrayName = currentNode.text.trim;

        currentNode = xmlConfigTree \\ extendedDatasetsArrayNameTagName;
        extendedDatasetsArrayName = currentNode.text.trim;

        currentNode = xmlConfigTree \\ sparqlEndpointTagName;
        sparqlEndpoint = currentNode.text.trim;

        //currentNode = xmlConfigTree \\ mainDatasetNameTagName;
        //mainDatasetName = currentNode.text.trim;

        currentNode = xmlConfigTree \\ titleLanguageTagName;
        titleLanguage = currentNode.text.trim; 
        
        currentNode = xmlConfigTree \\ datasetsTagName;

        //Reading the datasets that are loaded into the sparql endpoint
        currentNode = xmlConfigTree \\ loadedDatasetsTagName;
        currentNode = currentNode \\ datasetTagName;

        currentNode.foreach(datasetNode => LoadedDatasetsList = LoadedDatasetsList ::: List(datasetNode.text.trim));
        //println(LoadedDatasetsList);

        println("voiD configuration file read successfully");

      }
      catch
      {
        case exp => println("Unable to read the voiD configuration file");
      }
    }

  //This function takes the xml tree and returns a list of file descriptions read from the configuration file
  //@param XMLTree  The XML tree contained in the configuration file
  //@return A list of dataset description, describing each dataset
  def readDataseDescriptiontList(XMLTree: NodeSeq):Unit=
    {
      /*try
      {

        DatasetDescriptionList = List();
        var CurrentDatasetDescription:DatasetDescription=new DatasetDescription();
        var CurrentNode = XMLTree \\ datasetTagName;
        CurrentNode.foreach(XMLNode =>
          {
            CurrentDatasetDescription = new DatasetDescription();
            CurrentDatasetDescription.DatasetType = (XMLNode \ ("@"+datasetTypeAttributeName)).text.trim;
            CurrentDatasetDescription.DatasetTitle = (XMLNode \\ datasetTitleTagName).text.trim;
            CurrentDatasetDescription.DatasetDescription = (XMLNode \\ datasetDescriptionTagName).text.trim;
            CurrentDatasetDescription.RootFileName = (XMLNode \\ dumpFileNameTagName).text.trim;

            //[TODO] add the rest of the extraction code here

            
            DatasetDescriptionList = DatasetDescriptionList ::: List(CurrentDatasetDescription);

          }
          );
      }
      catch
      {
        case exp => println("Unable to read the dataset description from the configuration file");
      }*/
    }

  def constructDatasetFilesList(RequiredDataset : DatasetDescription):Unit =
    {
       RequiredDataset.DatasetDescription = "Dataset description";
    }

  def readOwlDataset():DatasetDescription =
    {
        val OWLDataset = new DatasetDescription();
       //Get the current directory in which the application resides because it also contains the configuration file
        val strDirectoryFullPath = Directory.Current.toList(0);

        val configFileFullPath = strDirectoryFullPath + "\\" + this.configFileName;

        //Read the XML file
        val xmlConfigTree = XML.loadFile(configFileFullPath);

        var currentNode = xmlConfigTree \\ datasetsTagName;
        currentNode = currentNode \\datasetTagName;
        OWLDataset.DatasetName = (currentNode \\ datasetNameTagName).text.trim;
        OWLDataset.DatasetDescription = (currentNode \\ datasetDescriptionTagName ).text.trim;
        OWLDataset.DatasetTitle = (currentNode \\ datasetTitleTagName ).text.trim;
        OWLDataset.DatasetType = (currentNode \ ("@"+datasetTypeAttributeName)).text.trim;
        val OWLDatasetFileDesc = new DatasetFileDescription();
        OWLDatasetFileDesc.DumpFileLocation = (currentNode \\ dataDumpTagName).text.trim;
        OWLDatasetFileDesc.NumberOfTriples = (currentNode \\ numberOftriplesTagName).text.trim;
        OWLDatasetFileDesc.FileSize = (currentNode \\ fileSizeTagName).text.trim;
        OWLDatasetFileDesc.DownloadFileSize = (currentNode \\ downloadFileSizeTagName).text.trim;
        OWLDataset.DatasetFilesList = List(OWLDatasetFileDesc);
        //println(OWLDataset);
        OWLDataset;
    }

  //This function checks to see whether the passed dataset is loaded into the sparql endpoint or not
  //@param DatasetName  The name of the dataset to be checked
  //@return A boolean value indicating whether the dataset is found in the list of loaded datasets or not 
  def isDatasetLoadedIntoSparqlEndPoint(DatasetName : String) : Boolean =
    {
      LoadedDatasetsList.foreach(ds =>
        {
          if(ds.equalsIgnoreCase(DatasetName))
            return true;
        });

      false;
    }

    //This function checks returns the sparql endpoint to which the passed dataset is loaded
    //@param DatasetName  The name of the dataset to be checked
    //@return The sparql endpoint of the dataset if it is loaded, and null otherwise 
    def getDatasetSparqlEndPoint(DatasetName : String) : String = 
      {

        //Get the current directory in which the application resides because it also contains the configuration file
        val strDirectoryFullPath = Directory.Current.toList(0);

        val configFileFullPath = strDirectoryFullPath + "\\" + this.configFileName;
        
        val xmlConfigTree = XML.loadFile(configFileFullPath);

        //Reading the datasets that are loaded into the sparql endpoint
        var currentNode = xmlConfigTree \\ loadedDatasetsTagName;
        currentNode = currentNode \\ datasetTagName;
        currentNode.foreach( node =>
          {
              val dsName = (node \\ datasetNameTagName).text.trim;
              val dsSparqlEndPoint = node \\ sparqlEndpointTagName;
              if(dsName.equalsIgnoreCase(DatasetName))
                return dsSparqlEndPoint.text.trim;
          }
          );
        null;

      }

    //This function checks returns the subject of the passed links dataset
    //@param DatasetName  The name of the dataset, to which the subject belongs
    //@return A list of containing the subject and the object of the passed links dataset if they are found ,
    // and null otherwise
    def getLinksDatasetFullInfo(DatasetName : String) : LinkSetInfo =
      {

        //Get the current directory in which the application resides because it also contains the configuration file
        val strDirectoryFullPath = Directory.Current.toList(0);

        val configFileFullPath = strDirectoryFullPath + "\\" + this.configFileName;

        val xmlConfigTree = XML.loadFile(configFileFullPath);

        //Reading the datasets that are loaded into the sparql endpoint
        var currentNode = xmlConfigTree \\ loadedDatasetsTagName;
        currentNode = currentNode \\ datasetTagName;
        currentNode.foreach( node =>
          {
              val dsName = (node \\ datasetNameTagName).text.trim;
              val dsSubject = (node \\ subjectDatasetTagName).text.trim;
              val dsObject = (node \\ objectDatasetTagName).text.trim;
              val dsLinkPredicate = (node \\ linkPredicateTagName).text.trim;
              if(dsName.equalsIgnoreCase(DatasetName))
                {
                  val linksetInfo = new LinkSetInfo(dsName, dsSubject, dsObject, dsLinkPredicate);
                  return linksetInfo;
                }
          }
          );
        null;

      }

    //This function returns a list of datasets to which DBpedia is linked e.g. USCensus
    //@param DatasetName  The name of the dataset, to which the subject belongs
    //@return A list of containing the subject and the object of the passed links dataset if they are found ,
    // and null otherwise
    def getDatasetsLinkedToDBpedia() : List[DatasetLinkedToDBpedia] =
      {

        //Get the current directory in which the application resides because it also contains the configuration file
        val strDirectoryFullPath = Directory.Current.toList(0);

        val configFileFullPath = strDirectoryFullPath + "\\" + this.configFileName;

        val xmlConfigTree = XML.loadFile(configFileFullPath);

        //Reading the datasets that are linked to DBpedia
        var currentNode = xmlConfigTree \\ linkedToDBpediaDatasetsTagName;
        currentNode = currentNode \\ datasetTagName;

        var linkedToDBpediaDatasetList : List[DatasetLinkedToDBpedia] = List();

        currentNode.foreach( node =>
          {
              val datasetName = (node \\ datasetNameTagName).text.trim;
              val datasetTitle = (node \\ datasetTitleTagName).text.trim;
              val homepage = (node \\ homepageTagName).text.trim;
              linkedToDBpediaDatasetList = linkedToDBpediaDatasetList :::
                      List(new DatasetLinkedToDBpedia(datasetName,datasetTitle, homepage));
          }
          );
        linkedToDBpediaDatasetList;

      }

  //This function reads the information describing the main dataset from the void.config file and returns it
  //@returns  An object containing the full information about the main dataset 
  def getMainDatasetInfo():MainDatasetInfo =
    {
       //Get the current directory in which the application resides because it also contains the configuration file
        val strDirectoryFullPath = Directory.Current.toList(0);

        val configFileFullPath = strDirectoryFullPath + "\\" + this.configFileName;

        val xmlConfigTree = XML.loadFile(configFileFullPath);

        //Reading the datasets that are loaded into the sparql endpoint
        val currentNode = xmlConfigTree \\ mainDatasetTagName;


        val mainDatasetInfo = new MainDatasetInfo();

      mainDatasetInfo.DatasetName = (currentNode \\ datasetNameTagName).text.trim;
      mainDatasetInfo.Title = (currentNode \\ datasetTitleTagName).text.trim;
      mainDatasetInfo.Description = (currentNode \\ datasetDescriptionTagName).text.trim;
      mainDatasetInfo.Homepage = (currentNode \\ homepageTagName).text.trim;
      mainDatasetInfo.SparqlEndPoint = (currentNode \\ sparqlEndpointTagName).text.trim;
      mainDatasetInfo.ModifiedDate =(currentNode \\ modifiedTagName).text.trim;
      mainDatasetInfo.URIRegexPattern = (currentNode \\ uriRegexPatternTagName).text.trim;

      val SourcesNode = currentNode \\ sourcesTagName;
      val SourceNodesList = SourcesNode \\ sourceTagName;

      SourceNodesList.foreach(node =>
      {
        mainDatasetInfo.SourceList = mainDatasetInfo.SourceList ::: List(node.text.trim);

      });

      val ContributorsNode = currentNode \\ contributorsTagName;
      val ContributorNodesList = ContributorsNode \\ contributorTagName;

      ContributorNodesList.foreach( node =>
      {
        mainDatasetInfo.ContributorList = mainDatasetInfo.ContributorList ::: List(node.text.trim);

      });

      val ExampleResourcesNode = currentNode \\ exampleResourcesTagName;
      val ExampleResourceNodesList = ExampleResourcesNode \\ exampleResourceTagName;

      ExampleResourceNodesList.foreach( node =>
      {
        mainDatasetInfo.ExampleResourceList = mainDatasetInfo.ExampleResourceList ::: List(node.text.trim);

      });

      mainDatasetInfo;

    }

    //It is proposed to build the main DBPedia dataset and make another dataset called DBpediaSPARQL
    //as a subset of it and write the sparql endpoint in this class
    //This function returns the full information about this class
    //@returns The full information about that dataset including its name and the sparql endpoint
    def getDBpediaSparqlEndpointDatasetInfo() : List[String] =
      {

        //Get the current directory in which the application resides because it also contains the configuration file
        val strDirectoryFullPath = Directory.Current.toList(0);

        val configFileFullPath = strDirectoryFullPath + "\\" + this.configFileName;

        val xmlConfigTree = XML.loadFile(configFileFullPath);

        //Reading the datasets that are loaded into the sparql endpoint
        var currentNode = xmlConfigTree \\ dbpediaSparqlDatasetTagName;
        val DatasetName = (currentNode \\ datasetNameTagName).text.trim;
        val SparqlEndPoint = (currentNode \\ sparqlEndpointTagName).text.trim;
        List(DatasetName, SparqlEndPoint);

      }
}
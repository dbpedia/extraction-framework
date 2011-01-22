package org.dbpedia.extraction.scripts

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: May 5, 2010
 * Time: 11:41:16 AM
 * This object creates the voiD file
 */

import org.openrdf.rio.turtle._
import org.openrdf.sail.memory.model.NumericMemLiteral;
import org.openrdf.repository._;
import java.io._;
import org.openrdf.model.impl._;
import org.openrdf.model._;
import org.openrdf.sail.nativerdf.model._;
import scala.io.Source._;


object voiDFileGenerator
{
  val voiDrdr = new voiDConfigurationFileReader();
  def main(args: Array[String])
    {
      voiDrdr.readConfigurationData();

      //Construct the list dataset descriptions
      val downloadPageCreatorLines = scala.io.Source.fromFile(new java.io.File(voiDrdr.downloadPageCreatorFileName)).mkString;
      voiDrdr.DatasetDescriptionList = constructDatasetDescriptionList(partitionDownloadPageCreatorFileForCoreDatasets(downloadPageCreatorLines, true), true);
      voiDrdr.DatasetDescriptionList = voiDrdr.DatasetDescriptionList :::
              constructDatasetDescriptionList(partitionDownloadPageCreatorFileForCoreDatasets(downloadPageCreatorLines, false), false);


      //Construct the list dataset file descriptions
      val coreFileLines  = scala.io.Source.fromFile(new java.io.File(voiDrdr.coreDatasetsDescriptionFileName)).mkString;
      getDatasetInfo(coreFileLines);

      val extendedFileLines  = scala.io.Source.fromFile(new java.io.File(voiDrdr.extendedDatasetsDescriptionFileName)).mkString;
      getDatasetInfo(extendedFileLines);

      try
      {
        writeDatasetsTovoiDFile();
      }
      catch
      {
        //case exp => println("Unable to write the voiD file");
        case exp => println(exp.toString());
      }

      //voiDrdr.readOwlDataset();

    }

  //This function takes the file contents (as string) as parameter and returns a list of type class DatasetFileDescription
  //containing the description of each dataset described in that file
  //@param  strFileContents   The contents of the file containing the description
  private def getDatasetInfo(strFileContents: String):Unit =
    {

      var listDatasetFiles = constructDatasetFileDescriptionList(partitionDatasetsFile(strFileContents));

      voiDrdr.DatasetDescriptionList.foreach(DatasetDesc =>
        {
          for(currentDatasetFile <- listDatasetFiles)
            {
              if(currentDatasetFile.DatasetFileName.startsWith(DatasetDesc.RootFileName))
                {
                  //The language is contained in the filename after the root filename
                  //e.g. DatasetFilesList long_abstracts_en.nt, the language is en
                  //so the language lies in position equals to the length of the root filename and with length 2
                  val Lang = currentDatasetFile.DatasetFileName.substring(DatasetDesc.RootFileName.size, DatasetDesc.RootFileName.size+2);
                  currentDatasetFile.Language = Lang;

                  DatasetDesc.DatasetFilesList = DatasetDesc.DatasetFilesList ::: List(currentDatasetFile);
                }
            }
        }
        );
    }

  //This function takes the file contents (as string) as parameter and returns a list of strings containing the file parts
  //@param  strFileContents   The contents of the file containing the description
  //@return A list containing the file parts
  private def partitionDatasetsFile(strFileContents: String):List[String] =
    {
      try
      {
        var listFileDescription : List[String] = List();

        //The idea of extracting the required information is to look for the single quotes because all of the required
        //information are always placed between single quotes
        var FirstSingleQuotePosion:Int = -1;
        var SecondSingleQuotePosion:Int = -1;
        var CurrentPosition:Int = 0;

        while(CurrentPosition < strFileContents.size)
          {
            FirstSingleQuotePosion = strFileContents.indexOf("'", CurrentPosition);

            if(FirstSingleQuotePosion<0)
              CurrentPosition = strFileContents.size; //This statement forces the loop to terminate
            else
              {
                //Now we have found a single quote and we must look for the next one
                CurrentPosition = FirstSingleQuotePosion+1;

                SecondSingleQuotePosion = strFileContents.indexOf("'", CurrentPosition+1);

                if(SecondSingleQuotePosion<0)
                  CurrentPosition = strFileContents.size; //This statement forces the loop to terminate
                else
                  {
                    //Now we have two single quotes, so we can now extract the required information
                    CurrentPosition = SecondSingleQuotePosion+1;

                    val str = strFileContents.substring(FirstSingleQuotePosion+1, SecondSingleQuotePosion);
                    listFileDescription = listFileDescription ::: List(str);
                  }
              }
          }
        listFileDescription;
      }
      catch
      {
        case exp => println("Unable to partion the php file describing the dataset files");
        null;
      }

    }

  //This function takes a list of strings containing the required dataset description but it also contains some unneeded
  //parts like the word "lines", and the word "filesize"
  //@param listFileParts A list containing the file parts
  //@return A list containing objects each one of them represents the full description of a specific dataset file
  private def constructDatasetFileDescriptionList(listFileParts :List[String]):List[DatasetFileDescription] =
    {
       var CurrentDataset : DatasetFileDescription = new DatasetFileDescription();
       var DatasetIndex : Int = 0;
       var listDatasets : List[DatasetFileDescription] = List();

           //The listFileParts is organized in the following manner
           //[Datasetname, "lines", #OfLines, "filesize", FileSize, "bzip2", DownloadFileSize,...]
           //So the important information is contained in indices 0, 2, 4, 6 respectively

            while(DatasetIndex<listFileParts.size)
              {
                CurrentDataset = new DatasetFileDescription();
                CurrentDataset.DatasetFileName = listFileParts(DatasetIndex);
                CurrentDataset.NumberOfTriples = listFileParts(DatasetIndex+2);
                CurrentDataset.FileSize = listFileParts(DatasetIndex+4);
                CurrentDataset.DownloadFileSize = listFileParts(DatasetIndex+6);
                CurrentDataset.SparqlEndPoint = voiDrdr.getDatasetSparqlEndPoint(CurrentDataset.DatasetFileName);

                //val SubjectObjectList = voiDrdr.getLinksDatasetFullInfo(CurrentDataset.DatasetFileName);
                //println(CurrentDataset.SparqlEndPoint);

                //Since the dataset is not stored directly on baseURL, but in a subfolder with language name
                //we must extract the language name (subfolder)
                val UnderscorePosition = CurrentDataset.DatasetFileName.lastIndexOf("_");
                val DotPosition = CurrentDataset.DatasetFileName.indexOf(".", UnderscorePosition);

                val FolderName = CurrentDataset.DatasetFileName.substring(UnderscorePosition+1, DotPosition);

                CurrentDataset.DumpFileLocation = voiDrdr.uriDumpBase + FolderName + "/" + CurrentDataset.DatasetFileName;

           listDatasets = listDatasets ::: List(CurrentDataset);
           DatasetIndex = DatasetIndex + 7;
         }

      listDatasets;
    }


  //This function partitions the DownloadPageCreator file in order to extract each dataset along with its description
  //and its filename
  //@param  strFileContents A string containing the contents of the file
  //@param  isCoreDatasets  A boolean indicator to indicate whether the fucntion will work on core or extended datasets
  //as this function is generic and works on both
  //@return A list of strings extracted from the file
  private def partitionDownloadPageCreatorFileForCoreDatasets(strFileContents: String, areCoreDatasets: Boolean):List[String] =
    {
      try
      {
        var DatasetsArrayStartPosition:Int = -1;
        if(areCoreDatasets)
          DatasetsArrayStartPosition = strFileContents.indexOf(voiDrdr.coreDatasetsArrayName);//This indicates the
          //start of the datasets array
        else
          DatasetsArrayStartPosition = strFileContents.indexOf(voiDrdr.extendedDatasetsArrayName);

        val semiColonPosition = strFileContents.indexOf(";", DatasetsArrayStartPosition);//This indicates the end of
        //the array
        //Cut the part starting from the name of the array till the ;
        val strDatasetDescription = strFileContents.substring(DatasetsArrayStartPosition, semiColonPosition);

        var listDatasetInfo : List[String] = List();

        //The idea of extracting the required information is to look for the single quotes because all of the required
        //information are always placed between single quotes
        var FirstSingleQuotePosion:Int = -1;
        var SecondSingleQuotePosion:Int = -1;
        var CurrentPosition:Int = 0;

        while(CurrentPosition < strDatasetDescription.size)
          {
            FirstSingleQuotePosion = strDatasetDescription.indexOf("'", CurrentPosition);

            if(FirstSingleQuotePosion<0)
              CurrentPosition = strFileContents.size; //This statement forces the loop to terminate
            else
              {
                //Now we have found a single quote and we must look for the next one
                CurrentPosition = FirstSingleQuotePosion+1;

                SecondSingleQuotePosion = strDatasetDescription.indexOf("'", CurrentPosition+1);

                if(SecondSingleQuotePosion<0)
                  CurrentPosition = strFileContents.size; //This statement forces the loop to terminate
                else
                  {
                    //Now we have two single quotes, so we can now extract the required information
                    CurrentPosition = SecondSingleQuotePosion+1;

                    val str = strDatasetDescription.substring(FirstSingleQuotePosion+1, SecondSingleQuotePosion);
                    listDatasetInfo = listDatasetInfo ::: List(str);
                  }
              }
          }
        listDatasetInfo;
      }
      catch
      {
        case exp => println("Unable to find the core datasets description in the downloadpagecreator file");
        null;
      }
    }

  //This function takes a list of strings containing the required dataset description but it also contains some unneeded
  //parts like the word "file", and the word "title"
  //@param listFileParts A list containing the file parts
  //@param  areCoreDatasets Indicates whether we are working on core datasets or extended datasets
  //@return A list containing objects each one of them represents the full description of a specific dataset
  private def constructDatasetDescriptionList(listFileParts :List[String], areCoreDatasets :Boolean):List[DatasetDescription] =
    {
       var CurrentDataset : DatasetDescription = new DatasetDescription();
       var DatasetIndex : Int = 0;
       var listDatasets : List[DatasetDescription] = List();

      //The listFileParts is organized in the following manner
      //["file", RootFileName, "title", DatasetTitle, "description", DatasetDescription,...]
      //So the important information is contained in indices 1, 3, 5 respectively

       while(DatasetIndex<listFileParts.size)
         {
           CurrentDataset = new DatasetDescription();
           //CurrentDataset.RootFileName = listFileParts(DatasetIndex);
           if(areCoreDatasets)
              CurrentDataset.DatasetType = "core";
           else
              CurrentDataset.DatasetType = "links";

           CurrentDataset.RootFileName = listFileParts(DatasetIndex+1);
           CurrentDataset.DatasetTitle = "DBpedia "+listFileParts(DatasetIndex+3);
           CurrentDataset.DatasetDescription = listFileParts(DatasetIndex+5);

           //Building snappy names for the datasets
           var DatasetSanappyName = CurrentDataset.DatasetTitle.replace(" ","")
           DatasetSanappyName = DatasetSanappyName.replace("(","");
           DatasetSanappyName = DatasetSanappyName.replace(")","");

           CurrentDataset.DatasetName = DatasetSanappyName;

           listDatasets = listDatasets ::: List(CurrentDataset);
           DatasetIndex = DatasetIndex + 6;
         }
      listDatasets;

    }

  private def writeDatasetsTovoiDFile()
    {
       val fs = new FileOutputStream(voiDrdr.voiDFileName);
      //var turtleWriter=new TurtleWriter(fs);
      var turtleWriter=new DBpediaTurtleWriter(fs);
      turtleWriter.startRDF();

      //Write the required namespaces
      voiDrdr.prefixNameSpaceList foreach ( (prefixNamespace) =>
             {
               turtleWriter.handleNamespace(prefixNamespace._1, prefixNamespace._2);
             }
           );

      val mainDatasetInfo = voiDrdr.getMainDatasetInfo();

      var MainDatasetName : URIImpl = new URIImpl(voiDrdr.uriBase + mainDatasetInfo.DatasetName);//This is the main dataset, of which
                                                        //all other datasets are subset

      var datasetName : URIImpl = new URIImpl("http://dbpedia.org/DBpedia");//Just initialization

      val objDatasetType = new URIImpl("http://rdfs.org/ns/void#Dataset");
      val objLinksetType = new URIImpl("http://rdfs.org/ns/void#Linkset");

      val xsdDateDatatype = new URIImpl("http://www.w3.org/2001/XMLSchema#date");


      var stmt : StatementImpl = new StatementImpl(MainDatasetName, voiDPredicates.PredType, objDatasetType);
      turtleWriter.handleStatement(stmt);//Write the main dataset

      stmt = new StatementImpl(MainDatasetName, voiDPredicates.PredTitle,
          new LiteralImpl(mainDatasetInfo.Title, voiDrdr.titleLanguage));
      turtleWriter.handleStatement(stmt);//Write the title of the main dataset

      stmt = new StatementImpl(MainDatasetName, voiDPredicates.PredDescription,
          new LiteralImpl(mainDatasetInfo.Description, voiDrdr.titleLanguage));
      turtleWriter.handleStatement(stmt);//Write the description of the main dataset

      stmt = new StatementImpl(MainDatasetName, voiDPredicates.PredHomepage,
          new URIImpl(mainDatasetInfo.Homepage));
      turtleWriter.handleStatement(stmt);//Write the homepage of the main dataset

      stmt = new StatementImpl(MainDatasetName, voiDPredicates.PredURIRegexPattern,
          new LiteralImpl(mainDatasetInfo.URIRegexPattern));
      turtleWriter.handleStatement(stmt);//Write the URI regex pattern of the main dataset

      stmt = new StatementImpl(MainDatasetName, voiDPredicates.PredModified,
          new LiteralImpl(mainDatasetInfo.ModifiedDate.toString(), xsdDateDatatype));
      turtleWriter.handleStatement(stmt);//Write the homepage of the main dataset

      //Writing the sources of the main DBpedia dataset
      mainDatasetInfo.SourceList.foreach(Source =>
        {
           stmt = new StatementImpl(MainDatasetName, voiDPredicates.PredSource,
              new URIImpl(Source));
            turtleWriter.handleStatement(stmt);//Write a source from the sources of the main dataset
        }
      );

      //Writing the contributors of the main DBpedia dataset
      mainDatasetInfo.ContributorList.foreach(Contributor =>
        {
           stmt = new StatementImpl(MainDatasetName, voiDPredicates.PredContributor,
              new URIImpl(Contributor));
            turtleWriter.handleStatement(stmt);//Write a contributor from the contributors of the main dataset
        }
      );

      //Writing the example resources of the main DBpedia dataset
      mainDatasetInfo.ExampleResourceList.foreach(ExampleResource =>
        {
           stmt = new StatementImpl(MainDatasetName, voiDPredicates.PredExampleResource,
              new URIImpl(ExampleResource));
            turtleWriter.handleStatement(stmt);//Write an example resource from the resources of the main dataset
        }
      );

      val DBpediaSparqlDatasetInfo = voiDrdr.getDBpediaSparqlEndpointDatasetInfo();
      val DBpediaSparqlDataset = DBpediaSparqlDatasetInfo(0);
      val DBpediaSparqlDatasetEndpoint = DBpediaSparqlDatasetInfo(1);

      val DBPediaSparqlDatasetName =new URIImpl(voiDrdr.uriBase + DBpediaSparqlDataset); //Make the new DBpediaSPARQL dataset
          //as teh main dataset as all other datasets will be subsets of it not the DBpedia dataset

      //Make DBpediaSPARQL dataset a subset of the main dataset
      stmt = new StatementImpl(MainDatasetName, voiDPredicates.PredSubdataset, DBPediaSparqlDatasetName);
      turtleWriter.handleStatement(stmt);

      println(DBpediaSparqlDatasetInfo);

      stmt = new StatementImpl(DBPediaSparqlDatasetName, voiDPredicates.PredType, objDatasetType);
      turtleWriter.handleStatement(stmt);//Write the DBpediaSPARQL dataset

      stmt = new StatementImpl(DBPediaSparqlDatasetName, voiDPredicates.PredSparqlEndPoint,
          new URIImpl(mainDatasetInfo.SparqlEndPoint));
      turtleWriter.handleStatement(stmt);//Write the sparql endpoint of the main dataset

      var lastStatement = stmt; //This variable is used to avoid the repetition of the subdataset name, as its the same
        //for .nt and .nq files

      var LinkDatasetsToDBpediaSparqlDatasetStatements: List[Statement] = List();//This is a list of statements that relate
          //the main datasets to the largest dataset containing the whole file (DBpediaSPARQL dataset)

      //The following loop is necessary to connect the main dataset which is the parent of all datasets to its children
       voiDrdr.DatasetDescriptionList.foreach(CurrentDatasetDesc =>
         {
            //Placing dataset name
            datasetName = new URIImpl(voiDrdr.uriBase+CurrentDatasetDesc.DatasetName);
            //Declaring that the current dataset is a subset of the main dataset
            stmt = new StatementImpl(DBPediaSparqlDatasetName, voiDPredicates.PredSubdataset, datasetName);
            LinkDatasetsToDBpediaSparqlDatasetStatements = LinkDatasetsToDBpediaSparqlDatasetStatements ::: List(stmt);
         });

         //Write the statements to the document
        LinkDatasetsToDBpediaSparqlDatasetStatements.foreach (Statement => turtleWriter.handleStatement(Statement));

        //The owl dataset has special method of handling, so the following part is responsible for writing this special
        //dataset to the turtle file.
        val OWLDataset = voiDrdr.readOwlDataset();
        datasetName = new URIImpl(voiDrdr.uriBase + OWLDataset.DatasetName);
        //Declaring that the current dataset is a subset of the main dataset
        stmt = new StatementImpl(DBPediaSparqlDatasetName, voiDPredicates.PredSubdataset, datasetName);
        turtleWriter.handleStatement(stmt);

        //The OWL dataset must be a core dataset
        stmt = new StatementImpl(datasetName, voiDPredicates.PredType, objDatasetType);
        turtleWriter.handleStatement(stmt);

        //Writing the OWL dataset title
        stmt = new StatementImpl(datasetName, voiDPredicates.PredTitle, new LiteralImpl(OWLDataset.DatasetTitle, voiDrdr.titleLanguage));
        turtleWriter.handleStatement(stmt);

        //Writing the OWL dataset description
        stmt = new StatementImpl(datasetName, voiDPredicates.PredDescription, new LiteralImpl(OWLDataset.DatasetDescription,
          voiDrdr.titleLanguage));
        turtleWriter.handleStatement(stmt);

        //Writing the predicate stating the dump file
        stmt = new StatementImpl(datasetName, voiDPredicates.PredDataDump, new URIImpl(OWLDataset.DatasetFilesList(0).DumpFileLocation));
        turtleWriter.handleStatement(stmt);

      voiDrdr.DatasetDescriptionList.foreach(CurrentDatasetDesc =>
        {
          //Placing dataset name
          datasetName = new URIImpl(voiDrdr.uriBase+CurrentDatasetDesc.DatasetName);
          //println(datasetName);

          if(CurrentDatasetDesc.DatasetType.equalsIgnoreCase("core"))
            stmt = new StatementImpl(datasetName, voiDPredicates.PredType, objDatasetType);
          else
            stmt = new StatementImpl(datasetName, voiDPredicates.PredType, objLinksetType);

          turtleWriter.handleStatement(stmt);

          //Placing dataset title
          stmt = new StatementImpl(datasetName, voiDPredicates.PredTitle, new LiteralImpl(CurrentDatasetDesc.DatasetTitle,
              voiDrdr.titleLanguage));
          turtleWriter.handleStatement(stmt);

          //Placing description
          stmt = new StatementImpl(datasetName, voiDPredicates.PredDescription, new LiteralImpl(CurrentDatasetDesc.DatasetDescription,
            voiDrdr.titleLanguage));
          turtleWriter.handleStatement(stmt);

          //
          var SubdatasetsStatements : List[Statement] = List();//This is a list of all statements related to the
          //sub datasets in different languages

          var IncludeQuotesInFileSizes : List[Boolean] = List();//This list is required to determine whether to include
          //the double quotes around 

          var LinkSubdatasetToParentDatasetStatements: List[Statement] = List();//This is a list of statements that relate
          //a dataset to its subsets (in different languages)

          //Print subdatasets to the voiD file, which are belonging to the same dataset but in different languages
          CurrentDatasetDesc.DatasetFilesList.foreach(CurrentDatasetFile =>
            {
              //Use the DatasetFileName as the dataset name used in the voiD file, but without dots
              val strSubdatasetName = Utilities.getSubDatasetNameWithoutDots(CurrentDatasetFile.DatasetFileName);
              //println(CurrentDatasetFile.DatasetFileName +"    "+strSubdatasetName);

              var SubdatasetName = new URIImpl(voiDrdr.uriBase + strSubdatasetName);
              //The type of the subdataset is the same as the type of its parent dataset
              if(CurrentDatasetDesc.DatasetType.equalsIgnoreCase("core"))
                {
                  stmt = new StatementImpl(SubdatasetName, voiDPredicates.PredType, objDatasetType);
                  SubdatasetsStatements = SubdatasetsStatements ::: List(stmt);
                }
              else
                {
                  //If we are handling a links dataset, we must get its subject dataset along with its object dataset
                  //from the config file
                  val linksetInfo = voiDrdr.getLinksDatasetFullInfo(CurrentDatasetFile.DatasetFileName);
                  if(linksetInfo!=null)
                    {
                      stmt = new StatementImpl(datasetName, voiDPredicates.PredSubjectTarget, new URIImpl(linksetInfo.SubjectDataset));
                      LinkSubdatasetToParentDatasetStatements = LinkSubdatasetToParentDatasetStatements ::: List(stmt);

                      stmt = new StatementImpl(datasetName, voiDPredicates.PredObjectTarget, new URIImpl(linksetInfo.ObjectDataset));
                      LinkSubdatasetToParentDatasetStatements = LinkSubdatasetToParentDatasetStatements ::: List(stmt);

                      stmt = new StatementImpl(datasetName, voiDPredicates.PredLinkPredicate, new URIImpl(linksetInfo.LinkPredicate));
                      LinkSubdatasetToParentDatasetStatements = LinkSubdatasetToParentDatasetStatements ::: List(stmt);
                      
                      /*stmt = new StatementImpl(datasetName, voiDPredicates.PredType, objDatasetType);
                      LinkSubdatasetToParentDatasetStatements= LinkSubdatasetToParentDatasetStatements ::: List(stmt);*/
                    }
                  
                }

              /*
             //Declare the sparql endpoint of the dataset, only in case it is loaded into the sparql endpoint
              if((CurrentDatasetFile.SparqlEndPoint!=null) && (!CurrentDatasetFile.SparqlEndPoint.isEmpty))
                {
                  if(CurrentDatasetDesc.DatasetType.equalsIgnoreCase("core"))
                    {
                      stmt = new StatementImpl(SubdatasetName, voiDPredicates.PredSparqlEndPoint,
                          new URIImpl(CurrentDatasetFile.SparqlEndPoint));
                      //turtleWriter.handleStatement(stmt);//Write the main dataset
                      SubdatasetsStatements = SubdatasetsStatements ::: List(stmt);
                    }
                  else//In case of links datasets we must attach the sparql endpoint directly to the dataset itself
                    {
                      stmt = new StatementImpl(datasetName, voiDPredicates.PredSparqlEndPoint, new URIImpl(CurrentDatasetFile.SparqlEndPoint));
                      //turtleWriter.handleStatement(stmt);//Write the main dataset
                      LinkSubdatasetToParentDatasetStatements = LinkSubdatasetToParentDatasetStatements ::: List(stmt);
                    }
                }
               */
              
              if(CurrentDatasetDesc.DatasetType.equalsIgnoreCase("core"))
                {
                    //Writing the number of triples contained in the dataset
                    stmt = new StatementImpl(SubdatasetName, voiDPredicates.PredNumberOftriples,
                    new NumericLiteralImpl(
                      Utilities.getNumberOfTriplesAsInteger(CurrentDatasetFile.NumberOfTriples).toString().toLong));
                    SubdatasetsStatements = SubdatasetsStatements ::: List(stmt);

                    //Writing the predicate that relates the sub-dataset to language of its dump file
                    stmt = new StatementImpl(SubdatasetName, voiDPredicates.PredLanguage, new LiteralImpl(CurrentDatasetFile.Language));
                    SubdatasetsStatements = SubdatasetsStatements ::: List(stmt);

                    //Writing the predicate that relates the sub-dataset to its dump file
                    stmt = new StatementImpl(SubdatasetName, voiDPredicates.PredDataDump, new URIImpl(CurrentDatasetFile.DumpFileLocation));
                    SubdatasetsStatements = SubdatasetsStatements ::: List(stmt);

                    //Writing the FileSize the dataset
                    stmt = new StatementImpl(SubdatasetName, voiDPredicates.PredUncompressedFileSize,
                    new NumericLiteralImpl(Utilities.getFileSizeInBytes(CurrentDatasetFile.FileSize).toString().toLong));
                    SubdatasetsStatements = SubdatasetsStatements ::: List(stmt);

                    //Writing the FileSize the dataset in compressed state
                    stmt = new StatementImpl(SubdatasetName, voiDPredicates.PredFileSize,
                    new NumericLiteralImpl(Utilities.getFileSizeInBytes(CurrentDatasetFile.DownloadFileSize).toString().toLong));
                    SubdatasetsStatements = SubdatasetsStatements ::: List(stmt);

                    //Writing the file format i.e. nt or nq
                    stmt = new StatementImpl(SubdatasetName, voiDPredicates.PredFormat,
                    new LiteralImpl(CurrentDatasetFile.getFormat()));
                    /*stmt = new StatementImpl(SubdatasetName, voiDPredicates.PredCompressedFileSize,
                    new DBpediaNumbericLiteralImpl());*/
                  
                    SubdatasetsStatements = SubdatasetsStatements ::: List(stmt);


                }
              else//Case of links datasets, we don't have subdatasets, so all information will be attached to the parent
                  //dataset directly
                {
                    //Writing the number of triples contained in the dataset
                    stmt = new StatementImpl(datasetName, voiDPredicates.PredNumberOftriples,
                    new NumericLiteralImpl(Utilities.getNumberOfTriplesAsInteger(CurrentDatasetFile.NumberOfTriples).toString().toLong));
                    LinkSubdatasetToParentDatasetStatements = LinkSubdatasetToParentDatasetStatements ::: List(stmt);

                    //Writing the predicate that relates the sub-dataset to its dump file
                    stmt = new StatementImpl(datasetName, voiDPredicates.PredDataDump, new URIImpl(CurrentDatasetFile.DumpFileLocation));
                    LinkSubdatasetToParentDatasetStatements = LinkSubdatasetToParentDatasetStatements ::: List(stmt);

                    //Writing the FileSize the dataset
                    stmt = new StatementImpl(datasetName, voiDPredicates.PredUncompressedFileSize,
                    new NumericLiteralImpl(Utilities.getFileSizeInBytes(CurrentDatasetFile.FileSize).toString().toLong));

                    LinkSubdatasetToParentDatasetStatements = LinkSubdatasetToParentDatasetStatements ::: List(stmt);

                    //Writing the FileSize the dataset in compressed state
                    stmt = new StatementImpl(datasetName, voiDPredicates.PredFileSize,
                    new NumericLiteralImpl(Utilities.getFileSizeInBytes(CurrentDatasetFile.DownloadFileSize).toString().toLong));
                    LinkSubdatasetToParentDatasetStatements = LinkSubdatasetToParentDatasetStatements ::: List(stmt);

                    //Writing the predicate that relates the sub-dataset to language of its dump file
                    //stmt = new StatementImpl(datasetName, voiDPredicates.PredLanguage, new LiteralImpl(CurrentDatasetFile.Language));
                    //LinkSubdatasetToParentDatasetStatements = LinkSubdatasetToParentDatasetStatements ::: List(stmt);

                }
              //Declaring this as a subset from the parent dataset, only in case of core datasets, as links datasets
              //don't need to be divided
              if(CurrentDatasetDesc.DatasetType.equalsIgnoreCase("core"))
                {
                  stmt = new StatementImpl(datasetName, voiDPredicates.PredSubdataset, SubdatasetName);
                  //Avoid repetition in subdataset names 
                  if(lastStatement!=stmt)
                    {
                      LinkSubdatasetToParentDatasetStatements = LinkSubdatasetToParentDatasetStatements ::: List(stmt);
                      lastStatement = stmt;
                    }
                }

            });

          LinkSubdatasetToParentDatasetStatements.foreach (Statement =>{ 
            if(Statement.getObject().isInstanceOf[NumericLiteralImpl])
              turtleWriter.handleStatement(Statement, false);
            else
              turtleWriter.handleStatement(Statement)});

          if(CurrentDatasetDesc.DatasetType.equalsIgnoreCase("core"))
            SubdatasetsStatements = reorderSubdatasetStatements(SubdatasetsStatements);
          
          SubdatasetsStatements.foreach (Statement =>{
            //println(Statement.getObject);

            if(Statement.getObject().isInstanceOf[NumericLiteralImpl])
              turtleWriter.handleStatement(Statement, false);
            else
              turtleWriter.handleStatement(Statement);
          }
            );

        });

      //Writing the list of datasets to which DBpedia is related to e.g. USCensus
      val listDatasetsLinkedToDBpedia = voiDrdr.getDatasetsLinkedToDBpedia();
      listDatasetsLinkedToDBpedia.foreach(dataset =>
        {
          //Writing the dataset name
          val DatasetFullName = new URIImpl(voiDrdr.uriBase+dataset.DatasetName);
          stmt = new StatementImpl(DatasetFullName, voiDPredicates.PredType, objDatasetType);
          turtleWriter.handleStatement(stmt);

          //Writing the dataset title
          stmt = new StatementImpl(DatasetFullName, voiDPredicates.PredTitle,
            new LiteralImpl(dataset.DatasetTitle, voiDrdr.titleLanguage));
          turtleWriter.handleStatement(stmt);

          //Writing the dataset homepage
          stmt = new StatementImpl(DatasetFullName, voiDPredicates.PredHomepage, new URIImpl(dataset.Homepage));
          turtleWriter.handleStatement(stmt);

        }
        );

      turtleWriter.endRDF();

      println("voiD file created successfully");
    }

  //This function is responsible for reordering the statements of the subdatasets, as it is required to make the subdatasets
  // of the same language as one dataset and divide them into 2 different dumps with all common information e.g.
  //NumberOfTriples, and Language place at first as a property for the dataset, whereas the information specific to
  //each dump e.g. fileSize is placed with the dump itself
  //@param  listSubdatasetStatements  List of statements of ths subdatasets
  //@returns  A new list of statements that is properly ordered
  private def reorderSubdatasetStatements(listSubdatasetStatements : List[Statement]): List[Statement] =
    {
      if(listSubdatasetStatements.length<=0)
        return List();

      var outputList : List[Statement] = List();
      var dataDumpList : List[Statement] = List();//This list contains the statements that are used to descripe the
          //the dumps e.g. uncompressedSize
      
      //This boolean variable is used to determine whether the dataset has been traversed before i.e. the common data is
      //placed e.g. Triples and Language, and this data must not be repeated again
      var bSubdataSetAlreadyTraversed = false;

      var dataDumpURI :Resource = new URIImpl("http://dbpedia.org/DBpedia");//Just initialization

      listSubdatasetStatements.foreach(Statement =>
        {
          val Predicate = Statement.getPredicate();
          Predicate match
          {
            case voiDPredicates.PredType =>
              //This is the first time to traverse this dataset and so place its type directly to the output list
              if(!bSubdataSetAlreadyTraversed)
                outputList = outputList ::: List(Statement);

            case voiDPredicates.PredNumberOftriples =>
              //This is the first time to traverse this dataset and so place its type directly to the output list
              if(!bSubdataSetAlreadyTraversed)
                outputList = outputList ::: List(Statement);

            case voiDPredicates.PredLanguage =>
              //This is the first time to traverse this dataset and so place its type directly to the output list
              if(!bSubdataSetAlreadyTraversed)
                outputList = outputList ::: List(Statement);

            case voiDPredicates.PredDataDump =>
              //In case of dataDump, we place it directly to the outputList, and establish a short list describing the
              //dump itself
              outputList = outputList ::: List(Statement);
              dataDumpURI = new URIImpl(Statement.getObject().toString());

            case voiDPredicates.PredUncompressedFileSize =>
              //We place the statements describing the dataDump in a short list and eventually place it in the outputList
              val Stmt = new StatementImpl(dataDumpURI, Statement.getPredicate(), Statement.getObject());
              dataDumpList = dataDumpList ::: List(Stmt);

            case voiDPredicates.PredFileSize =>
              //We place the statements describing the dataDump in a short list and eventually place it in the outputList
              val Stmt = new StatementImpl(dataDumpURI, Statement.getPredicate(), Statement.getObject());
              dataDumpList = dataDumpList ::: List(Stmt);

            case voiDPredicates.PredFormat =>
              //We place the statements describing the dataDump in a short list and eventually place it in the outputList
              val Stmt = new StatementImpl(dataDumpURI, Statement.getPredicate(), Statement.getObject());
              dataDumpList = dataDumpList ::: List(Stmt);

              bSubdataSetAlreadyTraversed = !bSubdataSetAlreadyTraversed;

              //Append the statements desrcibing the data dumps to the list, if we have finished the subdataset
              if(!bSubdataSetAlreadyTraversed)
                {
                  dataDumpList.foreach(dumpStmt => outputList = outputList ::: List(dumpStmt));
                  dataDumpList = List();
                }

            case _ =>
              ;
          }
        });

      outputList;
    }
}

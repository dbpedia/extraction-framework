package org.dbpedia.extraction.scripts

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: May 7, 2010
 * Time: 3:39:44 PM
 * This class holds the information describing the file of a dataset that will be extracted from php files that are stated in the
 * configuration file
 */

class DatasetFileDescription
{
  var DatasetFileName : String = "";
  var NumberOfTriples : String = "";
  var FileSize : String = "";
  var DownloadFileSize : String = "";

  
  var DumpFileLocation : String = "";
  var SparqlEndPoint : String = "";
  var SubjectTarget : String = "";
  var ObjectTarget : String = "";
  var Language : String = "";

  //Override for the function toString
  override def toString = "Dataset file name = "+DatasetFileName + "\nlanguage = " + Language +
                          "\nnumber of triples = " + NumberOfTriples + "\ndump file location = " + DumpFileLocation +
                          "\nfile size = " + FileSize + "\ndownload file size = " + DownloadFileSize +
                          "\nSPARQL endpoint = " + SparqlEndPoint + "\nsubject target = " + SubjectTarget +
                          " \nobject target = " + ObjectTarget + "\nFromat= " + getFormat() + "\n";

  //This function checks whether the passed DatasetFileName is the same as one represented within this object
  //@param  RequiredDatasetFileName The file name that must be checked against the internal DatasetFileName represented
  // within this object
  //@return The result of comparing the two FileNames   
  def isEqualDatasetFileName(RequiredDatasetFileName : String) : Boolean=
    {
      DatasetFileName.equalsIgnoreCase(RequiredDatasetFileName);
    }

  override def equals(RequiredItem :Any):Boolean =
    {
      RequiredItem match
      {
        case that: DatasetFileDescription => that.DatasetFileName.equals(this.DatasetFileName);
        case _ => false;
  
      }
    }

  //This function returns the format of the subdataset which is nt or nq
  //@returns  The file format
  def getFormat():String =
    {
      //The file format is contained in the DatasetFileName after the last dot
      val DotPosition = DatasetFileName.lastIndexOf(".");
      if(DotPosition<0)
        "";
      val Format = DatasetFileName.substring(DotPosition+1);
      Format;
    }

  //def setLangu

  /*//This function returns the number of triples in the file as integer number as it is required to be written in voiD file
  def getNumberOfTriplesAsInteger() : Int =
    {
      try
      {
          val LastChar = NumberOfTriples(NumberOfTriples.size-1);
          var NumOfTriples :Int = 0;
          //println(NumberOfTriples(NumberOfTriples.size-1)+ "  "+ NumberOfTriples);
          LastChar match
          {
            case 'K' | 'k' =>
                val NumberPart = NumberOfTriples.substring(0, NumberOfTriples.size-2);//Get the number of triples
                                                                                            //without the K poststring
                NumOfTriples = (NumberPart.toFloat * 1000).toInt;

            case 'M' | 'm' =>
                val NumberPart = NumberOfTriples.substring(0, NumberOfTriples.size-2);//Get the number of triples
                                                                                            //without the M poststring
                NumOfTriples = (NumberPart.toFloat * 1000 * 1000).toInt;

            case 'G' | 'g' =>
                val NumberPart = NumberOfTriples.substring(0, NumberOfTriples.size-2);//Get the number of triples
                                                                                            //without the G poststring
                NumOfTriples = (NumberPart.toFloat * 1000 * 1000 * 1000).toInt;

            case _ => NumOfTriples = NumberOfTriples.toInt;
          }

          //println(NumberOfTriples + "     " + NumOfTriples);
        NumOfTriples;
      }
      catch
      {
        case exp => 0;
      }
    }*/
}
package org.dbpedia.extraction.scripts

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: May 19, 2010
 * Time: 4:17:56 PM
 * This class contains some helpful methods used in several places in the project
 */


object Utilities
{
  //This function returns the size of the file in bytes as it is required to be written in voiD file
  //@param  FileSize  The file size in KB, MB, GB, or TB
  //returns The file size in bytes 
  def getFileSizeInBytes(FileSize : String) : Long =
    {
      try
      {
          val FileSizePostString = FileSize.substring(FileSize.size-2);
          var FileSizeInBytes :Long = 0;

          val NumberPart = FileSize.substring(0, FileSize.size-2);//Get the number of triples
                                                                  //without the KB, MB, ... poststring
          //println(NumberPart.toFloat+ "   " + NumberPart);

          if(FileSizePostString.equalsIgnoreCase("kb"))
            {
              FileSizeInBytes = (NumberPart.toFloat * 1024).toLong;
            }
          else if(FileSizePostString.equalsIgnoreCase("mb"))
            {
                FileSizeInBytes = (NumberPart.toFloat * 1024 * 1024).toLong;
            }
          else if(FileSizePostString.equalsIgnoreCase("gb"))
            {
                FileSizeInBytes = (NumberPart.toFloat * 1024 * 1024 * 1024).toLong;
            }
          else if(FileSizePostString.equalsIgnoreCase("tb"))
            {
               FileSizeInBytes = (NumberPart.toFloat * 1024 * 1024 * 1024 * 1024).toLong;
            }
          else
            {
              val FileSizeWithoutBytes = FileSize.replace("Bytes","");
              FileSizeInBytes = FileSizeWithoutBytes.toLong;
            }

        FileSizeInBytes;
      }
      catch
      {
        case exp => 0;
      }
    }

  //This function returns the number of triples in the file as integer number as it is required to be
  // written in voiD file
  //@param  NumberOfTriples Number of triples expressed in format 1.1m, 1.1g, and so on
  //returns The number of triples as integer 
  def getNumberOfTriplesAsInteger(NumberOfTriples : String) : Long =
    {
      try
      {
          val LastChar = NumberOfTriples(NumberOfTriples.size-1);
          var NumOfTriplesAsInteger :Long = 0;
          val NumberPart = NumberOfTriples.substring(0, NumberOfTriples.size-1);//Get the number of triples
                                                                                //without the K, M, ... poststring
          //println(NumberPart.toFloat+ "   " + NumberPart);
          LastChar match
          {
            case 'K' | 'k' =>
                NumOfTriplesAsInteger = (NumberPart.toFloat * 1000).toLong;

            case 'M' | 'm' =>
                NumOfTriplesAsInteger = (NumberPart.toFloat * 1000 * 1000).toLong;

            case 'G' | 'g' =>
                NumOfTriplesAsInteger = (NumberPart.toFloat * 1000 * 1000 * 1000).toLong;

            case 'T' | 't' =>
                NumOfTriplesAsInteger = (NumberPart.toFloat * 1000 * 1000 * 1000 * 1000).toLong;

            case _ => NumOfTriplesAsInteger = NumberOfTriples.toLong;
          }

        NumOfTriplesAsInteger;
      }
      catch
      {
        case exp => 0;
      }
    }

  //This function creates a SubdatasetName from the passed SubdatasetName after eliminating the dots, because the
  //the names of the subdatasets contain .nt or .nq at the end, and those are not needed
  def getSubDatasetNameWithoutDots(SubdatasetName : String):String=
    {
      val DotPosition = SubdatasetName.lastIndexOf(".");
      val str = SubdatasetName.substring(0,DotPosition);
      str;
    }
}
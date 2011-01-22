package org.dbpedia.extraction.scripts

/**
 * Created by IntelliJ IDEA.
 * User: mabrouk
 * Date: May 7, 2010
 * Time: 1:29:43 PM
 * To change this template use File | Settings | File Templates.
 */

object CommonTasksHandler
{
  //This function is important because when we we get the node text from the config file, it returns the actual
  //node text along with blank space, so we must remove those blank space, in order to get the real node text
  def removeBlankSpacesFromString(inputString:String):String=
    {
      var processString:String = inputString;
      processString = processString.replace(" ","");
      processString = processString.replace("\n","");
      processString = processString.replace("\t","");
      processString;
    }
}
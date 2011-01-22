package org.dbpedia.extraction.scripts
/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: Apr 24, 2010
 * Time: 4:07:18 PM
 * This class is used to write the XML data of the Sitemap.
 */

import scala.xml._;
import javax.xml._;

class XMLHandler
{
  private var xmlTree: Elem = <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:sc="http://sw.deri.org/2007/07/sitemapextension/scschema.xsd"></urlset>;
  private var internalNode :Elem = <sc:dataset></sc:dataset>

  def resetXMLTree = xmlTree= <urlset></urlset>;

  //Loads the xml data from the passed file to the XML tree internally represented by the class
  def loadFile(filePath: String):Unit =
    {
      try
      {
        xmlTree = XML.loadFile(filePath);
      }
      catch
      {
         case exp => println("Unable to load XML file, the XML file may be missing");
      }
    }

  //Appends the files list passed to the XML tree internally represented by the class
  def appendDirectoryFiles(filesList: List[String] ):Unit=
    {
      try
      {
         if(xmlTree == null )
          xmlTree= <urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:sc="http://sw.deri.org/2007/07/sitemapextension/scschema.xsd"></urlset>;
        internalNode = <sc:dataset></sc:dataset>;

        for(fileName <- filesList)
          {
            val fileNode: Node = <sc:dataDumpLocation>{fileName}</sc:dataDumpLocation>;

            internalNode=new Elem(internalNode.prefix, internalNode.label, internalNode.attributes, internalNode.scope,
            internalNode.child ++ fileNode: _*);

          }
        //xmlTree=new Elem(xmlTree.prefix, xmlTree.label, xmlTree.attributes, xmlTree.scope,
          //  xmlTree.child ++ internalNode: _*);
      }
      catch
      {
        case exp => println("Unable to append files to the xml tree");
      }
    }

  //Saves the xml tree internally represent by the class to the passed file
  //filePath: the path to which the file will be saved
  def saveFile(filePath: String):Unit =
    {
      try
      {
        xmlTree=new Elem(xmlTree.prefix, xmlTree.label, xmlTree.attributes, xmlTree.scope,
            xmlTree.child ++ internalNode: _*);
        XML.save(filePath, xmlTree, "UTF-8", true, null);
        println("XML file successfully saved");
      }
      catch
      {
        case exp => println("Unable to save the XML file, the file path may be incorrect");
      }
    }

  //Appends a new node to the XML tree internally represented by the class
  //nodeName: The name of the node (NodeKey)
  //nodeValue: The value of the node
  //attributes: List of attributes of the node
  def appendNode(nodeName : String, nodeValue : String, attributes: Map[String, String], tagPrefix: String):Unit=
    {
      val node = xmlTree \\ "dataset";


      var newNode:Elem = null;
      //The node has no attributes
      if(attributes == null)
        {
          /*if(appendPrefix)
            newNode = new Elem("sc", nodeName, Null, TopScope, Text(nodeValue));
          else
             newNode = new Elem(null, nodeName, Null, TopScope, Text(nodeValue));*/

          newNode = new Elem(tagPrefix, nodeName, Null, TopScope, Text(nodeValue));

        }

      else
        {
          //The node has several attributes, so we create an Attribute list
           var attributeList:MetaData = null;

          //Iterate through the attributes to get the name and the value of each one
          //attributes foreach ( (t2) => println (t2._1 + "----->" + t2._2))
           attributes foreach ( (currentAttribute) =>
             {
               if(attributeList == null)
                 {
                attributeList = new UnprefixedAttribute(currentAttribute._1, currentAttribute._2, Null);
                 }
               else
                 {
                 attributeList = attributeList.append(new UnprefixedAttribute(currentAttribute._1, currentAttribute._2, Null));
                 }
             }
             );
          /*if(appendPrefix)
            newNode = new Elem("sc", nodeName, attributeList, TopScope, Text(nodeValue));
          else
            newNode = new Elem(null, nodeName, attributeList, TopScope, Text(nodeValue));*/

          newNode = new Elem(tagPrefix, nodeName, attributeList, TopScope, Text(nodeValue));
        }
         
      //Attach the newly created node to the XML tree internally represented by the class
      //xmlTree=new Elem(xmlTree.prefix, xmlTree.label, xmlTree.attributes, xmlTree.scope,
        //    xmlTree.child ++ newNode: _*);

       //internalNode=new Elem(internalNode.prefix, internalNode.label, internalNode.attributes, internalNode.scope,
            //internalNode.child ++ fileNode: _*);
      internalNode=new Elem(internalNode.prefix, internalNode.label, internalNode.attributes, internalNode.scope,
            internalNode.child ++ newNode: _*);
      
    }
}
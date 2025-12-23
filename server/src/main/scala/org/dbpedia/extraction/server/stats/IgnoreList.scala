package org.dbpedia.extraction.server.stats

import java.io.{File,FileOutputStream,OutputStreamWriter}
import scala.collection.mutable
import scala.io.Source
import org.dbpedia.extraction.server.util.CollectionReader
import IgnoreList._

object IgnoreList
{
  val TemplatesTag = "templates|"
  val TemplateTag = "template|"
  val PropertiesTag = "properties|"
}



/**
 * Contains the ignored templates and properties
 */
class IgnoreList(file: File, update: () => Unit)
{
  private val templates = new mutable.LinkedHashSet[String]
  private val properties = new mutable.LinkedHashMap[String, mutable.Set[String]]

  if (file.exists) {
    val source = Source.fromFile(file, "UTF-8")
    try {
      val reader = new CollectionReader(source.getLines)
      val templateCount = reader.readCount(TemplatesTag)
      var templateIndex = 0
      while(templateIndex < templateCount) {
        templates += reader.readLine
        templateIndex += 1
      }
      reader.readEmpty
      
      val propertiesCount = reader.readCount(PropertiesTag)
      reader.readEmpty
      
      var propertiesIndex = 0
      while(propertiesIndex < propertiesCount) {
        val template = reader.readTag(TemplateTag)
        val props = properties.getOrElseUpdate(template, new mutable.LinkedHashSet[String])
        val propsCount = reader.readCount(PropertiesTag)
        var propsIndex = 0
        while(propsIndex < propsCount) {
          props += reader.readLine
          propsIndex += 1
        }
        reader.readEmpty
        propertiesIndex += 1
      }
      reader.readEnd
    } 
    finally source.close
  }
    
  /**
   * Must only be called from synchronized blocks.
   */
  // package-private for IgnoreListTest
  private[stats] def save() {
    
    update()
    
    val out = new FileOutputStream(file)
    try {
      val writer = new OutputStreamWriter(out, "UTF-8")
      writer.write(TemplatesTag+templates.size+'\n')
      for (template <- templates) {
        writer.write(template+'\n')
      }
      writer.write('\n')
      
      writer.write(PropertiesTag+properties.size+'\n')
      
      writer.write('\n')
      for ((template, props) <- properties) {
        writer.write(TemplateTag+template+"\n")
        writer.write(PropertiesTag+props.size+"\n")
        for (prop <- props) {
          writer.write(prop+'\n')
        }
        writer.write('\n')
      }
      
      writer.close
    } 
    finally out.close
  }
  
  def isTemplateIgnored(template: String) : Boolean = synchronized {
    templates.contains(template)
  }

  def isPropertyIgnored(template: String, property: String) : Boolean = synchronized {
    properties.contains(template) && properties(template).contains(property)
  }

  def addTemplate(template: String) = synchronized {
    if (templates.add(template)) save()
  }

  def removeTemplate(template: String) = synchronized {
    if (templates.remove(template)) save()
  }

  def addProperty(template: String, property: String) = synchronized {
    if (properties.getOrElseUpdate(template, new mutable.HashSet[String]).add(property)) save()
  }

  def removeProperty(template: String, property: String) = synchronized {
    if (properties.contains(template) && properties(template).remove(property)) save()
  }
  
}
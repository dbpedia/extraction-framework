package org.dbpedia.extraction.server.util

import scala.Serializable
import java.io.{File,PrintWriter}
import org.dbpedia.extraction.util.Language
import scala.collection.mutable
import java.io._
import scala.io.Source

/**
 * Contains the ignored templates and properties
 */
class IgnoreList(file: File)
{
  private val templates = new mutable.HashSet[String]
  private val properties = new mutable.HashMap[String, mutable.Set[String]]

  val source = Source.fromFile(file, "UTF-8")
  try {
    
  } 
  finally source.close
    
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

  private def save() {
    val out = new FileOutputStream(file)
    try {
      val writer = new OutputStreamWriter(out, "UTF-8")
      writer.write("templates|"+templates.size+'\n')
      for (template <- templates) {
        writer.write(template+'\n')
      }
      writer.write('\n')
      
      writer.write("properties|"+properties.size+'\n')
      
      writer.write('\n')
      for ((template, props) <- properties) {
        writer.write("template|"+template+"\n")
        writer.write("properties|"+props.size+"\n")
        for (prop <- props) {
          writer.write(prop+'\n')
        }
        writer.write('\n')
      }
      writer.write('\n')
      
      writer.close
    } 
    finally out.close
  }
  
}
package org.dbpedia.extraction.server.util

import scala.Serializable
import java.io.{File,PrintWriter}
import org.dbpedia.extraction.util.Language

/**
 * Contains the ignored templates and properties
 * TODO: write proper text files, read text files, drop serialization
 * TODO: then switch to mutable collections 
 */
class IgnoreList(language : Language) extends Serializable
{
    /** for backwards compatibility */
    private val serialVersionUID = -8153347725542465170L
  
    private var templates = Set[String]()
    private var properties = Map[String, Set[String]]()

    def isTemplateIgnored(template: String) : Boolean = synchronized {
      templates.contains(template)
    }

    def isPropertyIgnored(template: String, property: String) : Boolean = synchronized {
      properties.contains(template) && properties(template).contains(property)
    }

    def addTemplate(template: String) : Unit = synchronized {
        templates += template
    }

    def removeTemplate(template: String) : Unit = synchronized {
        templates -= template
    }

    def addProperty(template: String, property: String) : Unit = synchronized {
        if (properties.contains(template)) {
            var ignoredProps: Set[String] = properties(template)
            if (! ignoredProps.contains(property)) {
                ignoredProps += property
                properties = properties.updated(template, ignoredProps)
            }
        }
        else
        {
            val ignoredProps: Set[String] = Set(property)
            properties = properties.updated(template, ignoredProps)
        }
    }

    def removeProperty(template: String, property: String) {
        if (properties.contains(template)) {
            var ignoredProps: Set[String] = properties(template)
            if (ignoredProps.contains(property))
            {
                ignoredProps -= property
                properties = properties.updated(template, ignoredProps)
            }
            else throw new IndexOutOfBoundsException(property + " not found in " + template)
        }
        else throw new IndexOutOfBoundsException(template + " not found in the ignored properties map.")
    }

    def exportToTextFile(ignoreListTemplatesFile : File, ignoreListPropertiesFile : File)
    {
        printToFile(ignoreListTemplatesFile) { writer =>
          for (template <- templates) writer.println(template)
        }
        
        printToFile(ignoreListPropertiesFile) { writer =>
          for ((template, templateProperties) <- properties) {
            writer.println(template)
            for (templateProperty <- templateProperties) {
              writer.println(templateProperty)
            }
            writer.println()
          }
        }
    }

    def printToFile(file: File)(use: PrintWriter => Unit) {
        val writer = new PrintWriter(file, "UTF-8")
        try use(writer) finally writer.close
    }

}
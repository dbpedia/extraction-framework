package org.dbpedia.extraction.server.util

import scala.Serializable
import java.io.File

/**
 * Contains the ignored templates and properties
 */
class IgnoreList() extends Serializable
{

    var templates = Set[String]()
    var properties = Map[String, Set[String]]()

    def isTemplateIgnored(template: String) =
    {
        if (templates.contains(template)) true else false
    }

    def isPropertyIgnored(template: String, property: String) =
    {
        if (properties.contains(template))
        {
            if (properties(template).contains(property)) true else false
        }
        else false
    }

    def addTemplate(template: String)
    {
        if (!templates.contains(template)) templates += template
    }

    def removeTemplate(template: String)
    {
        if (templates.contains(template)) templates -= template
    }

    def addProperty(template: String, property: String)
    {
        if (properties.contains(template))
        {
            var ignoredProps: Set[String] = properties(template)
            if (!ignoredProps.contains(property))
            {
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

    def removeProperty(template: String, property: String)
    {
        if (properties.contains(template))
        {
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

    def exportToTextFile()
    {
        printToFile(new File(CreateMappingStats.ignoreListTemplatesFileName))(p =>
        {
            templates.foreach(p.println(_))
        })

        printToFile(new File(CreateMappingStats.ignoreListPropertiesFileName))(p =>
        {
            properties.foreach(p.println(_))
        })

    }

    def printToFile(f: java.io.File)(op: java.io.PrintWriter => Unit)
    {
        val p = new java.io.PrintWriter(f)
        try
        {
            op(p)
        } finally
        {
            p.close()
        }
    }

}
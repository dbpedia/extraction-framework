package org.dbpedia.extraction.server.util

import java.util.logging.Logger
import io.Source
import java.lang.IllegalArgumentException
import org.dbpedia.extraction.wikiparser.impl.wikipedia.Namespaces
import org.dbpedia.extraction.wikiparser._
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.util.{WikiUtil, Language}
import scala.Serializable
import scala.collection
import scala.collection.mutable
import java.io._
import org.dbpedia.extraction.ontology.OntologyNamespaces
import org.dbpedia.extraction.destinations.{DBpediaDatasets,Dataset}
import org.dbpedia.extraction.server.util.CreateMappingStats._
import java.net.{URLDecoder, URLEncoder}

/**
 * Contains the statistic of a Template
 */
class MappingStats(templateStats: TemplateStats, val templateName: String, ignoreList: IgnoreList) extends Ordered[MappingStats]
{
    var templateCount = templateStats.templateCount
    var isMapped: Boolean = false
    var properties: mutable.Map[String, (Int, Boolean)] = templateStats.properties.map{case (name, freq) => (name -> (freq, false))}

    def setTemplateMapped(mapped: Boolean)
    {
        isMapped = mapped
    }

    def setPropertyMapped(propertyName: String, mapped: Boolean)
    {
        val (freq, _) = properties.getOrElse(propertyName, (-1, false)) // -1 mapped but not allowed in the template
        properties(propertyName) = (freq, mapped)
    }

    def getNumberOfProperties =
    {
        var counter: Int = 0
        for ((propName, _) <- templateStats.properties)
        {
            if (!ignoreList.isPropertyIgnored(templateName, propName))
            {
                counter = counter + 1
            }
        }
        counter
    }

    def getNumberOfMappedProperties =
    {
        var numMPs: Int = 0
        for ((propName, (propCount, propIsMapped)) <- properties)
        {
            if (propIsMapped && propCount != -1 && !ignoreList.isPropertyIgnored(templateName, propName)) numMPs = numMPs + 1
        }
        numMPs
    }

    def getRatioOfMappedProperties =
    {
        var mappedRatio: Double = 0
        mappedRatio = getNumberOfMappedProperties.toDouble / getNumberOfProperties.toDouble
        mappedRatio
    }

    def getNumberOfPropertyOccurrences =
    {
        var numPOs: Int = 0
        for ((propName, (propCount, propIsMapped)) <- properties)
        {
            if (propCount != -1 && !ignoreList.isPropertyIgnored(templateName, propName)) numPOs = numPOs + propCount
        }
        numPOs
    }

    def getNumberOfMappedPropertyOccurrences =
    {
        var numMPOs: Int = 0
        for ((propName, (propCount, propIsMapped)) <- properties)
        {
            if (propIsMapped && propCount != -1 && !ignoreList.isPropertyIgnored(templateName, propName)) numMPOs = numMPOs + propCount
        }
        numMPOs
    }

    def getRatioOfMappedPropertyOccurrences =
    {
        var mappedRatio: Double = 0
        mappedRatio = getNumberOfMappedPropertyOccurrences.toDouble / getNumberOfPropertyOccurrences.toDouble
        mappedRatio
    }

    def compare(that: MappingStats) =
        this.templateCount.compare(that.templateCount)
}

/**
 * Hold template statistics
 * TODO: comment
 * @param templateCount apparently the number of pages that use the template. a page that uses
 * the template multiple times is counted only once.
 */
class TemplateStats
{
    var templateCount = 0
    val properties = new mutable.HashMap[String, Int]
    
    override def toString = "TemplateStats[count:" + templateCount + ",properties:" + properties.mkString(",") + "]"
}

object WikipediaStatsFormat {
    val WikiStatsTag = "wikiStats|"
    val RedirectsTag = "redirects|"
    val RedirectTag = "r|" // tag is used ~10^6 times - use short tag to save space
    val TemplatesTag = "templates|"
    val TemplateTag = "template|"
    val CountTag = "count|"
    val PropertiesTag = "properties|"
    val PropertyTag = "p|" // tag is used ~10^5 times - use short tag to save space
}

import WikipediaStatsFormat._

// Hold template redirects and template statistics
class WikipediaStats(val language : Language, val redirects: mutable.Map[String, String], val templates: mutable.Map[String, TemplateStats])
{
    def checkForRedirects(mappingStats: Map[MappingStats, Int], mappings: Map[String, ClassMapping]) =
    {
        val templateNamespacePrefix = Namespace.Template.getName(language) + ":"
        val mappedRedirects = redirects.filterKeys(title => mappings.contains(title))
        mappedRedirects.map(_.swap)
    }
    
    def write(writer : Writer) : Unit = {
      
        writer.write(WikiStatsTag)
        writer.write(language.wikiCode)
        writer.write('\n')
        
        writer.write('\n')
        
        writer.write(RedirectsTag)
        writer.write(redirects.size.toString)
        writer.write('\n')
        for ((from, to) <- redirects) { 
            writer.write(RedirectTag)
            writer.write(from)
            writer.write('|')
            writer.write(to)
            writer.write('\n')
        }
        
        writer.write('\n')
        
        // don't save templates that are rarely used to keep stats files at a manageable size
        val minCount = 0 // if (language.wikiCode == "en") 100 else 50

        writer.write(TemplatesTag)
        writer.write(templates.count(_._2.templateCount >= minCount).toString)
        writer.write('\n')
        writer.write('\n')
        for ((name, stats) <- templates) {
            if (stats.templateCount >= minCount) {
                writer.write(TemplateTag)
                writer.write(name)
                writer.write('\n')
                writer.write(CountTag)
                writer.write(stats.templateCount.toString)
                writer.write('\n')
                writer.write(PropertiesTag)
                writer.write(stats.properties.size.toString)
                writer.write('\n')
                for ((property,count) <- stats.properties) {
                  writer.write(PropertyTag)
                  writer.write(property)
                  writer.write('|')
                  writer.write(count.toString)
                  writer.write('\n')
                }
                writer.write('\n')
            }
        }
        writer.write('\n')
    }
}

class WikipediaStatsReader(lines : Iterator[String])
{
    def read() : WikipediaStats = {
      
        var line = ""
        
        val language = Language(readTag(WikiStatsTag))
        readEmpty()
        
        val redirCount = readCount(RedirectsTag)
        val redirects = new mutable.HashMap[String, String]()
        var redirIndex = 0
        while(redirIndex < redirCount) {
            redirects += readRedirect
            redirIndex += 1
        }
        readEmpty()
        
        val templateCount = readCount(TemplatesTag)
        readEmpty()
        val templates = new mutable.HashMap[String, TemplateStats]()
        var templateIndex = 0
        while(templateIndex < templateCount) {
            val templateStats = new TemplateStats
            templates(readTag(TemplateTag)) = templateStats
            templateStats.templateCount = readCount(CountTag)
            val propCount = readCount(PropertiesTag)
            var propIndex = 0
            while(propIndex < propCount) {
                templateStats.properties += readProperty
                propIndex += 1
            }
            templateIndex += 1
            readEmpty()
        }
        readEmpty()
        
        new WikipediaStats(language, redirects, templates)
    }
    
    private def readTag(tag: String) : String = {
        if (! lines.hasNext) throw new Exception("missing line starting with '"+tag+"'")
        val line = lines.next
        if (! line.startsWith(tag)) throw new Exception("expected line starting with '"+tag+"', found '"+line+"'")
        line.substring(tag.length)
    }
    
    private def readCount(tag: String) : Int = {
        readTag(tag).toInt
    }
    
    private def readRedirect : (String, String) = {
        val tail = readTag(RedirectTag)
        val pair = tail.split("\\|", -1)
        if (pair.length == 2) {
          return (pair(0), pair(1))
        }
        throw new Exception("expected line starting with '"+RedirectTag+"' followed by two strings, found '"+RedirectTag+tail+"'")
    }
    
    private def readProperty : (String, Int) = {
        val tail = readTag(PropertyTag)
        val pair = tail.split("\\|", -1)
        if (pair.length == 2) {
          try { return (pair(0), pair(1).toInt) }
          catch { case nfe : NumberFormatException => /* fall through, throw below */ }
        }
        throw new Exception("expected line starting with '"+PropertyTag+"' followed by two strings, found '"+PropertyTag+tail+"'")
    }
    
    private def readEmpty() : Unit = {
        if (! lines.hasNext) throw new Exception("missing empty line")
        val line = lines.next
        if (line.nonEmpty) throw new Exception("expected empty line, found '"+line+"'")
    }
}

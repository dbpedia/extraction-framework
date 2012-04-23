package org.dbpedia.extraction.server.util

import scala.collection.mutable

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

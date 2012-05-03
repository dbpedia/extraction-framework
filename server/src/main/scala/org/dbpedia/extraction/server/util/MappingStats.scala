package org.dbpedia.extraction.server.util

import MappingStats.InvalidTarget

/**
 * Contains the statistic of a Template
 */

object MappingStats {
  
  /**
   * Signals that a template or property is mapped but not used in Wikipedia.
   * Currently only used for properties. TODO: also use for templates.
   * We use Int.MaxValue because that means that invalid mappings or sorted first.
   */
  val InvalidTarget = Int.MaxValue
}

class MappingStats(templateStats: TemplateStats, val templateName: String, val isMapped: Boolean, val properties: Map[String, (Int, Boolean)], ignoreList: IgnoreList)
{
  val templateCount = templateStats.templateCount

  /**
   * @param all count all properties or only the ones that are mapped
   * @param use count property use in wikipedia articles or just the definition in template
   */
  private def count(all: Boolean, use: Boolean) = {
    var sum = 0
    for ((name, (count, mapped)) <- properties) {
      if (all || mapped) {
        if (count != InvalidTarget && ! ignoreList.isPropertyIgnored(templateName, name)) {
          sum += (if (use) count else 1)
        }
      }
    }
    sum
  }

  val propertyCount = count(true, false)
  val mappedPropertyCount = count(false, false) 

  val propertyUseCount = count(true, true)
  val mappedPropertyUseCount = count(false, true) 

  val mappedPropertyRatio = mappedPropertyCount.toDouble / propertyCount.toDouble
  val mappedPropertyUseRatio = mappedPropertyUseCount.toDouble / propertyUseCount.toDouble
}

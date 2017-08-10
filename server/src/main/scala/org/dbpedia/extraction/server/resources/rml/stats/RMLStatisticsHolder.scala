package org.dbpedia.extraction.server.resources.rml.stats

import org.dbpedia.extraction.mappings.rml.model.RMLModel
import org.dbpedia.extraction.server.resources.rml.MappingsTrackerRepo


/**
  * Created by wmaroy on 10.08.17.
  *
  * An instance of this class keeps track of statistics for RML Mappings
  * An instance of this class is created through apply
  *
  */
class RMLStatisticsHolder(languagesStats: Map[String, RMLLanguageStats]) {

  def apply(key : String) : RMLLanguageStats = {
    languagesStats(key)
  }

  def updated(mappings : Map[String, Set[String]]) : RMLStatisticsHolder = {
    null
  }

}

object RMLStatisticsHolder {

  /**
    * Creates new RMLStatisticsHolder.
    * This calculates statistics for all mappings in the mappings-tracker repository.
    *
    * @return
    */
  def apply() : RMLStatisticsHolder = {
    val languagesStats = getLanguagesStats
    new RMLStatisticsHolder(languagesStats)
  }

  /**
    *
    * Calculates for every language the statistics of every mapping.
    * This checks every language folder in the mappings-tracker repository.
    *
    * @return
    */
  private def getLanguagesStats : Map[String, RMLLanguageStats] = {

    // load all mapping models per language
    val languageModels = MappingsTrackerRepo.getLanguageRMLModels

    // iterate over each language
    languageModels.map(entry => {

      val language = entry._1
      val rmlModels = entry._2

      // iterate over each mapping in current language
      val mappingStats = rmlModels.map(rmlModel => {

        val name = rmlModel._1
        val mappedProperties = rmlModel._2.getMappedProperties

        // retrieve stats of current mapping
        name -> RMLMappingStats(mappedProperties)
      })

      // retreive stats of current language
      language -> RMLLanguageStats(mappingStats)
    })
  }

}

case class RMLMappingStats(val mappedProperties : List[String])

case class RMLLanguageStats(mappingStats : Map[String, RMLMappingStats]) {

  def apply(key: String): RMLMappingStats = {
    mappingStats(key)
  }

}

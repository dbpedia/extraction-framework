package org.dbpedia.extraction.server.resources.rml.stats

import java.util.logging.Logger

import org.dbpedia.extraction.server.resources.rml.MappingsTrackerRepo


/**
  * Created by wmaroy on 10.08.17.
  *
  * An instance of this class keeps track of statistics for RML Mappings
  * An instance of this class is created through apply
  *
  */
class RMLStatisticsHolder(languagesStats: Map[String, RMLLanguageStats]) {

  private val logger = Logger.getLogger(RMLStatisticsHolder.getClass.getName)

  def apply(key : String) : RMLLanguageStats = {
    languagesStats(key)
  }

  /**
    * Updates the RMLStatisticsHolder by only updating the statistics of the given mappigns
    * @param mappings A map with keyset the languages and as value a set of mapping names
    * @return
    */
  def updated(mappings : Map[String, Set[String]]) : RMLStatisticsHolder = {

    val updatedMappingsPerLanguage = MappingsTrackerRepo.getLanguageRMLModels(mappings)

    val updatedLanguageStats = updatedMappingsPerLanguage.foldLeft(languagesStats)((languageStats, update) => {


      val language = update._1
      val changedMappings = update._2
      val mappingStats = languageStats(language).mappingStats

      logger.info("Updating RML Statistics for language " + language)

      val updatedMappingStats = changedMappings.foldLeft(mappingStats)((mappingStats, update) => {
        val name = update._1

        logger.info("Updating RML Statistics for " + name)

        val rmlModel = update._2
        val mappingStat = RMLMappingStats(rmlModel.getMappedProperties)

        if(mappingStats.contains(name)) {
          val from = mappingStats(name).mappedProperties.size
          val to = mappingStat.mappedProperties.size
          logger.info("Mapped properties updated from " + from + " to " +  to)
        }

        mappingStats.updated(name, mappingStat)
      })

      languageStats.updated(language, RMLLanguageStats(updatedMappingStats))

    })

    new RMLStatisticsHolder(updatedLanguageStats)

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
    val languageModels = MappingsTrackerRepo.getLanguageRMLModels()

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

case class RMLMappingStats(mappedProperties : List[String])

case class RMLLanguageStats(mappingStats : Map[String, RMLMappingStats]) {

  def apply(key: String): RMLMappingStats = {
    mappingStats(key)
  }

}

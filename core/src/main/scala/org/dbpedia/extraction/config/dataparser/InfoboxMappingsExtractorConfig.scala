package org.dbpedia.extraction.config.dataparser

/**
  * Created by aditya on 6/4/16.
  */
object InfoboxMappingsExtractorConfig {

  val infoboxNameMap = Map(
    "en" -> "Infobox",
    "no" -> "Infoboks"

  )

  val directTemplateMapsToWikidata = Map(
    "en" -> Map("Official website" -> "P856", "Official URL" -> "P856"),
    "no" -> Map("BetingetURL" -> "P856")
  )
}

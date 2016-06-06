package org.dbpedia.extraction.config.dataparser

/**
  * Created by aditya on 6/4/16.
  */
object InfoboxMappingsExtractorConfig {

  val infoboxNameMap = Map(
    "en" -> "Infobox",
    "no" -> "Infoboks"

  )

  val KeyWordsMapForP856 = Map(
    "en" -> Map("Official website" -> "1", "Official URL" -> "2"),
    "no" -> Map("BetingetURL" -> "1")
  )
}

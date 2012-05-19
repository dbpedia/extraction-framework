package org.dbpedia.extraction.config.mappings


object DisambiguationExtractorConfig
{
    val disambiguationTitlePartMap = Map(
         "ca" -> " (desambiguació)",
         "de" -> " (Begriffsklärung)",
         "el" -> " (αποσαφήνιση)",
         "en" -> " (disambiguation)",
         "es" -> " (desambiguación)",
         "eu" -> " (argipena)",
         "it" -> " (disambigua)",
         "ko" -> " (동음이의)",
         "pl" -> " (ujednoznacznienie)",
         "pt" -> " (desambiguação)",
         "ru" -> " (значения)",
         "fr" -> " (homonymie)"
    )

    val supportedLanguages = disambiguationTitlePartMap.keySet
}

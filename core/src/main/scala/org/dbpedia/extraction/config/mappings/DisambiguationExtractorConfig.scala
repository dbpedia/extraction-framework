package org.dbpedia.extraction.config.mappings


object DisambiguationExtractorConfig
{
    val supportedLanguages = disambiguationTitlePartMap.keySet

    val disambiguationTitlePartMap = Map(
         "ca" -> " (desambiguació)",
         "de" -> " (Begriffsklärung)",
         "el" -> " (αποσαφήνιση)",
         "en" -> " (disambiguation)",
         "es" -> " (desambiguación)",
         "it" -> " (disambigua)",
         "pl" -> " (ujednoznacznienie)",
         "pt" -> " (desambiguação)",
         "ru" -> " (значения)"
    )
}
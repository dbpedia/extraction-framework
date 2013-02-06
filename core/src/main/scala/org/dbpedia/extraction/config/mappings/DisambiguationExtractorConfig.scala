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
         "fr" -> " (homonymie)",
         "it" -> " (disambigua)",
         "ko" -> " (동음이의)",
         "nl" -> " (doorverwijspagina)", //TODO make it Set() for multiple “nl” -> " (disambigueren)"
         "pl" -> " (ujednoznacznienie)",
         "pt" -> " (desambiguação)",
         "ru" -> " (значения)"
    )
}

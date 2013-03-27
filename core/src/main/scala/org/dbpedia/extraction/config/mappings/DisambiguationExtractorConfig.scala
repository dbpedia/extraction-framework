package org.dbpedia.extraction.config.mappings


object DisambiguationExtractorConfig
{
    // For "ar" configuration, rendering right-to-left may seems like a bug, but it's not.
    // Don't change this else if you know how it is done.
    val disambiguationTitlePartMap = Map(
         "ar" -> " (توضيح)",
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

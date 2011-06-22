package org.dbpedia.extraction.config.mappings


object InterLanguageLinksExtractorConfig
{
    val supportedLanguages = intLinksMap.keySet

    val intLinksMap = Map(
        "de" -> Set("en", "el"),
        "el" -> Set("en", "de", "ru"),
        "en" -> Set("el", "de", "co"),
        "ru" -> Set("en", "el", "de")
    )
}
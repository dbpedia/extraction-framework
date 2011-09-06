package org.dbpedia.extraction.config.mappings


object InterLanguageLinksExtractorConfig
{
    val intLinksMap = Map(
        "de" -> Set("en", "el"),
        "el" -> Set("en", "de", "ru"),
        "en" -> Set("el", "de", "co"),
        "ru" -> Set("en", "el", "de")
    )

    val supportedLanguages = intLinksMap.keySet
}
package org.dbpedia.extraction.config.mappings


object InterLanguageLinksExtractorConfig
{
    val intLinksMap = Map(
        "de" -> Set("en", "el"),
        "el" -> Set("en", "de", "ru", "pt"),
        "it" -> Set("en", "de", "el"),
        "en" -> Set("co", "el", "de", "pt", "ru"),
        "ru" -> Set("en", "el", "de", "pt")
    )

    val supportedLanguages = intLinksMap.keySet
}

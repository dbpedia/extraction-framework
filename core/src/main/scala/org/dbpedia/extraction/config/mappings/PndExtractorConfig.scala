package org.dbpedia.extraction.config.mappings


object PndExtractorConfig
{
    val supportedLanguages = Set("de", "en", "fr")

    // TODO: use a map with language-specific template names?
    val pndTemplates = Set("normdaten", "pnd", "dnb")
}
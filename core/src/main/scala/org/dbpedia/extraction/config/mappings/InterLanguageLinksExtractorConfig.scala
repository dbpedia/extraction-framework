package org.dbpedia.extraction.config.mappings

/**
 * TODO: What's the point of this? Why don't we just use all languages?
 */
object InterLanguageLinksExtractorConfig
{
    val intLinksMap = Map(
        "de" -> Set("en", "el"),
        "el" -> Set("en", "de", "ru", "pt"),
        "it" -> Set("en", "de", "el"),
        // TODO: the Set for "en" once included "co", but that's Corsican.
        // I think that was a bug and removed it, but what's correct? "ca"? "ko"?
        "en" -> Set("el", "de", "pt", "ru"),
        "ru" -> Set("en", "el", "de", "pt"),
        "fr" -> Set("en", "el", "de", "pt", "ru")
    )
}

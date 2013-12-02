package org.dbpedia.extraction.config.mappings


object DateIntervalMappingConfig
{
    // For "ar" configuration, rendering right-to-left may seems like a bug, but it's not.
    // Don't change this else if you know how it is done.
    val presentMap = Map(
        "ar" -> "الحاضر",
        "en" -> "present",
        "ga" -> "inniu",
        "id" -> "sekarang",
        "pl" -> "nadal"
    )
}

package org.dbpedia.extraction.config.mappings


object DateIntervalMappingConfig
{
    // For "ar" configuration, rendering right-to-left may seems like a bug, but it's not.
    // Don't change this else if you know how it is done.
    val presentMap = Map(
        "en" -> Set("present", "now"),
        "ar" -> Set("الحاضر"),
        "el" -> Set("Παρόν"),
        "es" -> Set("presente", "actualidad", "fecha"),
        "fr" -> Set("aujourd'hui", "en cours"),
        "ga" -> Set("inniu", "actualidade"),
        "id" -> Set("sekarang"),
        "it" -> Set("in attività"),
        "nl" -> Set("heden"),
        "pl" -> Set("nadal"),
        "pt" -> Set("presente", "atualidade", "atualmente")
    )

    val sinceMap = Map(
        "en" -> "since",
        "ca" -> "des del",
        "es" -> "desde",
        "fr" -> "depuis",
        "pl" -> "od",
        "pt" -> "desde"
    )

    val splitMap = Map(
        "en" -> "to",
        "es" -> "al|a la|a|hasta (?:el|la)",
        "fr" -> "à|au",
        "pt" -> "a"
    )
}

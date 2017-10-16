package org.dbpedia.extraction.config.mappings


object DateIntervalMappingConfig
{
    val splitPropertyNodeMap = Map (
      "en" -> """<br\s*\/?>|\n|,|;"""
    )
    
    // For "ar" configuration, rendering right-to-left may seems like a bug, but it's not.
    // Don't change this else if you know how it is done.
    val presentMap = Map(
        "en" -> Set("present", "now"),
        "ar" -> Set("الحاضر"),
        "be" -> Set("па гэты дзень", "па сучаснасць"),
        "bg" -> Set("до наши дни", "настояще", "досега"),
        "ca" -> Set("actualitat"),
        "cs" -> Set("současnost"),
        "el" -> Set("Παρόν", "σήμερα"),
        "es" -> Set("presente", "actualidad", "fecha"),
        "eu" -> Set("gaur egun", "gaur egun arte", "egun"),
        "fr" -> Set("aujourd'hui", "en cours"),
        "ga" -> Set("inniu"),
        "hr" -> Set("danas"),
        "hu" -> Set("napjainkig"),
        "id" -> Set("sekarang"),
        "it" -> Set("in attività"),
        "nl" -> Set("heden"),
        "pl" -> Set("nadal", "obecnie"),
        "pt" -> Set("presente", "atualidade", "atualmente"),
        "ru" -> Set("наши дни", "настоящее время", "наст. время", "н.вр."),
        "sk" -> Set("súčasnosť"),
        "sl" -> Set("danes"),
        "tr" -> Set("günümüz", "günümüze", "halen")
    )

    val sinceMap = Map(
        "en" -> "since",
        "de" -> "seit",
        "ca" -> "des del",
        "es" -> "desde",
        "fr" -> "depuis",
        "pl" -> "od",
        "pt" -> "desde"
    )

    val onwardMap = Map(
        "en" -> "onward",
        "es" -> "en adelante"
    )

    val splitMap = Map(
        "en" -> "to",
        "es" -> "al|a la|a|hasta (?:el|la)",
        "fr" -> "à|au",
        "pl" -> "do",
        "pt" -> "a"
    )
}

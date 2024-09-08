package org.dbpedia.extraction.config.mappings


object DateIntervalMappingConfig
{
    val splitPropertyNodeMap = Map (
      "en" -> """<br\s*\/?>|\n|,|;"""
    )
    
    // For "ar" configuration, rendering right-to-left may seems like a bug, but it's not.
    // Don't change this unless you know how it works.
    val presentMap = Map(
        "en" -> Set("present", "now"), // for example see https://en.wikipedia.org/wiki/Donald_Trump -> Political party -> Republican (1987–1999, 2009–2011, 2012–present)
        "am" -> Set("አሁን", "እስካሁን", "እስካሁን ድረስ"),
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
        "mk" -> Set("денес"),
        "nl" -> Set("heden"),
        "pl" -> Set("nadal", "obecnie"),
        "pt" -> Set("presente", "atualidade", "atualmente","agora"),
        "ru" -> Set("наши дни", "настоящее время", "наст. время", "н.вр."),
        "sk" -> Set("súčasnosť"),
        "sl" -> Set("danes"),
        "tr" -> Set("günümüz", "günümüze", "halen"),
        "uk" -> Set("зараз", "в даний момент часу", "в нинішні дні")
    )

    val sinceMap = Map(
        "en" -> "since",
        "am" -> "(?:ጀምሮ|አንሥቶ|አንስቶ|ከ)",
        "ca" -> "des del",
        "es" -> "desde",
        "fr" -> "depuis",
        "pl" -> "od",
        "pt" -> "desde",
        "uk" -> "від"
    )

    val onwardMap = Map(
        "en" -> "onward",
        "am" -> "በኋላ",
        "es" -> "en adelante",
        "pt" -> "adiante|avante"
    )

    val splitMap = Map(
        "en" -> "to",
        "am" -> "እስከ",
        "es" -> "al|a la|a|hasta (?:el|la)",
        "fr" -> "à|au",
        "pl" -> "do",
        "pt" -> "a"
    )
}

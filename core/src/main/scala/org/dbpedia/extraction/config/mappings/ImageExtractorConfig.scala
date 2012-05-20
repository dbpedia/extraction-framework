package org.dbpedia.extraction.config.mappings

/**
 * See page https://meta.wikimedia.org/wiki/Non-free_content
 */
object ImageExtractorConfig
{
    val wikipediaUrlPrefix = "http://upload.wikimedia.org/wikipedia/"

    val NonFreeRegex = Map(
           "de" -> """(?iu)\{\{\s?(Dateiüberprüfung/benachrichtigt_\(Kategorie\)|Geschützt|Geschützt-Ungeklärt|Bild-LogoSH|Bild-PD-alt-100|Bild-PD-alt-1923|Bild-WikimediaCopyright)\s?\}\}""".r ,
           "el" -> """(?iu)\{\{\s?(εύλογη χρήση|σήμα|σήμα αθλητικού σωματείου|αφίσα ταινίας|σκηνή από ταινία|γραφικά υπολογιστή|εξώφυλλο άλμπουμ|εξώφυλλο βιβλίου|μη ελεύθερο έργο τέχνης|σελίδα κόμικς|σελίδα εφημερίδας|εικόνα-βιντεοπαιχνίδι|ιδιοκτησία Wikimedia)\s?\}\}""".r ,
           "en" -> """(?i)\{\{\s?non-free""".r,
           "es" -> """(?iu)\{\{\s?(CopyrightByWikimedia|Copyvio|Logo|Screenshot|PD-CAGov|Fairuse|Noncommercial|Nonderivative|NZCrownCopyright|PolandGov|PD-IndiaGov|ADRM2)\s?\}\}""".r,
           "it" -> """(?iu)\{\{\s?(Sconosciuto|Riservato|NonCommerciale|Unknown|Noncommercial|Nonderivative|Copyrighted|Screenshot|Ordinance Survey Copyright|Fairuse|Cc-nc|cc-by-nc|cc-by-nc-2.0|cc-nc-sa|cc-by-nc-sa|Cc-by-nc-sa-1.0|cc-by-nc-sa-2.0|cc-nd-nc|cc-by-nd-nc|cc-by-nd-nc-2.0|cc-nd|cc-by-nd|cc-by-nd-2.0|TW-cc-by-nc-nd-2.0|TW-cc-by-nc-sa-2.0|Copyright by Wikimedia|CopyrightbyWikimedia)\s?\}\}""".r,
           "pt" -> """(?iu)\{\{\s?(Unknown|Noncommercial|Nonderivative|Copyrighted|Screenshot|Ordnance Survey Copyright|Fairuse|Cc-nc|cc-by-nc|cc-by-nc-2.0|cc-nc-sa|cc-by-nc-sa|Cc-by-nc-sa-1.0|cc-by-nc-sa-2.0|cc-nd-nc|cc-by-nd-nc|cc-by-nd-nc-2.0|cc-nd|cc-by-nd|cc-by-nd-2.0|TW-cc-by-nc-nd-2.0|TW-cc-by-nc-sa-2.0|Copyright by Wikimedia|CopyrightbyWikimedia)\s?\}\}""".r,
           "ru" -> """(?iu)\{\{\s?(CopyrightByWikimedia|Fairuse|несвободный файл|несвободная лицензия|запрещенная лицензия)\s?\}\}""".r ,
           "fr" -> """(?iu)\{\{\s?(Copyright by Wikimedia|Copyvio|Logo|Screenshot|Ordnance Survey Copyright|Fairuse|Noncommercial|PolandGov|nonderivative|NZCrownCopyright|PD-IndiaGov|ADRM2|Marque déposée)\s?\}\}""".r
    )

    val supportedLanguages = NonFreeRegex.keySet

    val ImageRegex = """(?i)[^\"/\*?<>|:]+\.(?:jpe?g|png|gif|svg)""".r

    val ImageLinkRegex = """(?i).*\.(?:jpe?g|png|gif|svg)""".r
}

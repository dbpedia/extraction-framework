package org.dbpedia.extraction.config.mappings

/**
 * See page https://meta.wikimedia.org/wiki/Non-free_content
 */
object ImageExtractorConfig
{
    val wikipediaUrlPrefix = "http://upload.wikimedia.org/wikipedia/"

    // "NonFreeRegex" holds a regex with the templates for each Wikipedia language edition the state non-free license.
    // This is done in order to exclude these images completely from the extraction process and use only open-licensed images
    // Note again that this rule is only on  what to exclude (NOT include)
    // For "ar" configuration, rendering right-to-left may seems like a bug, but it's not.
    // Don't change this else if you know how it is done.
    val NonFreeRegex = Map(
           "ar" -> """(?i)\{\{\s?غير حر""".r,
           "de" -> """(?iu)\{\{\s?(Dateiüberprüfung/benachrichtigt_\(Kategorie\)|Geschützt|Geschützt-Ungeklärt|Bild-LogoSH|Bild-PD-alt-100|Bild-PD-alt-1923|Bild-WikimediaCopyright)\s?\}\}""".r ,
           "el" -> """(?iu)\{\{\s?(εύλογη χρήση|σήμα|σήμα αθλητικού σωματείου|αφίσα ταινίας|σκηνή από ταινία|γραφικά υπολογιστή|εξώφυλλο άλμπουμ|εξώφυλλο βιβλίου|μη ελεύθερο έργο τέχνης|σελίδα κόμικς|σελίδα εφημερίδας|εικόνα-βιντεοπαιχνίδι|ιδιοκτησία Wikimedia)\s?\}\}""".r ,
           "en" -> """(?i)\{\{\s?non-free""".r,
           "es" -> """(?iu)\{\{\s?(CopyrightByWikimedia|Copyvio|Logo|Screenshot|PD-CAGov|Fairuse|Noncommercial|Nonderivative|NZCrownCopyright|PolandGov|PD-IndiaGov|ADRM2)\s?\}\}""".r,
           "eu" -> """(?i)\{\{\s?(Cc-by-nc-sa-2.5|Wikimedia_logoa|Copyrightdun_logoa|Lizentzia_gabea|Album_azala|Aldizkari_azala|Fair_use|Bideo-zinta_azala|Dirua|DVD_azala|Egunkari_azala|Film_pantaila_irudia|Film_posterra|HQFL_logotipoa|Ikonoa|Ikurra|Irrati_logotipoa|Jatetxe_logotipoa|Joku_azala|Joku_pantaila_irudia|Kirol_logotipoa|Komiki_azala|Liburu_azala|Logotipoa|Mahai-joku_azala|Olinpiada_logotipoa|Politika_posterra|Propaganda|Software_azala|Software_pantaila_irudia|Zigilua|TB_pantaila_irudia|Web_pantaila_irudia)\s?\}\}""".r,
           "fr" -> """(?iu)\{\{\s?(Copyright by Wikimedia|Copyvio|Logo|Screenshot|Ordnance Survey Copyright|Fairuse|Noncommercial|PolandGov|nonderivative|NZCrownCopyright|PD-IndiaGov|ADRM2|Marque déposée)\s?\}\}""".r,
           "it" -> """(?iu)\{\{\s?(Sconosciuto|Riservato|NonCommerciale|Unknown|Noncommercial|Nonderivative|Copyrighted|Screenshot|Ordinance Survey Copyright|Fairuse|Cc-nc|cc-by-nc|cc-by-nc-2.0|cc-nc-sa|cc-by-nc-sa|Cc-by-nc-sa-1.0|cc-by-nc-sa-2.0|cc-nd-nc|cc-by-nd-nc|cc-by-nd-nc-2.0|cc-nd|cc-by-nd|cc-by-nd-2.0|TW-cc-by-nc-nd-2.0|TW-cc-by-nc-sa-2.0|Copyright by Wikimedia|CopyrightbyWikimedia)\s?\}\}""".r,
           "nl" -> """(?i)\{\{\s?(Copyright by Wikimedia)\s?\}\}""".r,
           "pl" -> """(?iu)\{\{\s?(Copyright by Wikimedia|brak licencji|brak źródła|brak autora|brak pozwolenia|SWMPL|Zgoda PWM)\s?\}\}""".r,
           "pt" -> """(?iu)\{\{\s?(Unknown|Noncommercial|Nonderivative|Copyrighted|Screenshot|Ordnance Survey Copyright|Fairuse|Cc-nc|cc-by-nc|cc-by-nc-2.0|cc-nc-sa|cc-by-nc-sa|Cc-by-nc-sa-1.0|cc-by-nc-sa-2.0|cc-nd-nc|cc-by-nd-nc|cc-by-nd-nc-2.0|cc-nd|cc-by-nd|cc-by-nd-2.0|TW-cc-by-nc-nd-2.0|TW-cc-by-nc-sa-2.0|Copyright by Wikimedia|CopyrightbyWikimedia)\s?\}\}""".r,
           "ru" -> """(?iu)\{\{\s?(CopyrightByWikimedia|Fairuse|несвободный файл|несвободная лицензия|запрещенная лицензия)\s?\}\}""".r
    )

    val supportedLanguages = NonFreeRegex.keySet

    val ImageRegex = """(?i)[^\"/\*?<>|:]+\.(?:jpe?g|png|gif|svg)""".r

    val ImageLinkRegex = """(?i).*\.(?:jpe?g|png|gif|svg)""".r
}

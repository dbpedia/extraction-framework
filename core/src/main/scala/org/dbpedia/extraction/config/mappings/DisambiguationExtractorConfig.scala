package org.dbpedia.extraction.config.mappings


object DisambiguationExtractorConfig
{
    // For "ar" and "he" configurations, rendering right-to-left may seem like a bug, but it's not.
    // Don't change this unless you know what you're doing.
    val disambiguationTitlePartMap = Map(
         "am" -> " (መንታ)",
         "ar" -> " (توضيح)",
         "bg" -> " (пояснение)",
         "ca" -> " (desambiguació)",
         "cs" -> " (rozcestník)",
         "de" -> " (Begriffsklärung)",
         "el" -> " (αποσαφήνιση)",
         "en" -> " (disambiguation)",
         "eo" -> " (apartigilo)",
         "es" -> " (desambiguación)",
         "eu" -> " (argipena)",
         "ext" -> " (desambiguáncia)",
         "fr" -> " (homonymie)",
         "ga" -> " (idirdhealáin)",
         "gl" -> " (homónimos)",
         "he" -> " (פירושונים)",
         "hi" -> " (बहुविकल्पी)", // eg. https://hi.wikipedia.org/wiki/आयरलैण्ड_(बहुविकल्पी)
         "hu" -> " (egyértelműsítő lap)",
         "id" -> " (disambig)",
         "it" -> " (disambigua)",
         "ja" -> " (曖昧さ回避)",
         "ko" -> " (동음이의)",
         "mk" -> " (појаснување)",
         "nl" -> " (doorverwijspagina)", //TODO make it Set() for multiple “nl” -> " (disambigueren)"
         "pl" -> " (ujednoznacznienie)",
         "ro" -> " (dezambiguizare)", // eg. https://ro.wikipedia.org/wiki/Atena_(dezambiguizare)
         "pt" -> " (desambiguação)",
         "ru" -> " (значения)",
         "sk" -> " (Rozlišovacia stránka)",
         "sr" -> " (Višeznačna odrednica)",  //TODO make it a Set() for multiple “sr” -> " (вишезначна одредница)"
         "uk" -> " (значення)"
    )
}

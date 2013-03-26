package org.dbpedia.extraction.config.mappings


object InfoboxExtractorConfig
{
    val ignoreTemplates = Set("redirect", "seealso", "see_also", "main", "cquote", "chess diagram", "ipa", "lang", "تحويل")

    val ignoreTemplatesRegex = List("cite.*".r, "citation.*".r, "assessment.*".r, "zh-.*".r, "llang.*".r, "IPA-.*".r, "اقتباس.*".r)

    val ignoreProperties = Map (
        "en"-> Set("image", "image_photo"),
        "ar"-> Set("صورة"),
        "el"-> Set("εικόνα", "εικονα", "Εικόνα", "Εικονα", "χάρτης", "Χάρτης")
    )
    
    // TODO: i18n
    val RankRegex = """(?i)([0-9]+)\s?(?:st|nd|rd|th)""".r

    val SplitWordsRegex = """_+|\s+|\-|:+""".r

    val TrailingNumberRegex = """[0-9]+$""".r
    
    // Template Statistics (not valid triples => do not load / disable in live)
    val extractTemplateStatistics = false 

}

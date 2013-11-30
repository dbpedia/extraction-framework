package org.dbpedia.extraction.config.mappings


object InfoboxExtractorConfig
{
    // For "ar" configuration, rendering right-to-left may seems like a bug, but it's not.
    // Don't change this else if you know how it is done.

    val ignoreTemplates = Set("redirect", "seealso", "see_also", "main", "cquote", "chess diagram", "ipa", "lang", "تحويل")

    val ignoreTemplatesRegex = List("cite.*".r, "citation.*".r, "assessment.*".r, "zh-.*".r, "llang.*".r, "IPA-.*".r, "citajxo.*".r)

    val ignoreProperties = Map (
        "en"-> Set("image", "image_photo"),
        "ar"-> Set("صورة"),
        "id"-> Set("foto", "gambar"),
        "el"-> Set("εικόνα", "εικονα", "Εικόνα", "Εικονα", "χάρτης", "Χάρτης"),
        "eo"-> Set("dosiero")
    )
    
    // TODO: i18n
    val RankRegex = """(?i)([0-9]+)\s?(?:st|nd|rd|th)""".r

    val SplitWordsRegex = """_+|\s+|\-|:+""".r

    val TrailingNumberRegex = """[0-9]+$""".r
    
    // When you generate statistics, set the following to true. To get full coverage, you should
    // probably set most other parameters here to zero or empty values. 
    val extractTemplateStatistics = false 

    val minPropertyCount = 2

    val minRatioOfExplicitPropertyKeys = 0.75
}

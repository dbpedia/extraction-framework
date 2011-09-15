package org.dbpedia.extraction.config.mappings


object TemplateParameterExtractorConfig
{
    val ignoreTemplates = Set("redirect", "seealso", "see_also", "main", "cquote", "chess diagram", "ipa", "lang")
    val ignoreTemplatesRegex = List("cite.*".r, "citation.*".r, "assessment.*".r, "zh-.*".r, "llang.*".r, "IPA-.*".r)
}
package org.dbpedia.extraction.config.dataparser

object DataParserConfig {

  // Default split property node regex
  // TODO no language context in DataParser
  val splitPropertyNodeRegex = Map (
    "en" -> """<br\s*\/?>|\n"""
  )

  val splitPropertyNodeRegexDateTime = Map (
    "en" -> """<br\s*\/?>|\n| and | or |;"""  //TODO this split regex might not be complete
  )

  val splitPropertyNodeRegexDouble   = Map (
    "en" -> """<br\s*\/?>|\n| and | or |;"""  //TODO this split regex might not be complete
  )

  val splitPropertyNodeRegexInteger  = Map (
    "en" -> """<br\s*\/?>|\n| and | or |;"""  //TODO this split regex might not be complete
  )

  // TODO no language context in LinkParser
  val splitPropertyNodeRegexLink     = Map (
    "en" -> """<br\s*\/?>|\n| and | or |/|;|,"""  //TODO this split regex might not be complete
  )

  val splitPropertyNodeRegexObject   = Map (
    "en" -> """<br\s*\/?>|\n| and | or | in |/|;|,"""
  )

  val splitPropertyNodeRegexUnitValue= Map (
    "en" -> """<br\s*\/?>|\n| and | or """  //TODO this split regex might not be complete
  )

  val splitPropertyNodeRegexInfobox  = Map (
    "en" -> """<br\s*\/?>"""
  )

  // Templates used instead of <br>
  val splitPropertyNodeRegexInfoboxTemplates  = Map (
    "fr" -> List("""[C-c]lr""")
  )

  // see https://www.cs.tut.fi/~jkorpela/dashes.html
  val dashVariations = List('-', '־', '‐', '‑', '‒','–', '—', '―', '⁻', '₋', '−', '﹣', '－' ,'—')
  val dashVariationsRegex = dashVariations.mkString("|")

}

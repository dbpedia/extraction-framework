package org.dbpedia.extraction.config.dataparser

object GeoCoordinateParserConfig {
  // Only actually used coordinate template names (based on test coverage)
  val coordTemplateNames = Set(
    "coord",           // Primary template - widely used
    "coor",            // Alternative spelling
    "coordinates",     // Full name variant
    "coordenadas",     // Spanish
    "coordonnées",     // French
    "coordinaten",     // Dutch
    "koordinaten",     // German
    "coordinate"      // German {{Coordinate ...}} Template
  )

  //map latitude letters used in languages to the ones used in English ("E" for East and "W" for West)
  val longitudeLetterMap = Map(
    "af" -> Map("E" -> "E", "W" -> "W", "O" -> "E"),
    "am" -> Map("E" -> "E", "W" -> "W"),
    "ar" -> Map("E" -> "E", "W" -> "W", "شرق" -> "E", "غرب" -> "W"),
    "bg" -> Map("E" -> "E", "W" -> "W", "И" -> "E", "З" -> "W"),
    "cs" -> Map("E" -> "E", "W" -> "W", "V" -> "E", "Z" -> "W"),
    "de" -> Map("E" -> "E", "O" -> "E", "W" -> "W"),
    "en" -> Map("E" -> "E", "W" -> "W"),
    "es" -> Map("E" -> "E", "W" -> "W", "O" -> "W"),
    "fr" -> Map("E" -> "E", "O" -> "E", "W" -> "W"),
    "hi" -> Map("E" -> "E", "W" -> "W", "पू" -> "E", "प" -> "W"),
    "it" -> Map("E" -> "E", "W" -> "W", "O" -> "W"),
    "ja" -> Map("E" -> "E", "W" -> "W", "東" -> "E", "西" -> "W"),
    "ko" -> Map("E" -> "E", "W" -> "W", "동" -> "E", "서" -> "W", "東" -> "E", "西" -> "W"),
    "mk" -> Map("E" -> "E", "W" -> "W", "И" -> "E", "З" -> "W"),
    "nl" -> Map("E" -> "E", "W" -> "W", "O" -> "E"),
    "pl" -> Map("E" -> "E", "W" -> "W", "wsch" -> "E", "zach" -> "W"),
    "pt" -> Map("E" -> "E", "W" -> "W", "O" -> "W"),
    "ru" -> Map("E" -> "E", "W" -> "W", "В" -> "E", "З" -> "W"),
    "zh" -> Map("E" -> "E", "W" -> "W", "东" -> "E", "西" -> "W", "東" -> "E", "西" -> "W")
    )

  // Map latitude letters used in different languages to English ("N" for North and "S" for South)
  val latitudeLetterMap = Map(
    "af" -> Map("N" -> "N", "S" -> "S"),
    "am" -> Map("N" -> "N", "S" -> "S"),
    "ar" -> Map("N" -> "N", "S" -> "S", "شمال" -> "N", "جنوب" -> "S"),
    "bg" -> Map("N" -> "N", "S" -> "S", "С" -> "N", "Ю" -> "S"),
    "cs" -> Map("N" -> "N", "S" -> "S"),
    "de" -> Map("N" -> "N", "S" -> "S"),
    "en" -> Map("N" -> "N", "S" -> "S"),
    "es" -> Map("N" -> "N", "S" -> "S"),
    "fr" -> Map("N" -> "N", "S" -> "S"),
    "hi" -> Map("N" -> "N", "S" -> "S", "उ" -> "N", "द" -> "S"),
    "it" -> Map("N" -> "N", "S" -> "S"),
    "ja" -> Map("N" -> "N", "S" -> "S", "北" -> "N", "南" -> "S"),
    "ko" -> Map("N" -> "N", "S" -> "S", "북" -> "N", "남" -> "S", "北" -> "N", "南" -> "S"),
    "mk" -> Map("N" -> "N", "S" -> "S", "С" -> "N", "Ј" -> "S"),
    "nl" -> Map("N" -> "N", "S" -> "S"),
    "pl" -> Map("N" -> "N", "S" -> "S", "płn" -> "N", "płd" -> "S"),
    "pt" -> Map("N" -> "N", "S" -> "S"),
    "ru" -> Map("N" -> "N", "S" -> "S", "С" -> "N", "Ю" -> "S"),
    "zh" -> Map("N" -> "N", "S" -> "S", "北" -> "N", "南" -> "S")
    )
}
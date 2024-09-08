package org.dbpedia.extraction.config.mappings

object GenderExtractorConfig
{

    val pronounsMap = Map(
        "en" -> Map("she" -> "female", "her" -> "female", "he" -> "male", "his" -> "male", "him" -> "male", "herself" -> "female", "himself" -> "male",
                    "She" -> "female", "Her" -> "female", "He" -> "male", "His" -> "male", "Him" -> "male", "Herself" -> "female", "Himself" -> "male" //TODO why not just do case insensitive matches?
        ),
        "am" -> Map(
          "እሷ" -> "ሴት",
          "እሷን" -> "ሴት",
          "የሷ" -> "ሴት",
          "እራሷን" -> "ሴት",
          "እራሷ" -> "ሴት",
          "እሱ" -> "ወንድ",
          "እሱን" -> "ወንድ",
          "የእሱ" -> "ወንድ",
          "የራሱ" -> "ወንድ",
          "እራሱ" -> "ወንድ",
          "እራሱን" -> "ወንድ"
        ),
         "pt" -> Map ("ela"-> "mulher", "dela" -> "mulher", "ele" -> "homem", "dele" -> "homem", "nela" -> "mulher", "nele" -> "homem",
                      "Ela"-> "mulher", "Dela" -> "mulher", "Ele" -> "homem", "Dele" -> "homem", "Nela" -> "mulher", "Nele" -> "homem"
         )

    )

    val supportedLanguages = pronounsMap.keySet

    val minCount = 3       // absolute count of gender indicators

    val minDifference = 2  // has to by X many winner genders over second placed genders

}

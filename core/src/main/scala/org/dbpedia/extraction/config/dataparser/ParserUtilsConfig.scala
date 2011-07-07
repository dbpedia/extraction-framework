package org.dbpedia.extraction.config.dataparser


object ParserUtilsConfig
{

    val scalesMap = Map(
        "en" -> Map(
            "thousand" -> 3,
            "million" -> 6,
            "mio" -> 6,
            "billion" -> 9,
            "trillion" -> 12,
            "quadrillion" -> 15
        ),
        "de" -> Map(
            "tausend" -> 3,
            "million" -> 6,
            "mio" -> 6,
            "milliarde" -> 9,
            "mrd" -> 9,
            "billion" -> 12
        )
    )

}
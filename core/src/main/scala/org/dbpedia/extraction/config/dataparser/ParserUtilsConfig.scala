package org.dbpedia.extraction.config.dataparser


object ParserUtilsConfig
{

    val scalesMap = Map(
        "en" -> Map(
            "thousand" -> 3,
            "million" -> 6,
            "mio" -> 6,
            "mln" -> 6,
            "billion" -> 9,
            "bln" -> 9,
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
        ),
        "el" -> Map(
            "χιλιάδες" -> 3,
            "χιλιαδες" -> 3,
            "εκατομμύρια" -> 6,
            "εκατομμυρια" -> 6,
            "δισεκατομμύρια" -> 9,
            "δισεκατομμυρια" -> 9,
            "τρισεκατομμύρια" -> 12,
            "τρισεκατομμυρια" -> 12,
            "τετράκις εκατομμύρια" -> 15
        ),
        
        "nl" -> Map(
            "honderd" -> 2,
            "duizend" -> 3,
            "miljoen" -> 6,
            "mio" -> 6,
            "mln" -> 6,
            "miljard" -> 9,
            "milj." -> 9,
            "mrd" -> 9,
            "biljard" -> 12,
            "triljoen" -> 15
        ),
        "pl" -> Map(
            "tysiąc" -> 3,
            "tysiące" -> 3,
            "tysięcy" -> 3,
            "tys" -> 3,
            "tyś" -> 3,
            "milion" -> 6,
            "miliony" -> 6,
            "milionów" -> 6,
            "mln" -> 6,
            "miliard" -> 9,
            "miliardy" -> 9,
            "miliardów" -> 9,
            "mld" -> 9,
            "bilion" -> 12,
            "biliony" -> 12,
            "bilionów" -> 12,
            "bln" -> 12
        ),
        "es" -> Map(
            "mil" -> 3,
            "millón" -> 6,
            "millardo" -> 9,
            "billón" -> 12,
            "trillón" -> 18,
            "cuatrillón" -> 24
        )
    )

}

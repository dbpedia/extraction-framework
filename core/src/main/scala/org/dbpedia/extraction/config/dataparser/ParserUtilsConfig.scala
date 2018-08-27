package org.dbpedia.extraction.config.dataparser

import java.util.Locale

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
        // For "ar" configuration, rendering right-to-left may seems like a bug, but it's not.
        // Don't change this else if you know how it is done.
        "ar" -> Map(
          "عشرة" -> 1,
          "مئة" -> 2,
          "ألف" -> 3,
          "مليون" -> 6,
          "مليار" -> 9,
          "بليون" -> 9,
          "تريليون" -> 12,
          "كوادريليون" -> 15
        ),
        "bg" -> Map(
            "хиляда" -> 3,
            "хиляди" -> 3,
            "милиони" -> 6,
            "милион" -> 6,
            "млрд" -> 9,
            "милиард" -> 9,
            "трлн." -> 12,
            "трилион" -> 12,
            "квадрилиона" -> 15,
            "квадрилиони" -> 15
        ),
        "ca" -> Map(
            "milion" -> 6,
            "milions" -> 6,
            "milion de" -> 6,
            "milion d'" -> 6,
            "milions de" -> 6,
            "milions d'" -> 6,
            "bilion" -> 9,
            "bilions" -> 9,
            "bilion de" -> 9,
            "bilion d'" -> 9,
            "bilions de" -> 9,
            "bilions d'" -> 9
        ),
        "de" -> Map(
            "tausend" -> 3,
            "million" -> 6,
            "mio" -> 6,
            "mio." -> 6,
            "milliarde" -> 9,
            "mrd" -> 9,
            "mrd." -> 9,
            "billion" -> 12
        ),
        "el" -> Map(
            "χιλιάδες" -> 3,
            "χιλιαδες" -> 3,
            "εκατομμύρια" -> 6,
            "εκατομμυρια" -> 6,
            "δισεκατομμύρια" -> 9,
            "δισεκατομμυρια" -> 9,
            "δισ." -> 9,
            "τρισεκατομμύρια" -> 12,
            "τρισεκατομμυρια" -> 12,
            "τετράκις εκατομμύρια" -> 15
        ),
        "eo" -> Map(
            "mil" -> 3,
            "miliono" -> 6,
            "miliardo" -> 9,
            "biliono" -> 12,
            "biliardo" -> 15,
            "triliono" -> 18
        ),
        "es" -> Map(
            "mil" -> 3,
            "millón" -> 6,
            "millones" -> 6,
            "mill." -> 6,
            "millardo" -> 9,
            "billón" -> 12,
            "trillón" -> 18,
            "cuatrillón" -> 24
        ),
        "fr" -> Map(
            "mille" -> 3,
            "million" -> 6,
            "millions" -> 6,
            "million de" -> 6,
            "million d'" -> 6,
            "millions de" -> 6,
            "millions d'" -> 6,
            "milliard" -> 9,
            "milliards" -> 9,
            "milliard de" -> 9,
            "milliard d'" -> 9,
            "milliards de" -> 9,
            "milliards d'" -> 9,
            "mrd" -> 9,
            "billion" -> 12,
            "trillion" -> 18
        ),
        "ga" -> Map(
            "míle" -> 3,
            "milliún" -> 6,
            "billiún" -> 9,
            "míle milliún" -> 9,
            "trilliún" -> 12,
            "cuaidrilliún" -> 15
        ),
        "gl" -> Map(
            "mil" -> 3,
            "miles" -> 3,
            "milleiro" -> 3,
            "milleiros" -> 3,
            "millar" -> 3,
            "millares" -> 3,
            "millón" -> 6,
            "millóns" -> 6,
            "mil millóns" -> 9,
            "miles de millóns" -> 9,
            "billón" -> 12,
            "billóns" -> 12,
            "mil billóns" -> 15,
            "miles de billóns" -> 15,
            "trillón" -> 18,
            "trillóns" -> 18,
            "cuadrillón" -> 24,
            "cuadrillóns" -> 24
        ),
        "it" -> Map(
            "mille" -> 3,
            "milione" -> 6,
            "milioni" -> 6,
            "milioni di" -> 6,
            "mln" -> 6,
            "miliardo" -> 9,
            "miliardi" -> 9,
            "miliardi di" -> 9,
            "bilione" -> 12,
            "quadrilione" -> 15
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
            "bln" -> 12,
            "kwadrylion" -> 15
        ),
        "pt" -> Map(
            "mil" -> 3,
            "milhão" -> 6,
            "mil milhões" -> 9,
            "bilhão" -> 9,
            "bilhões" -> 9,
            "bilião" -> 12,
            "biliões" -> 12,
            "trilhão" -> 12,
            "trilhões" -> 12,
            "mil bilhões" -> 15,
            "quatrilhão" -> 15,
            "quatrilhões" -> 15,
            "trilião" -> 18,
            "triliões" -> 18,
            "quintilhão" -> 18,
            "quintilhões" -> 18,
            "quinquilhão" -> 18,
            "quinquilhões" -> 18
        ),
        "sv" -> Map(
            "tusen" -> 3,
            "miljoner" -> 6,
            "miljarder" -> 9,
            "biljoner" -> 12,
            "biljarder" -> 15

        ),
        "cs" -> Map(
            "tisíc" -> 3,
            "tisíce" -> 3,
            "milión" -> 6,
            "milióny" -> 6,
            "miliardy" -> 9,
            "bilión" -> 12,
            "bilion" -> 12,
            "biliarda" -> 15,
            "kvadrilion" -> 15
        ),
        "hu" -> Map(
            "ezer" -> 3,
            "több ezer" -> 3,
            "millió" -> 6,
            "több millió" -> 6,
            "milliárd" -> 9,
            "billió" -> 12,
            "kvadrillió" -> 15
        ),
        "uk" -> Map(
            "тисяча" -> 3,
            "тисячі" -> 3,
            "мільйони" -> 6,
            "мільйона" -> 6,
            "млн" -> 6,
            "мільярд" -> 9,
            "млрд" -> 9,
            "мільярди" -> 9,
            "трильйон" -> 12,
            "квадрильйон" -> 15
        ),
        "lv" -> Map(
            "tūkstotis" -> 3,
            "tūkstošiem" -> 3,
            "miljons" -> 6,
            "miljoni" -> 6,
            "miljoniem" -> 6,
            "miljardiem" -> 9,
            "miljardi" -> 9,
            "miljards" -> 9,
            "triljons" -> 12,
            "quadrillion" -> 15
        ),
        "lt" -> Map(
            "tūkst" -> 3,
            "tūkstantis" -> 3,
            "milijonai" -> 6,
            "milijonas" -> 6,
            "milijardus" -> 9,
            "milijardų" -> 9,
            "mlrd" -> 9,
            "milijardai" -> 9,
            "trilijonas" -> 12,
            "kvadrilijonas" -> 15,
            "kvadrilijonai" -> 15
        )
    )
    
    /**
     * By default the locale used for number parsing is the language locale.
     * However, this may not be suited for some languages, for instance French where "." is used as a decimal separator instead of "," (because the decimal separator ',' would interfere with template separators)
     * Add an entry when other separators than the locale default are used.
     */
    val decimalSeparators = Map(
        "bg" -> ",|.",
        "fr" -> ",|.",
        "gl" -> ","
        
    )

}

package org.dbpedia.extraction.config.dataparser

object DateTimeParserConfig
{
    //names of months; have to be in lower-case
    val monthsMap = Map(
        "de" -> Map("januar"->1,"februar"->2,"märz"->3,"maerz"->3,"april"->4,"mai"->5,"juni"->6,"juli"->7,"august"->8,"september"->9,"oktober"->10,"november"->11,"dezember"->12),
        "el" -> Map("ιανουάριος"->1,"φεβρουάριος"->2,"μάρτιος"->3,"απρίλιος"->4,"μάϊος"->5,"μάιος"->5,"ιούνιος"->6,"ιούλιος"->7,"αύγουστος"->8,"σεπτέμβριος"->9,"οκτώβριος"->10,"νοέμβριος"->11,"δεκέμβριος"->12,
                    "ιανουαρίου"->1,"φεβρουαρίου"->2,"μαρτίου"->3,"απριλίου"->4,"μαΐου"->5,"μαίου"->5,"ιουνίου"->6,"ιουλίου"->7,"αυγούστου"->8,"σεπτεμβρίου"->9,"οκτωβρίου"->10,"νοεμβρίου"->11,"δεκεμβρίου"->12),
        "en" -> Map("january"->1,"february"->2,"march"->3,"april"->4,"may"->5,"june"->6,"july"->7,"august"->8,"september"->9,"october"->10,"november"->11,"december"->12),
        "es" -> Map("enero"->1,"febrero"->2,"marzo"->3,"abril"->4,"mayo"->5,"junio"->6,"julio"->7,"agosto"->8,"septiembre"->9,"octubre"->10,"noviembre"->11,"diciembre"->12),
        "fr" -> Map("janvier"->1,"février"->2,"mars"->3,"avril"->4,"mai"->5,"juin"->6,"juillet"->7,"août"->8,"septembre"->9,"octobre"->10,"novembre"->11,"décembre"->12),
        "hr" -> Map("siječanj"->1,"veljača"->2,"ožujak"->3,"travanj"->4,"svibanj"->5,"lipanj"->6,"srpanj"->7,"kolovoz"->8,"rujan"->9,"listopad"->10,"studeni"->11,"prosinac"->12),
        "it" -> Map("gennaio"->1,"febbraio"->2,"marzo"->3,"aprile"->4,"maggio"->5,"giugno"->6,"luglio"->7,"agosto"->8,"settembre"->9,"ottobre"->10,"novembre"->11,"dicembre"->12),
        "pl" -> Map("stycznia"->1,"lutego"->2,"marca"->3,"kwietnia"->4,"maja"->5,"czerwca"->6,"lipca"->7,"sierpnia"->8,"września"->9,"października"->10,"listopada"->11,"grudnia"->12),
        "pt" -> Map("janeiro"->1,"fevereiro"->2,"março"->3,"abril"->4,"maio"->5,"junho"->6,"julho"->7,"agosto"->8,"setembro"->9,"outubro"->10,"novembro"->11,"dezembro"->12,
                    "jan"->1,"fev"->2,"mar"->3,"abr"->4,"mai"->5,"jun"->6,"jul"->7,"ago"->8,"set"->9,"out"->10,"nov"->11,"dez"->12),
        "ru" -> Map("январь"->1,"февраль"->2,"март"->3,"апрель"->4,"май"->5,"июнь"->6,"июль"->7,"август"->8,"сентябрь"->9,"октябрь"->10,"ноябрь"->11,"декабрь"->12,
                    "янв"->1,"фев"->2,"мар"->3,"апр"->4,"май"->5,"июн"->6,"июл"->7,"авг"->8,"сен"->9,"окт"->10,"ноя"->11,"дек"->12)
    )

    //set of wiki codes for which this parser can be applied
    val supportedLanguages = monthsMap.keySet

    // -1 is for BC
    //TODO matches anything e.g. 20 bd
    val eraStrMap =  Map(
        "en" -> Map("BCE" -> -1, "BC" -> -1, "CE"-> 1, "AD"-> 1, "AC"-> -1, "CE"-> 1),
        "el" -> Map("ΠΧ"-> -1, "Π\\.Χ\\."-> -1, "Π\\.Χ"-> -1 , "ΜΧ"-> 1 , "Μ\\.Χ\\."-> 1, "Μ\\.Χ"-> 1),
        "pt" -> Map("AC"-> -1, "A\\.C\\."-> -1, "DC"-> 1, "D\\.C\\."-> 1, "AD"-> 1, "A\\.D\\."-> 1, "AEC"-> 1, "A\\.E\\.C\\."-> 1 , "EC"-> 1, "E\\.C\\."-> 1),
        "es" -> Map("AC"-> -1, "A\\.C\\."-> -1, "DC"-> 1, "D\\.C\\."-> 1, "AD"-> 1, "A\\.D\\."-> 1, "AEC"-> 1, "A\\.E\\.C\\."-> 1 , "EC"-> 1, "E\\.C\\."-> 1)
    )

    //suffixes for 1st, 2nd etc. (maybe add this to infobox extractor RankRegex val)
    val cardinalityRegexMap = Map(
        "en" -> "st|nd|rd|th",
        "el" -> "η|ης",
        "pt" -> "°|\\.°|°\\.",
        "es" -> "°|\\.°|°\\."
    )

    //specifies for a template name (lower-cased) the property keys of year, month and day
    val templateDateMap = Map(
        // Sometimes the templates are used wrong like the folifPropertyNumlowing, but this is handled properly
        // {{birth date|df=yes|1833|10|21}}

        // http://en.wikipedia.org/wiki/Template:Birth_date_and_age
        // {{Birth date|year_of_birth|month_of_birth|day_of_birth|...}}
        // http://en.wikipedia.org/wiki/Template:BirthDeathAge
        // gets the text from the single textNode of the first PropertyNode
        // {{BirthDeathAge|BIRTH_OR_DEATH_FLAG|year_of_birth|month_of_birth|day_of_birth|year_of_death|month_of_death|day_of_death|...}}
        "en" -> Map(
            "birth date and age"  -> Map ("year" -> "1", "month"-> "2", "day" -> "3"), //"Birth date and age"
            "birth date and age2" -> Map ("year" -> "1", "month"-> "2", "day" -> "3"), //"Birth date and age2"
            "death date and age"  -> Map ("year" -> "1", "month"-> "2", "day" -> "3"), //"Death date and age"
            "birth date"          -> Map ("year" -> "1", "month"-> "2", "day" -> "3"), //"Birth date"
            "death date"          -> Map ("year" -> "1", "month"-> "2", "day" -> "3"), //"Death date"
            "bda"                 -> Map ("year" -> "1", "month"-> "2", "day" -> "3"), //"Bda"
            "dob"                 -> Map ("year" -> "1", "month"-> "2", "day" -> "3"), //"Dob"
            "start date"          -> Map ("year" -> "1", "month"-> "2", "day" -> "3"), //"Start date"

            //Contains only text, just call findDate()  "1" is the property to look up for
            "birth-date"          -> Map ("text" -> "1"),                              //"Birth-date"

            //conditional mapping .. for multiple matching ifPropertyNumHasValue could be a regex (not implemented for multiple)
            "birthdeathage"       -> Map ("ifPropertyNum" -> "1", "ifPropertyNumHasValue" -> "B", //"BirthDeathAge"
                                          "year" -> "2", "month"-> "3", "day" -> "4",
                                          "elseYear" -> "4", "elseMonth"-> "5", "elseDay" -> "6")
        ),

        //alphabetically for other languages

        "ca" -> Map(
            "Edat"                  -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "Data naixement i edat" -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "Data naixement"        -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "Data defunció i edat"  -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "Data defunció"         -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "Data d'inici i edat"   -> Map ("year" -> "3", "month"-> "2", "day" -> "1")
        ),
        "cs" -> Map(
            "Datum narození a věk"  -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Datum narození"        -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Datum úmrtí a věk"     -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Datum úmrtí"           -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "věk v letech a dnech"  -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "věk ve dnech"          -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "věk"                   -> Map ("year" -> "1", "month"-> "2", "day" -> "3")
        ),
        "el" -> Map(
            "ημερομηνία γέννησης και ηλικία" -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "ημερομηνία θανάτου και ηλικία"  -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Ημερομηνία εκκίνησης και ηλικία"-> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "ημερομηνία γέννησης"            -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "ηθηλ"                           -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "ηγη"                            -> Map ("year" -> "1", "month"-> "2", "day" -> "3")
        ),
        "eu" -> Map(
            "adina"                -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "adin parentesigabea"  -> Map ("year" -> "1", "month"-> "2", "day" -> "3")
        ),
        "pt" -> Map(
            "Nascimento"  -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Dni"         -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Dnibr"       -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "DataExt"     -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Falecimento" -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Morte"       -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Falecimento2"-> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Dtlink"      -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Dtext"       -> Map ("year" -> "1", "month"-> "2", "day" -> "3")
        ),
        "ru" -> Map(
            "Возраст"      -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "ДатаРождения" -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "ДатаСмерти"   -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "Прошло лет"   -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "Умер"         -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "Родился"      -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),

            // English template names
            "Start date"           -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Start date and age"   -> Map ("year" -> "1", "month"-> "2", "day" -> "3")
        ),
        "sl" -> Map(
            "Datum rojstva"             -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Datum rojstva in starost"  -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Datum rojstva in starost2" -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Datum smrti"               -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Datum smrti in starost"    -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Starost na datum"          -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Starost v letih in dnevih" -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "starost"                   -> Map ("year" -> "1", "month"-> "2", "day" -> "3")
        ),
        "uk" -> Map(
            "Дата з віком"        -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Вік"                 -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Age"                 -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Дата смерті з віком" -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Дата народження"     -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "Народився"           -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "Дата смерті"         -> Map ("year" -> "3", "month"-> "2", "day" -> "1")
        )
    )

}

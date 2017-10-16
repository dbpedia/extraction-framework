package org.dbpedia.extraction.config.dataparser

object DateTimeParserConfig
{
    //names of months; have to be in lower-case
    val monthsMap = Map(
        // For "ar" configuration, right-to-left rendering may seem like a bug, but it's not.
        // Don't change this unless you know how it is done.
        "ar" -> Map("جانفي"->1,"فيفري"->2,"مارس"->3,"أفريل"->4,"ماي"->5,"جوان"->6,"جويلية"->7,"أوت"->8,"سبتمبر"->9,"أكتوبر"->10,"نوفمبر"->11,"ديسمبر"->12,
          "يناير"->1,"فبراير"->2,"أبريل"->4,"مايو"->5,"يونيو"->6,"يوليو"->7,"يوليوز"->7,"أغسطس"->8,"غشت"->8,"شتنبر"->9,"نونبر"->11,"دجنبر"->12),
        "bg" -> Map("януари"->1,"февруари"->2,"март"->3,"април"->4,"май"->5,"юни"->6,"юли"->7,"август"->8,"септември"->9,"октомври"->10,"ноември"->11,"декември"->12),
        "cs" -> Map("leden"->1,"únor"->2,"březen"->3,"duben"->4,"květen"->5,"červen"->6,"červenec"->7,"srpen"->8,"září"->9,"říjen"->10,"listopad"->11,"prosinec"->12),
        "de" -> Map("januar"->1,"februar"->2,"märz"->3,"maerz"->3,"april"->4,"mai"->5,"juni"->6,"juli"->7,"august"->8,"september"->9,"oktober"->10,"november"->11,"dezember"->12),
        "el" -> Map("ιανουάριος"->1,"φεβρουάριος"->2,"μάρτιος"->3,"απρίλιος"->4,"μάϊος"->5,"μάιος"->5,"ιούνιος"->6,"ιούλιος"->7,"αύγουστος"->8,"σεπτέμβριος"->9,"οκτώβριος"->10,"νοέμβριος"->11,"δεκέμβριος"->12,
                    "ιανουαρίου"->1,"φεβρουαρίου"->2,"μαρτίου"->3,"απριλίου"->4,"μαΐου"->5,"μαίου"->5,"ιουνίου"->6,"ιουλίου"->7,"αυγούστου"->8,"σεπτεμβρίου"->9,"οκτωβρίου"->10,"νοεμβρίου"->11,"δεκεμβρίου"->12),
        "en" -> Map("january"->1,"february"->2,"march"->3,"april"->4,"may"->5,"june"->6,"july"->7,"august"->8,"september"->9,"october"->10,"november"->11,"december"->12),
        "eo" -> Map("januaro"->1,"februaro"->2,"marto"->3,"aprilo"->4,"majo"->5,"junio"->6,"julio"->7,"aŭgusto"->8,"septembro"->9,"oktobro"->10,"novembro"->11,"decembro"->12),
        "es" -> Map("enero"->1,"febrero"->2,"marzo"->3,"abril"->4,"mayo"->5,"junio"->6,"julio"->7,"agosto"->8,"septiembre"->9,"octubre"->10,"noviembre"->11,"diciembre"->12),
        "eu" -> Map("urtarrila"->1,"otsaila"->2,"martxoa"->3,"apirila"->4,"maiatza"->5,"ekaina"->6,"uztaila"->7,"abuztua"->8,"iraila"->9,"urria"->10,"azaroa"->11,"abendua"->12),
        "fr" -> Map("janvier"->1,"février"->2,"mars"->3,"avril"->4,"mai"->5,"juin"->6,"juillet"->7,"août"->8,"septembre"->9,"octobre"->10,"novembre"->11,"décembre"->12),
        "ga" -> Map("eanáir"->1,"feabhra"->2,"marta"->3,"aibreán"->4,"bealtaine"->5,"meitheamh"->6,"iúil"->7,"lúnasa"->8,"meán fómhair"->9,"deireadh fómhair"->10,"samhain"->11,"nollaig"->12),
        "gl" -> Map("xaneiro"->1,"febreiro"->2,"marzo"->3,"abril"->4,"maio"->5,"xuño"->6,"xullo"->7,"agosto"->8,"setembro"->9,"outubro"->10,"novembro"->11,"decembro"->12,
                    "xan"->1,"feb"->2,"mar"->3,"abr"->4,"mai"->5,"xuñ"->6,"xul"->7,"ago"->8,"set"->9,"out"->10,"nov"->11,"dec"->12),
        "hr" -> Map("siječanj"->1,"veljača"->2,"ožujak"->3,"travanj"->4,"svibanj"->5,"lipanj"->6,"srpanj"->7,"kolovoz"->8,"rujan"->9,"listopad"->10,"studeni"->11,"prosinac"->12),
        "id" -> Map("januari"->1,"februari"->2,"maret"->3,"april"->4,"mei"->5,"juni"->6,"juli"->7,"agustus"->8,"september"->9,"oktober"->10,"november"->11,"desember"->12),
        "it" -> Map("gennaio"->1,"febbraio"->2,"marzo"->3,"aprile"->4,"maggio"->5,"giugno"->6,"luglio"->7,"agosto"->8,"settembre"->9,"ottobre"->10,"novembre"->11,"dicembre"->12),
        "ja" -> Map("一月"->1,"二月"->2,"三月"->3,"四月"->4,"五月"->5,"六月"->6,"七月"->7,"八月"->8,"九月"->9,"十月"->10,"十一月"->11,"十二月"->12,
                    "1月"->1,"2月"->2,"3月"->3,"4月"->4,"5月"->5,"6月"->6,"7月"->7,"8月"->8,"9月"->9,"10月"->10,"11月"->11,"12月"->12,
                    "１月"->1,"２月"->2,"３月"->3,"４月"->4,"５月"->5,"６月"->6,"７月"->7,"８月"->8,"９月"->9,"１０月"->10,"１１月"->11,"１２月"->12),
        "nl" -> Map("januari"->1,"februari"->2,"maart"->3,"april"->4,"mei"->5,"juni"->6,"juli"->7,"augustus"->8,"september"->9,"oktober"->10,"november"->11,"december"->12),
        "pl" -> Map("stycznia"->1,"lutego"->2,"marca"->3,"kwietnia"->4,"maja"->5,"czerwca"->6,"lipca"->7,"sierpnia"->8,"września"->9,"października"->10,"listopada"->11,"grudnia"->12,
                    "styczeń"->1,"luty"->2,"marzec"->3,"kwiecień"->4,"maj"->5,"czerwiec"->6,"lipiec"->7,"sierpień"->8,"wrzesień"->9,"październik"->10,"listopad"->11,"grudzień"->12),
        "pt" -> Map("janeiro"->1,"fevereiro"->2,"março"->3,"abril"->4,"maio"->5,"junho"->6,"julho"->7,"agosto"->8,"setembro"->9,"outubro"->10,"novembro"->11,"dezembro"->12,
                    "jan"->1,"fev"->2,"mar"->3,"abr"->4,"mai"->5,"jun"->6,"jul"->7,"ago"->8,"set"->9,"out"->10,"nov"->11,"dez"->12),
        "ru" -> Map("январь"->1,"февраль"->2,"март"->3,"апрель"->4,"май"->5,"июнь"->6,"июль"->7,"август"->8,"сентябрь"->9,"октябрь"->10,"ноябрь"->11,"декабрь"->12,
                    "янв"->1,"фев"->2,"мар"->3,"апр"->4,"май"->5,"июн"->6,"июл"->7,"авг"->8,"сен"->9,"окт"->10,"ноя"->11,"дек"->12,
                    "января"->1,"февраля"->2,"марта"->3,"апреля"->4,"мая"->5,"июня"->6,"июля"->7,"августа"->8,"сентября"->9,"октября"->10,"ноября"->11,"декабря"->12),
        "sk" -> Map("január"->1,"február"->2,"marec"->3,"apríl"->4,"máj"->5,"jún"->6,"júl"->7,"august"->8,"september"->9,"október"->10,"november"->11,"december"->12),
        "sv" -> Map("januari"->1, "februari"->2, "mars"->3, "april"->4, "maj"->5, "juni"->6, "juli"->7, "augusti"->8, "september"->9, "oktober"->10, "november"->11, "december"->12),
        "uk" -> Map("січень"->1,"лютий"->2,"березень"->3,"квітень"->4,"травень"->5,"червень"->6,"липень"->7,"серпень"->8,"вересень"->9,"жовтень"->10,"листопад"->11,"грудень"->12,
                    "січня"->1,"лютого"->2,"березня"->3,"квітня"->4,"травня"->5,"червня"->6,"липня"->7,"серпня"->8,"вересня"->9,"жовтня"->10,"листопада"->11,"грудня"->12)
    )

    //set of wiki codes for which this parser can be applied
    val supportedLanguages = monthsMap.keySet

    // -1 is for BC
    //TODO matches anything e.g. 20 bd
    val eraStrMap =  Map(
        "en" -> Map("BCE" -> -1, "BC" -> -1, "CE"-> 1, "AD"-> 1, "AC"-> -1, "CE"-> 1),
        // For "ar" configuration, right-to-left rendering may seem like a bug, but it's not.
        // Don't change this unless you know how it is done.
        "ar" -> Map("ق.م." -> -1, "م." -> 1),
        "bg" -> Map("пр\\.н\\.е\\." -> -1),
        "el" -> Map("ΠΧ"-> -1, "Π\\.Χ\\."-> -1, "Π\\.Χ"-> -1 , "ΜΧ"-> 1 , "Μ\\.Χ\\."-> 1, "Μ\\.Χ"-> 1),
        "eo" -> Map("a.K." -> -1, "p.K." -> -1),
        "es" -> Map("AC"-> -1, "A\\.C\\."-> -1, "DC"-> 1, "D\\.C\\."-> 1, "AD"-> 1, "A\\.D\\."-> 1, "AEC"-> 1, "A\\.E\\.C\\."-> 1 , "EC"-> 1, "E\\.C\\."-> 1),
        "eu" -> Map("K\\.a\\."-> -1, "A\\. D\\."-> 1),
        "fr" -> Map("av\\. J\\.-C\\."-> -1, "ap\\. J\\.-C\\." -> 1),
        "ga" -> Map("B\\.C\\." -> -1, "R\\.C\\." -> -1, "r\\. Chr\\." -> -1, "BC" -> -1, "RC" -> -1, "A\\.D\\." -> 1, "AD" -> 1, "I\\.C\\." -> 1, "IC" -> 1),
        "gl" -> Map("AC"-> -1, "A\\.C\\."-> -1, "DC"-> 1, "D\\.C\\."-> 1, "aC"-> -1, "a\\.C\\."-> -1, "dC"-> 1, "d\\.C\\."-> 1, "AEC"-> -1, "A\\.E\\.C\\."-> -1 , "EC"-> 1, "E\\.C\\."-> 1),
        "it" -> Map("AC"-> -1, "A\\.C\\."-> -1, "DC"-> 1, "D\\.C\\."-> 1, "AD"-> 1, "A\\.D\\."-> 1, "PEV"-> -1, "P\\.E\\.V\\."-> -1, "EV"-> 1, "E\\.V\\." -> 1),
        "ja" -> Map("前"-> -1, "B\\.C\\."-> -1, "西暦"-> 1),
        "nl" -> Map("v\\.Chr\\." -> -1, "n\\.C\\."-> 1, "v\\.C\\." -> -1, "n\\.Chr\\."-> 1, "voor Chr\\." -> -1, "na Chr\\."-> 1), 
        "pl" -> Map("p\\.n\\.e\\." -> -1, "n\\.e\\." -> 1),
        "pt" -> Map("AC"-> -1, "A\\.C\\."-> -1, "DC"-> 1, "D\\.C\\."-> 1, "AD"-> 1, "A\\.D\\."-> 1, "AEC"-> 1, "A\\.E\\.C\\."-> 1 , "EC"-> 1, "E\\.C\\."-> 1),
        "sv" -> Map("f\\.Kr\\." -> -1, "e\\.Kr\\." -> 1, "f\\.v\\.t\\." -> -1, "e\\.v\\.t\\." -> 1)
    )

    //suffixes for 1st, 2nd etc. (maybe add this to infobox extractor RankRegex val)
    val cardinalityRegexMap = Map(
        "en" -> "st|nd|rd|th",
        "cs" -> "\\.",
        "el" -> "η|ης",
        "eo" -> "-a|-an",
        "es" -> "°|\\.°|°\\.",
        "eu" -> "\\.",
        "fr" -> "er|nd|ème",
        "ga" -> "ú",
        "gl" -> "°|\\.°|°\\.|ª|\\.ª|ª\\.|º|\\.º|º\\.",
        "it" -> "°|\\.°|°\\.",
        "nl" -> "ste|de|e",
        "pt" -> "°|\\.°|°\\.",
        "sk" -> "\\.",
        "sv" -> ":e"
    )

    //specifies for a template name (lower-cased) the property keys of year, month and day
    // KEYS HAVE TO BE LOWERCASE!!!
    val templateDateMap = Map(
        // Sometimes the templates are used wrong like the following, but this is handled properly
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
            "end date"            -> Map ("year" -> "1", "month"-> "2", "day" -> "3"), //"End date"

            //Contains only text, just call findDate()  "1" is the property to look up for
            "birth-date"          -> Map ("text" -> "1"),                              //"Birth-date"
            "start-date"          -> Map ("text" -> "1"),
            "death-date"          -> Map ("text" -> "1"),                              //"Death-date"
            "end-date"            -> Map ("text" -> "1"),

            //conditional mapping .. for multiple matching ifPropertyNumHasValue could be a regex (not implemented for multiple)
            "birthdeathage"       -> Map ("ifPropertyNum" -> "1", "ifPropertyNumHasValue" -> "B", //"BirthDeathAge"
                                          "year" -> "2", "month"-> "3", "day" -> "4",
                                          "elseYear" -> "4", "elseMonth"-> "5", "elseDay" -> "6"),
            "year"                -> Map ("year" -> "1"),
            "bc year in topic"    -> Map ("year" -> "1"),
            "imdb country year"   -> Map ("year" -> "2"),
            "birth year and age"  -> Map ("year" -> "1"),
            "death year and age"  -> Map ("year" -> "1"),
            "year dab"            -> Map ("year" -> "1"),
            "dash year"           -> Map ("year" -> "1", "year2" -> "2"),
            "cfl year"            -> Map ("year" -> "1", "year2" -> "2"),
            "cfly"                -> Map ("year" -> "1", "year2" -> "2"),
            "nba year"            -> Map ("year" -> "1", "year2" -> "2"),
            "nbay"                -> Map ("year" -> "1", "year2" -> "2"),
            "nhl_year"            -> Map ("year" -> "1", "year2" -> "2"),
            "nhly"                -> Map ("year" -> "1", "year2" -> "2"),
            "baseball year"       -> Map ("year" -> "1", "year2" -> "2"),
            "nfl_year"            -> Map ("year" -> "1", "year2" -> "2"),
            "nfly"                -> Map ("year" -> "1", "year2" -> "2"),
            "afl year"            -> Map ("year" -> "1", "year2" -> "2"),
            "afly"                -> Map ("year" -> "1", "year2" -> "2")
        ),

        // alphabetically for other languages
        
        // For "ar" configuration, right-to-left rendering may seem like a bug, but it's not.
        // Don't change this unless you know how it is done.
        "ar" -> Map(
          "تاريخ الازدياد و العمر"  -> Map ("year" -> "1", "month"-> "2", "day" -> "3"), //"Birth date and age"
          "تاريخ الوفاة و العمر"  -> Map ("year" -> "1", "month"-> "2", "day" -> "3"), //"Death date and age"
          "تاريخ الولادة"          -> Map ("year" -> "1", "month"-> "2", "day" -> "3"), //"Birth date"
          "تاريخ الوفاة"          -> Map ("year" -> "1", "month"-> "2", "day" -> "3"), //"Death date"
          "تاريخ الازدياد"                 -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
          "تاريخ البدأ"                 -> Map ("year" -> "1", "month"-> "2", "day" -> "3")
        ),
        "bg" -> Map(
            "Дата на раждане и години" -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Дата на раждане"          -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Дата на смърт и години"   -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Дата на смърт"            -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "роден"                    -> Map ("year" -> "3", "month"-> "2", "day" -> "1")
        ),
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
            "adin parentesigabea"  -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "data"                 -> Map ("year" -> "1", "month"-> "2", "day" -> "3")
        ),
        "fr" -> Map(
            "date"                            -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "date de naissance"               -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "date de décès"                   -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "date sport"                      -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "date début"                      -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "birth date and age"              -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            //conditional mapping
            "naissance décès âge"             -> Map ("ifPropertyNum" -> "1", "ifPropertyNumHasValue" -> "N",
                                                      "year" -> "4", "month"-> "3", "day" -> "2",
                                                      "elseYear" -> "7", "elseMonth"-> "6", "elseDay" -> "5"),
            "année de naissance et âge"       -> Map ("year" -> "1", "month"-> "2"),
            "année de décès et âge"           -> Map ("year" -> "1", "month"-> "3")
        ),
        "ga" -> Map(
            "dáta breithe agus aois"  -> Map ("bliain" -> "1", "mí"-> "2", "lá" -> "3"), //"Lá breithe agus aois"
            "dáta breithe"        -> Map ("bliain" -> "1", "mí"-> "2", "lá" -> "3") //"Birth date"
        ),
        "id" -> Map(
            "Mula tanggal dan usia"         -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Tanggal lahir dan umur"        -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Tanggal kematian dan umur"     -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Umur pada tanggal"             -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "umur"                          -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Tanggal lahir dan umur2/doc"   -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Tanggal lahir dan umur2"       -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "lahirmati"                     -> Map ("year" -> "2", "month"-> "3", "day" -> "4"),
            "birth date and age"            -> Map ("year" -> "1", "month"-> "2", "day" -> "3"), //"Birth date and age"
            "birth date and age2"           -> Map ("year" -> "1", "month"-> "2", "day" -> "3"), //"Birth date and age2"
            "death date and age"            -> Map ("year" -> "1", "month"-> "2", "day" -> "3"), //"Death date and age"
            "birth date"                    -> Map ("year" -> "1", "month"-> "2", "day" -> "3"), //"Birth date"
            "death date"                    -> Map ("year" -> "1", "month"-> "2", "day" -> "3"), //"Death date"
            "bda"                           -> Map ("year" -> "1", "month"-> "2", "day" -> "3"), //"Bda"
            "dob"                           -> Map ("year" -> "1", "month"-> "2", "day" -> "3"), //"Dob"
            "start date"                    -> Map ("year" -> "1", "month"-> "2", "day" -> "3") //"Start date"
        ), 
        "it" -> Map(
            "Data nascita"        -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "data nascita"        -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "data di nascita"     -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "data di nascita"     -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "nascita anno"        -> Map ("year" -> "1"),
            "nascita mese"        -> Map ("month" -> "1"),
            "nascita giorno"      -> Map ("day" -> "1"),
            "data di morte"       -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "Morte"               -> Map ("year" -> "3", "month"-> "2", "day" -> "1")
        ),
        "ja" -> Map(
            "生年月日と年齢"        -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "生年月日と年齢2"       -> Map ("year" -> "4", "month"-> "5", "day" -> "6"),
            "生年月日と馬齢"        -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "死亡年月日と没年齢"    -> Map ("year" -> "4", "month"-> "5", "day" -> "6"),
            "死亡年月日と没馬齢"    -> Map ("year" -> "4", "month"-> "5", "day" -> "6"),
            "没年齢"                -> Map ("year" -> "4", "month"-> "5", "day" -> "6"),
            "没年齢2"               -> Map ("year" -> "4", "month"-> "5", "day" -> "6"),
            "年数"                  -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "年"                    -> Map ("year" -> "1"),
            "birth date and age"    -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "birth date and age2"   -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "death date and age"    -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "start date"            -> Map ("year" -> "1", "month"-> "2", "day" -> "3")
        ),
        "nl" -> Map(
            "geboren"  			    -> Map ("year" -> "3", "month"-> "2", "day" -> "1"), 
            "geboortedatum" 		-> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "overleden" 		    -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "sterfdatum" 		    -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "overlijdensdatum" 	-> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "overlijddatum" 		-> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "datumbegin"     		-> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "begindatum" 		    -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "einddatum" 		    -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "datumeind" 		    -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "datum begin" 	   	-> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "datum eind" 		    -> Map ("year" -> "3", "month"-> "2", "day" -> "1"),
            "datum afbeelding"	-> Map ("year" -> "3", "month"-> "2", "day" -> "1")
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
        "sk" -> Map(
            "Počiatočný dátum a vek" -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            // FIXME: The following can take a second date as params 4-6
            "Dátum narodenia a vek"  -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Dnv"                    -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Vek"                    -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Vek v dňoch"            -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Vek v rokoch a dňoch"   -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Dátum úmrtia a vek"     -> Map ("year" -> "1", "month"-> "2", "day" -> "3"),
            "Dúv"                    -> Map ("year" -> "1", "month"-> "2", "day" -> "3")
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
    ).map( x=> x._1 -> x._2.map( y => y._1.toLowerCase.replace("_", " ") -> y._2)).toMap

}

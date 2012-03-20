package org.dbpedia.extraction.util

import java.util.Locale

/**
 * Represents a Wikipedia language.
 */
class Language private(val wikiCode : String, val isoCode: String)
{
    val locale : Locale = new Locale(isoCode)
    
    /** 
     * Note that Wikipedia dump files use this prefix (with underscores), 
     * but Wikipedia domains use the wikiCode (with dashes), e.g. http://be-x-old.wikipedia.org
     */
    val filePrefix = wikiCode.replace("-", "_")
    
    override def toString() = locale.toString

    override def equals(other : Any) = other match
    {
        case otherLang : Language => (wikiCode == otherLang.wikiCode)
        case _ => false
    }

    override def hashCode = wikiCode.hashCode
}

object Language
{
    private val isoCodes = Locale.getISOLanguages.toSet

    /**
     * Maps Wikipedia language codes which do not follow ISO-639-1, to a related ISO-639-1 code.
     * See: http://s23.org/wikistats/wikipedias_html.php (and http://en.wikipedia.org/wiki/List_of_Wikipedias)
     * Mappings are mostly based on similarity of the languages and in some cases on the regions where a related language is spoken.
     */
    // not private for NonIsoLanguagesMappingTest
    protected[util] val nonIsoWpCodes = Map(
        "commons" -> "en",       // commons uses en, mostly
        "war" -> "tl",           // Waray-Waray language
        "new" -> "ne",           // Newar / Nepal Bhasa
        "simple" -> "en",        // simple English
        "roa-rup" -> "ro",       // Aromanian
        "ceb" -> "tl",           // Cebuano
        "sh" -> "hr",            // Serbo-Croatian (could also be sr)
        "pms" -> "it",           // Piedmontese
        "be-x-old" -> "be",      // Belarusian (Taraskievica)
        "bpy" -> "bn",           // Bishnupriya Manipuri
        "ksh" -> "de",           // Ripuarian
        "lmo" -> "it",           // Lombard
        "nds" -> "de",           // Low Saxon
        "scn" -> "it",           // Sicilian
        "zh-yue" -> "zh",        // Cantonese
        "ast" -> "es",           // Asturian
        "nap" -> "it",           // Neapolitan
        "bat-smg" -> "lt",       // Samogitian
        "roa-tara" -> "it",      // Tarantino
        "vec" -> "it",           // Venetian
        "pnb" -> "pa",           // Western Panjabi
        "zh-min-nan" -> "zh",    // Minnan
        "pam" -> "tl",           // Kapampangan
        "sah" -> "ru",           // Sakha
        "als" -> "sq",           // Tosk Albanian
        "arz" -> "ar",           // Egyptian Arabic
        "nah" -> "es",           // Nahuatl
        "hsb" -> "pl",           // Upper Sorbian
        "glk" -> "fa",           // Gilaki
        "gan" -> "zh",           // Gan Chinese
        "bcl" -> "tl",           // Central Bicolano
        "fiu-vro" -> "et",       // Voro
        "nds-nl" -> "de",        // Dutch Low Saxon
        "vls" -> "nl",           // West Flemish
        "sco" -> "en",           // Scots
        "bar" -> "de",           // Bavarian
        "nrm" -> "fr",           // Norman
        "pag" -> "tl",           // Pangasinan
        "map-bms" -> "jv",       // Banyumasan
        "diq" -> "tr",           // Zazaki
        "ckb" -> "ku",           // Sorani
        "wuu" -> "zh",           // Wu Chinese
        "mzn" -> "fa",           // Mazandarani
        "fur" -> "it",           // Friulian
        "lij" -> "it",           // Ligurian
        "nov" -> "ia",           // Novial
        "csb" -> "pl",           // Kashubian
        "ilo" -> "tl",           // Ilokano
        "zh-classical" -> "zh",  // Classical Chinese
        "lad" -> "he",           // Judaeo-Spanish
        "ang" -> "en",           // Anglo-Saxon / Old English
        "cbk-zam" -> "es",       // Zamboanga Chavacano
        "frp" -> "it",           // Franco-Provencal
        "hif" -> "hi",           // Fiji Hindi
        "hak" -> "zh",           // Hakka Chinese
        "xal" -> "ru",           // Kalmyk
        "pdc" -> "de",           // Pennsylvania German
        "szl" -> "pl",           // Silesian
        "haw" -> "en",           // Hawaiian
        "stq" -> "de",           // Saterland Frisian
        "crh" -> "tr",           // Crimean Tatar
        "ace" -> "id",           // Acehnese
        "myv" -> "ru",           // Erzya
        "krc" -> "ru",           // Karachay-Balkar
        "ext" -> "es",           // Extremaduran
        "mhr" -> "ru",           // Mari
        "arc" -> "tr",           // Assyrian Neo-Aramaic
        "eml" -> "it",           // Emilian-Romagnol
        "jbo" -> "en",           // Lojban
        "pcd" -> "fr",           // Picard
        "kab" -> "ar",           // Kabyle
        "frr" -> "de",           // North Frisian
        "tpi" -> "en",           // Tok Pisin
        "pap" -> "pt",           // Papiamento
        "zea" -> "nl",           // Zeelandic
        "srn" -> "nl",           // Sranan Tongo
        "udm" -> "ru",           // Udmurt
        "dsb" -> "pl",           // Lower Sorbian
        "tum" -> "ny",           // Tumbuka
        "rmy" -> "ro",           // Romani
        "mwl" -> "pt",           // Mirandese
        "mdf" -> "ru",           // Moksha
        "kaa" -> "uz",           // Karakalpak
        "tet" -> "id",           // Tetum
        "got" -> "it",           // Gothic
        "pih" -> "en",           // Norfuk
        "pnt" -> "el",           // Pontic Greek
        "chr" -> "en",           // Cherokee
        "cdo" -> "zh",           // Min Dong
        "bug" -> "id",           // Buginese
        "bxr" -> "ru",           // Buryat
        "lbe" -> "ru",           // Lak
        "chy" -> "en",           // Cheyenne
        "cho" -> "en",           // Choctaw
        "mus" -> "en",           // Muscogee / Creek
        "nan" -> "zh"            // redirect to zh-min-nan
    )
    
    val Default = forCode("en")
    
    /**
     * Gets a language object for a Wikipedia language code. For the Locale, this method uses 
     * the given code if it is an ISO code or a different code if there is an ISO code defined 
     * for the given code. Returns None if the given code neither is an ISO code nor has a defined ISO code.
     * See: http://s23.org/wikistats/wikipedias_html.php (and http://en.wikipedia.org/wiki/List_of_Wikipedias)
     * Throws IllegalArgumentException if language code is unknown.
     */
    def forCode( code : String ) : Language = tryCode(code).getOrElse(throw new IllegalArgumentException("unknown language code "+code))
    
    /**
     * Gets a language object for a Wikipedia language code. For the Locale, this method uses 
     * the given code if it is an ISO code or a different code if there is an ISO code defined 
     * for the given code. Returns None if the given code neither is an ISO code nor has a defined ISO code.
     * See: http://s23.org/wikistats/wikipedias_html.php (and http://en.wikipedia.org/wiki/List_of_Wikipedias)
     */
    def tryCode( code : String ) : Option[Language] =
    {
        // first convert wiki code to iso code, or just use the wiki code if no mapping is defined
        val isoCode = nonIsoWpCodes.get(code).getOrElse(code)
        // now check that it actually is an iso code
        if (isoCodes.contains(isoCode)) Some(new Language(code, isoCode))
        else None
    }

}

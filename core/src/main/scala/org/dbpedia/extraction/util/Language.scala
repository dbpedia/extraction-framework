package org.dbpedia.extraction.util

import java.util.Locale

/**
 * Represents a Wikipedia language. For each language, there is only one instance of this class. 
 */
class Language private(val wikiCode : String, val isoCode: String) extends Serializable
{
    // TODO: make this transient and add a readObject method
    val locale : Locale = new Locale(isoCode)
    
    /** 
     * Note that Wikipedia dump files use this prefix (with underscores), 
     * but Wikipedia domains use the wikiCode (with dashes), e.g. http://be-x-old.wikipedia.org
     */
    val filePrefix = wikiCode.replace("-", "_")
    
    /**
     */
    override def toString() = "wiki="+wikiCode+",locale="+locale.getLanguage
    
    // no need to override equals() and hashCode() - there is only one object for each value, so equality means identity. 
}

object Language extends (String => Language)
{
    val Values =
    {
      val languages = new collection.mutable.HashMap[String,Language]
      
      // Locale defines some languages that Wikipedia doesn't know, so we remove them.
      // TODO: get list of language codes from a source like these:
      // http://noc.wikimedia.org/conf/langlist
      // http://noc.wikimedia.org/conf/all.dblist
      val isoCodes = Locale.getISOLanguages.toSet &~ Set("nd")
      
      // Maps Wikipedia language codes which do not follow ISO-639-1, to a related ISO-639-1 code.
      // See: http://s23.org/wikistats/wikipedias_html.php (and http://en.wikipedia.org/wiki/List_of_Wikipedias)
      // Mappings are mostly based on similarity of the languages and in some cases on the regions where a related language is spoken.
      // See NonIsoLanguagesMappingTest and run it regularly. As of 2012-03-28, there are 11 wiki codes we haven't mapped yet.
      val nonIsoCodes = Map(
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
          "nds-nl" -> "de",        // Dutch Low Saxon  --- should probably map to nl
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
          "got" -> "it",           // Gothic  --- Italian???
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
          "nan" -> "zh",           // redirect to zh-min-nan
          "xmf" -> "ka",
          "rue" -> "uk",
          "pfl" -> "de",
          "vep" -> "fi",
          "nso" -> "st",
          "epo" -> "eo",
          "dk"  -> "da",
          "cz"  -> "cs",
          "ltg" -> "lv",
          "gag" -> "tr",
          "bjn" -> "id",
          "zh-cfr" -> "zh",
          "lez" -> "ru",
          "mrj" -> "ru",
          "jp"  -> "ja",
          "kbd" -> "ru",
          "minnan" -> "zh",
          "koi" -> "ru"
      )
      
      for (iso <- isoCodes) languages(iso) = new Language(iso, iso)

      // We could throw an exception if the mapped ISO code is not in the set of ISO codes, but then 
      // this class (and thus the whole system) wouldn't load, and that set may change depending on 
      // JDK version, and the affected wiki code may not even be used. Just silently ignore it. 
      // TODO: let this loop build a list of codes with bad mappings and throw the exception later.
      for ((code, iso) <- nonIsoCodes; if (isoCodes.contains(iso))) languages(code) = new Language(code, iso)
      
      languages.toMap // toMap makes immutable
    }
    
    // TODO: remove this. It is too often used in error.
    val Default = Values("en")
    
    /**
     * Gets a language object for a Wikipedia language code.
     * Throws IllegalArgumentException if language code is unknown.
     */
    def apply( code : String ) : Language = Values.getOrElse(code, throw new IllegalArgumentException("unknown language code "+code))
    
    /**
     * Gets a language object for a Wikipedia language code, or None if given code is unknown.
     */
    def get(code : String) : Option[Language] = Values.get(code)
    
    /**
     * Gets a language object for a Wikipedia language code, or the default if the given code is unknown.
     */
    def getOrElse(code : String, default : => Language) : Language = Values.getOrElse(code, default)
}

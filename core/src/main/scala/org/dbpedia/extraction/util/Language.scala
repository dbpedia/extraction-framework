package org.dbpedia.extraction.util

import java.util.Locale
import scala.collection.mutable.HashMap
import org.dbpedia.extraction.ontology.DBpediaNamespace

/**
 * Represents a MediaWiki instance and the language used on it. Initially, this class was
 * only used for xx.wikipedia.org instances, but now we also use it for mappings.dbpedia.org.
 * For each language, there is only one instance of this class.
 * TODO: rename this class to WikiCode or so.
 */
class Language private(val wikiCode : String, val isoCode: String)
{
    val locale = new Locale(isoCode)
    
    /** 
     * Note that Wikipedia dump files use this prefix (with underscores), e.g. be_x_old,
     * but Wikipedia domains use the wikiCode (with dashes), e.g. http://be-x-old.wikipedia.org
     */
    val filePrefix = wikiCode.replace("-", "_")
    
    /**
     * Specific DBpedia domain for this language, e.g. "en.dbpedia.org"
     */
    val dbpediaDomain = wikiCode+".dbpedia.org"
    
    /**
     * Specific DBpedia base URI for this language, e.g. "http://en.dbpedia.org"
     */
    val dbpediaUri = "http://"+dbpediaDomain
    
    /**
     * Specific DBpedia resource namespace for this language, e.g. "http://en.dbpedia.org/resource/"
     */
    val resourceUri = new DBpediaNamespace(dbpediaUri+"/resource/")
    
    /**
     * Specific DBpedia property namespace for this language, e.g. "http://en.dbpedia.org/property/"
     */
    val propertyUri = new DBpediaNamespace(dbpediaUri+"/property/")
    
    /**
     * URI prefix for this wiki, e.g. "http://be-x-old.wikipedia.org", "http://commons.wikimedia.org",
     * "http://mappings.dbpedia.org".
     */
    // TODO: this should not be hard-coded.
    val baseUri = wikiCode match {
      case "commons" => "http://commons.wikimedia.org"
      case "mappings" => "http://mappings.dbpedia.org"
      case _ => "http://"+wikiCode+".wikipedia.org"
    }
    
    /**
     * API URI for this wiki, e.g. "http://be-x-old.wikipedia.org/w/api.php", "http://commons.wikimedia.org/w/api.php",
     * "http://mappings.dbpedia.org/api.php".
     */
    // TODO: this should not be hard-coded.
    val apiUri = wikiCode match {
      case "mappings" => baseUri+"/api.php"
      case _ => baseUri+"/w/api.php"
    }
    
    /**
     */
    override def toString = "wiki="+wikiCode+",locale="+locale.getLanguage
    
    // no need to override equals() and hashCode() - there is only one object for each value, so equality means identity. 
}

object Language extends (String => Language)
{
  implicit val wikiCodeOrdering = Ordering.by[Language, String](_.wikiCode)
  
  val map: Map[String, Language] = locally {
    
    def language(code : String, iso: String): Language = new Language(code, iso)
    
    val languages = new HashMap[String,Language]
    
    // All two-letter codes from http://noc.wikimedia.org/conf/langlist as of 2012-04-15,
    // minus the redirected codes cz,dk,jp,sh (they are in the nonIsoCodes map below)
    // TODO: Automate this process. Or rather, download this list dynamically. Don't generate code.
    val isoCodes = Set(
      "aa","ab","af","ak","am","an","ar","as","av","ay","az","ba","be","bg","bh","bi","bm","bn",
      "bo","br","bs","ca","ce","ch","co","cr","cs","cu","cv","cy","da","de","dv","dz","ee","el",
      "en","eo","es","et","eu","fa","ff","fi","fj","fo","fr","fy","ga","gd","gl","gn","gu","gv",
      "ha","he","hi","ho","hr","ht","hu","hy","hz","ia","id","ie","ig","ii","ik","io","is","it",
      "iu","ja","jv","ka","kg","ki","kj","kk","kl","km","kn","ko","kr","ks","ku","kv","kw","ky",
      "la","lb","lg","li","ln","lo","lt","lv","mg","mh","mi","mk","ml","mn","mo","mr","ms","mt",
      "my","na","nb","ne","ng","nl","nn","no","nv","ny","oc","om","or","os","pa","pi","pl","ps",
      "pt","qu","rm","rn","ro","ru","rw","sa","sc","sd","se","sg","si","sk","sl","sm","sn","so",
      "sq","sr","ss","st","su","sv","sw","ta","te","tg","th","ti","tk","tl","tn","to","tr","ts",
      "tt","tw","ty","ug","uk","ur","uz","ve","vi","vo","wa","wo","xh","yi","yo","za","zh","zu"
    )
    
    // Maps Wikipedia language codes which do not follow ISO-639-1, to a related ISO-639-1 code.
    // See: http://s23.org/wikistats/wikipedias_html.php (and http://en.wikipedia.org/wiki/List_of_Wikipedias)
    // Mappings are mostly based on similarity of the languages and in some cases on the regions where a related language is spoken.
    // See NonIsoLanguagesMappingTest and run it regularly.
    // TODO: move these to a config file
    val nonIsoCodes = Map(
      "ace" -> "id",           // Acehnese
      "als" -> "sq",           // Tosk Albanian
      "ang" -> "en",           // Anglo-Saxon / Old English
      "arc" -> "tr",           // Assyrian Neo-Aramaic
      "arz" -> "ar",           // Egyptian Arabic
      "ast" -> "es",           // Asturian
      "bar" -> "de",           // Bavarian
      "bat-smg" -> "lt",       // Samogitian
      "bcl" -> "tl",           // Central Bicolano
      "be-x-old" -> "be",      // Belarusian (Taraskievica)
      "bjn" -> "id",
      "bpy" -> "bn",           // Bishnupriya Manipuri
      "bug" -> "id",           // Buginese
      "bxr" -> "ru",           // Buryat
      "cbk-zam" -> "es",       // Zamboanga Chavacano
      "cdo" -> "zh",           // Min Dong
      "ceb" -> "tl",           // Cebuano
      "cho" -> "en",           // Choctaw
      "chr" -> "en",           // Cherokee
      "chy" -> "en",           // Cheyenne
      "ckb" -> "ku",           // Sorani
      "commons" -> "en",       // commons uses en, mostly
      "crh" -> "tr",           // Crimean Tatar
      "csb" -> "pl",           // Kashubian
      "cz"  -> "cs",
      "diq" -> "tr",           // Zazaki
      "dk"  -> "da",
      "dsb" -> "pl",           // Lower Sorbian
      "eml" -> "it",           // Emilian-Romagnol
      "epo" -> "eo",
      "ext" -> "es",           // Extremaduran
      "fiu-vro" -> "et",       // Voro
      "frp" -> "it",           // Franco-Provencal
      "frr" -> "de",           // North Frisian
      "fur" -> "it",           // Friulian
      "gag" -> "tr",
      "gan" -> "zh",           // Gan Chinese
      "glk" -> "fa",           // Gilaki
      "got" -> "it",           // Gothic --- Italian???
      "hak" -> "zh",           // Hakka Chinese
      "haw" -> "en",           // Hawaiian
      "hif" -> "hi",           // Fiji Hindi
      "hsb" -> "pl",           // Upper Sorbian
      "ilo" -> "tl",           // Ilokano
      "jbo" -> "en",           // Lojban
      "jp"  -> "ja",
      "kaa" -> "uz",           // Karakalpak
      "kab" -> "ar",           // Kabyle
      "kbd" -> "ru",
      "koi" -> "ru",
      "krc" -> "ru",           // Karachay-Balkar
      "ksh" -> "de",           // Ripuarian
      "lad" -> "he",           // Judaeo-Spanish
      "lbe" -> "ru",           // Lak
      "lez" -> "ru",
      "lij" -> "it",           // Ligurian
      "lmo" -> "it",           // Lombard
      "ltg" -> "lv",
      "map-bms" -> "jv",       // Banyumasan
      "mappings" -> "en",      // mappings wiki uses en, mostly
      "mdf" -> "ru",           // Moksha
      "mhr" -> "ru",           // Mari
      "minnan" -> "zh",
      "mrj" -> "ru",
      "mus" -> "en",           // Muscogee / Creek
      "mwl" -> "pt",           // Mirandese
      "myv" -> "ru",           // Erzya
      "mzn" -> "fa",           // Mazandarani
      "nah" -> "es",           // Nahuatl
      "nan" -> "zh",           // redirect to zh-min-nan
      "nap" -> "it",           // Neapolitan
      "nds" -> "de",           // Low Saxon
      "nds-nl" -> "de",        // Dutch Low Saxon  --- should probably map to nl
      "new" -> "ne",           // Newar / Nepal Bhasa
      "nov" -> "ia",           // Novial
      "nrm" -> "fr",           // Norman
      "nso" -> "st",
      "pag" -> "tl",           // Pangasinan
      "pam" -> "tl",           // Kapampangan
      "pap" -> "pt",           // Papiamento
      "pcd" -> "fr",           // Picard
      "pdc" -> "de",           // Pennsylvania German
      "pfl" -> "de",
      "pih" -> "en",           // Norfuk
      "pms" -> "it",           // Piedmontese
      "pnb" -> "pa",           // Western Panjabi
      "pnt" -> "el",           // Pontic Greek
      "rmy" -> "ro",           // Romani
      "roa-rup" -> "ro",       // Aromanian
      "roa-tara" -> "it",      // Tarantino
      "rue" -> "uk",
      "sah" -> "ru",           // Sakha
      "scn" -> "it",           // Sicilian
      "sco" -> "en",           // Scots
      "sh" -> "hr",            // Serbo-Croatian (could also be sr)
      "simple" -> "en",        // simple English
      "srn" -> "nl",           // Sranan Tongo
      "stq" -> "de",           // Saterland Frisian
      "szl" -> "pl",           // Silesian
      "tet" -> "id",           // Tetum
      "tpi" -> "en",           // Tok Pisin
      "tum" -> "ny",           // Tumbuka
      "udm" -> "ru",           // Udmurt
      "vec" -> "it",           // Venetian
      "vep" -> "fi",
      "vls" -> "nl",           // West Flemish
      "war" -> "tl",           // Waray-Waray language
      "wuu" -> "zh",           // Wu Chinese
      "xal" -> "ru",           // Kalmyk
      "xmf" -> "ka",
      "zea" -> "nl",           // Zeelandic
      "zh-cfr" -> "zh",
      "zh-classical" -> "zh",  // Classical Chinese
      "zh-min-nan" -> "zh",    // Minnan
      "zh-yue" -> "zh"         // Cantonese
    )
    
    for (iso <- isoCodes) languages(iso) = language(iso, iso)

    // We could throw an exception if the mapped ISO code is not in the set of ISO codes, but then 
    // this class (and thus the whole system) wouldn't load, and that set may change depending on 
    // JDK version, and the affected wiki code may not even be used. Just silently ignore it. 
    // TODO: let this loop build a list of codes with bad mappings and throw the exception later.
    for ((code, iso) <- nonIsoCodes) if (isoCodes.contains(iso)) languages(code) = language(code, iso)
    
    languages.toMap // toMap makes immutable
  }
  
  /**
   * English Wikipedia
   */
  val English = map("en")
  
  /**
   * DBpedia mappings wiki
   */
  val Mappings = map("mappings")
  
  /**
   * Wikimedia commons
   */
  val Commons = map("commons")
  
  /**
   * Gets a language object for a Wikipedia language code.
   * Throws IllegalArgumentException if language code is unknown.
   */
  def apply(code: String) : Language = map.getOrElse(code, throw new IllegalArgumentException("unknown language code "+code))
  
  /**
   * Gets a language object for a Wikipedia language code, or None if given code is unknown.
   */
  def get(code: String) : Option[Language] = map.get(code)
  
  /**
   * Gets a language object for a Wikipedia language code, or the default if the given code is unknown.
   */
  def getOrElse(code: String, default: => Language) : Language = map.getOrElse(code, default)
}

package org.dbpedia.extraction.config.mappings


object HomepageExtractorConfig
{
    //TODO rewritten as map, need to clean up per language
    //private val propertyNames = Set("website", "homepage", "webpräsenz", "web", "site", "siteweb", "site web", "ιστότοπος", "Ιστοσελίδα", "strona", "página", "sitio", "pagina", "сайт")
    // For "ar" configuration, rendering right-to-left may seems like a bug, but it's not.
    // Don't change this else if you know how it is done.

    private val propertyNamesMap = Map(
        "am" -> Set(
          "ድህረገፅ",
          "ድህረ_ገፅ",
          "ገጽ",
          "ድህረ ገጽ",
          "ድህረ_ገጽ",
          "ድረ_ገፅ",
          "ድረገፅ",
          "ድረገጽ",
          "ድረ ገጽ",
          "ድረ_ገጽ",
          "ዋና_ገጽ",
          "ዌብሳይት",
          "website",
          "web",
          "site"
        ),
        "ar" -> Set("الموقع", "الصفحة الرسمية", "موقع", "الصفحة الرئيسية", "صفحة ويب", "موقع ويب"),
        "bg" -> Set("сайт", "уебсайт"),
        "ca" -> Set("pàgina", "web", "lloc"),
        "cs" -> Set("Webová stránka", "Oficiální web"),
        "de" -> Set("website", "homepage", "webpräsenz", "web", "site", "siteweb", "site web"),/*cleanup*/
        "el" -> Set("ιστότοπος", "ιστοσελίδα"),
        "en" -> Set("website", "homepage", "web", "site"),
        "eo" -> Set("ĉefpaĝo", "retejo"),
        "es" -> Set("website", "homepage", "web", "site", "siteweb", "site web", "página", "sitio", "pagina"),/*cleanup*/
        "eu" -> Set("webgunea"),
        "fr" -> Set("website", "homepage", "web", "site", "siteweb", "site web"),/*cleanup*/
        "ga" -> Set("suíomh"),
        "it" -> Set("homepage", "sito", "sito web"),
        "ja" -> Set("homepage", "website", "web", "siteweb", "HP", "ホームページ", "ウェブ", "サイト", "ウェブサイト", "公式サイト"),
        "mk" -> Set("Портал", "Мреж. место"),
        "nl" -> Set("website", "homepage", "hoofdpagina", "webpagina", "web", "site"),
        "pl" -> Set("web", "strona"),
        "pt" -> Set("website", "homepage", "web", "site", "siteweb", "site web", "página", "sitio", "pagina"),/*cleanup*/
        "ru" -> Set("сайт"),
        "uk" -> Set("веб-сайт", "домашня сторінка", "сайт")
    )

    def propertyNames(lang : String) : Set[String] = {
        propertyNamesMap.getOrElse(lang, Set())
    }

    val supportedLanguages = propertyNamesMap.keySet

    private val externalLinkSectionsMap = Map(
        "am" -> "(?:የውጭ ንባብ|የውጭ ማያያዣ)",
        "ar" -> "وصلات خارجية",
        "bg" -> "Външни препратки",
        "ca" -> "(?:Enllaços externs|Enllaço extern)",
        "cs" -> "Odkazy",
        "de" -> "Weblinks?",
        "el" -> "(?:Εξωτερικοί σύνδεσμοι|Εξωτερικές συνδέσεις)",
        "en" -> "External links?",
        "eo" -> "Eksteraj ligiloj",
        "es" -> "(?:Enlaces externos|Enlace externo|Links externos|Link externo)",
        "eu" -> "Kanpo loturak?",
        "fr" -> "(?:Lien externe|Liens externes|Liens et documents externes)",
        "ga" -> "(?:Naisc sheachtracha|Nasc sheachtrach)",
        "it" -> "Collegamenti esterni",
        "ja" -> "外部リンク",
        "mk" -> "Надворешни врски",
        "nl" -> "(?:Externe links|Externe link)",
        "pl" -> "(?:Linki zewnętrzne|Link zewnętrzny)",
        "pt" -> "(?:Ligações externas|Ligação externa|Links externos|Link externo)",
        "ru" -> "Ссылки",
        "uk" -> "Посилання"
    )

    def externalLinkSections(lang : String) : String = {
        externalLinkSectionsMap.getOrElse(lang, "")
    }

    private val officialMap = Map(
        "am" -> "ዋና",
        "ar" -> "رسمي",
        "bg" -> "официален",
        "ca" -> "oficial",
        "cs" -> "oficiální",
        "de" -> "offizielle",
        "el" -> "(?:επίσημος|επίσημη)",
        "en" -> "official",
        "eo" -> "oficiala",
        "es" -> "oficial",
        "eu" -> "ofiziala?",
        "fr" -> "officiel",
        "ga" -> "oifigiúil",
        "it" -> "ufficiale",
        "ja" -> "(?:公式|オフィシャル)",
        "mk" -> "официјален",
        "nl" -> "(?:officieel|officiële)",
        "pl" -> "oficjalna",
        "pt" -> "oficial",
        "ru" -> "официальный",
        "uk" -> "офіційний"
    )

    def official(lang : String) : String = {
        officialMap.getOrElse(lang, "")
    }

    // Map(language -> Map(templateName -> templatePropertyKey))
    private val templateOfficialWebsiteMap = Map(
        "ca" -> Map("Oficial" -> "1"),
        "bg" -> Map("Официален сайт" -> "1"),
        /* "it" -> Map("Sito Ufficiale" -> "1"), This does not exist, yet */
        "el" -> Map("Επίσημη ιστοσελίδα" -> "1"),
        "en" -> Map("Official website" -> "1"),
        "eo" -> Map("Oficiala_retejo" -> "1"),
        "es" -> Map("Página_web" -> "1"),
        "fr" -> Map("Site_officiel" -> "url"),
        "ga" -> Map("Páxina_web" -> "1"),
        "ja" -> Map("Official website" -> "1"),
        "pt" -> Map("Oficial" -> "1"),
        "ru" -> Map("Официальный сайт" -> "1"),
        "uk" -> Map("Official" -> "1")
    )

    def templateOfficialWebsite(lang : String) : Map[String, String] = {
        templateOfficialWebsiteMap.getOrElse(lang, Map())
    }

}

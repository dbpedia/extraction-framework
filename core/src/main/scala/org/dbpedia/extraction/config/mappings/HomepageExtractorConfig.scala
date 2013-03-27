package org.dbpedia.extraction.config.mappings


object HomepageExtractorConfig
{
    //TODO rewritten as map, need to clean up per language
    //private val propertyNames = Set("website", "homepage", "webpräsenz", "web", "site", "siteweb", "site web", "ιστότοπος", "Ιστοσελίδα", "strona", "página", "sitio", "pagina", "сайт")
    // For "ar" configuration, rendering right-to-left may seems like a bug, but it's not.
    // Don't change this else if you know how it is done.

    val propertyNamesMap = Map(
        "ar" -> Set("الموقع", "الصفحة الرسمية", "موقع", "الصفحة الرئيسية", "صفحة ويب", "موقع ويب"),
        "ca" -> Set("pàgina", "web", "lloc"),
        "de" -> Set("website", "homepage", "webpräsenz", "web", "site", "siteweb", "site web"),/*cleanup*/
        "el" -> Set("ιστότοπος", "ιστοσελίδα"),
        "en" -> Set("website", "homepage", "web", "site"),
        "es" -> Set("website", "homepage", "web", "site", "siteweb", "site web", "página", "sitio", "pagina"),/*cleanup*/
        "eu" -> Set("webgunea"),
        "fr" -> Set("website", "homepage", "web", "site", "siteweb", "site web"),/*cleanup*/
        "ga" -> Set("suíomh"),
        "it" -> Set("homepage", "sito", "sito web"),
        "nl" -> Set("website", "homepage", "hoofdpagina", "webpagina", "web", "site"),
        "pl" -> Set("web", "strona"),
        "pt" -> Set("website", "homepage", "web", "site", "siteweb", "site web", "página", "sitio", "pagina"),/*cleanup*/
        "ru" -> Set("сайт")
    )

    val supportedLanguages = propertyNamesMap.keySet

    val externalLinkSectionsMap = Map(
        "ar" -> "وصلات خارجية",
        "ca" -> "(?:Enllaços externs|Enllaço extern)",
        "de" -> "Weblinks?",
        "el" -> "(?:Εξωτερικοί σύνδεσμοι|Εξωτερικές συνδέσεις)",
        "en" -> "External links?",
        "es" -> "(?:Enlaces externos|Enlace externo|Links externos|Link externo)",
        "eu" -> "Kanpo loturak?",
        "fr" -> "(?:Lien externe|Liens externes|Liens et documents externes)",
        "ga" -> "(?:Naisc sheachtracha|Nasc sheachtrach)",
        "it" -> "Collegamenti esterni",
        "nl" -> "(?:Externe links|Externe link)",
        "pl" -> "(?:Linki zewnętrzne|Link zewnętrzny)",
        "pt" -> "(?:Ligações externas|Ligação externa|Links externos|Link externo)",
        "ru" -> "Ссылки"
    )

    val officialMap = Map(
        "ar" -> "رسمي",
        "ca" -> "oficial",
        "de" -> "offizielle",
        "el" -> "(?:επίσημος|επίσημη)",
        "en" -> "official",
        "es" -> "oficial",
        "eu" -> "ofiziala?",
        "fr" -> "officiel",
        "ga" -> "oifigiúil",
        "it" -> "ufficiale",
        "nl" -> "(?:officieel|officiële)",
        "pl" -> "oficjalna",
        "pt" -> "oficial",
        "ru" -> "официальный"
    )

}

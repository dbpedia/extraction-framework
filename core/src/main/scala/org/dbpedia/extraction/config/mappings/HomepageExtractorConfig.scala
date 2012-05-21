package org.dbpedia.extraction.config.mappings


object HomepageExtractorConfig
{
    //TODO rewritten as map, need to clean up per language
    //private val propertyNames = Set("website", "homepage", "webpräsenz", "web", "site", "siteweb", "site web", "ιστότοπος", "Ιστοσελίδα", "strona", "página", "sitio", "pagina", "сайт")
    val propertyNamesMap = Map(
        "ca" -> Set("pàgina", "web", "lloc"),
        "de" -> Set("website", "homepage", "webpräsenz", "web", "site", "siteweb", "site web"),/*cleanup*/
        "el" -> Set("ιστότοπος", "ιστοσελίδα"),
        "en" -> Set("website", "homepage", "web", "site"),
        "es" -> Set("website", "homepage", "web", "site", "siteweb", "site web", "página", "sitio", "pagina"),/*cleanup*/
        "eu" -> Set("webgunea"),
        "fr" -> Set("website", "homepage", "web", "site", "siteweb", "site web"),/*cleanup*/
        "ga" -> Set("suíomh"),
        "it" -> Set("homepage", "sito", "sito web"),
        "pl" -> Set("web", "strona"),
        "pt" -> Set("website", "homepage", "web", "site", "siteweb", "site web", "página", "sitio", "pagina"),/*cleanup*/
        "ru" -> Set("сайт")
    )

    val supportedLanguages = propertyNamesMap.keySet

    val externalLinkSectionsMap = Map(
        "ca" -> "(?:Enllaços externs|Enllaço extern)",
        "de" -> "Weblinks?",
        "el" -> "(?:Εξωτερικοί σύνδεσμοι|Εξωτερικές συνδέσεις)",
        "en" -> "External links?",
        "es" -> "(?:Enlaces externos|Enlace externo|Links externos|Link externo)",
        "eu" -> "Kanpo loturak?",
        "fr" -> "(?:Lien externe|Liens externes|Liens et documents externes)",
        "ga" -> "(?:Naisc sheachtracha|Nasc sheachtrach)",
        "it" -> "Collegamenti esterni",
        "pl" -> "(?:Linki zewnętrzne|Link zewnętrzny)",
        "pt" -> "(?:Ligações externas|Ligação externa|Links externos|Link externo)",
        "ru" -> "Ссылки"
    )

    val officialMap = Map(
        "ca" -> "oficial",
        "de" -> "offizielle",
        "el" -> "(?:επίσημος|επίσημη)",
        "en" -> "official",
        "es" -> "oficial",
        "eu" -> "ofiziala?",
        "fr" -> "officiel",
        "ga" -> "oifigiúil",
        "it" -> "ufficiale",
        "pl" -> "oficjalna",
        "pt" -> "oficial",
        "ru" -> "официальный"
        // TODO: ru is from http://translate.google.com/#en|ru|official%20homepage - check if it's correct
    )

}

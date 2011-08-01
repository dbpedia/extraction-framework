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
        "fr" -> Set("website", "homepage", "web", "site", "siteweb", "site web"),/*cleanup*/
        "ga" -> Set("suíomh"),
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
        "pl" -> "(?:Linki zewnętrzne|Link zewnętrzny)",
        "pt" -> "(?:Ligações externas|Ligação externa|Links externos|Link externo)",
        "ru" -> "Ссылки"
    )

    val officialMap = Map(
        "ca" -> "oficial",
        "en" -> "official",
        "de" -> "offizielle",
        "el" -> "(?:επίσημος|επίσημη)",
        "eu" -> "ofiziala?",
        "ga" -> "oifigiúil",
        "fr" -> "officiel",
        "pl" -> "oficjalna",
        "pt" -> "oficial",
        "es" -> "oficial"
    )

}
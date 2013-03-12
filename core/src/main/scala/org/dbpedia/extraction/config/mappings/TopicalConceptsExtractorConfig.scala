package org.dbpedia.extraction.config.mappings

object TopicalConceptsExtractorConfig
{

    //FIXME name conflict between categories might exist between languages. add language-specific maps
    val catMainTemplates = Set("Cat main",
                               "Artigo principal", // for pt.wikipedia.org
                               "AP",               // for es.wikipedia.org
                               "Article principal",// for fr.wikipedia.org
                               "Catmore",          // for el.wikipedia.org
                               "Nagusia",          // for eu.wikipedia.org
                               "Основная статья по теме категории", "Catmain", //for ru.wikipedia.org
                               "Infocat", "Infocatm" // for ca.wikipedia.org
                            )
}

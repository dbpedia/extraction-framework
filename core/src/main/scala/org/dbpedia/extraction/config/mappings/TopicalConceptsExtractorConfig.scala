package org.dbpedia.extraction.config.mappings

object TopicalConceptsExtractorConfig
{

    //FIXME name conflict between categories might exist between languages. add language-specific maps
    val catMainTemplates = Set("Cat main",
                        // For "ar" configuration, rendering right-to-left may seems like a bug, but it's not.
                        // Don't change this else if you know how it is done.
                                    "مزيد"   ,// for ar.wikipedia.org
                                "Artigo principal", // for pt.wikipedia.org
                                "AP",               // for es.wikipedia.org
                                "Article principal",// for fr.wikipedia.org
                                "Catmore",          // for el.wikipedia.org
                                "Nagusia",          // for eu.wikipedia.org
                                "Основная статья по теме категории", "Catmain", //for ru.wikipedia.org
                                "Infocat", "Infocatm" // for ca.wikipedia.org
                            )

}

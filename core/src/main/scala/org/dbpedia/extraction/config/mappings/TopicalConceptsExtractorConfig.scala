package org.dbpedia.extraction.config.mappings

object TopicalConceptsExtractorConfig
{

    //FIXME name conflict between categories might exist between languages. add language-specific maps
    // For "ar" configuration, rendering right-to-left may seems like a bug, but it's not.
    // Don't change this else if you know how it is done.

    val catMainTemplates = Set("Cat main",
      "مزيد"   ,// ar
                               "Infocat", "Infocatm", // ca
                               "Catmore",          // el
                               "AP",               // es
                               "Nagusia",          // eu
                               "Article principal",// fr
                               "Voce principale", "torna a",  // it
                               "Artigo principal", // pt
                               "Основная статья по теме категории", "Catmain" // ru
                            )
}

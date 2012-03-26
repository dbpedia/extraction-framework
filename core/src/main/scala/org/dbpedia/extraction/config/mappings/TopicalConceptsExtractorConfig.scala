package org.dbpedia.extraction.config.mappings

object TopicalConceptsExtractorConfig
{

    //FIXME name conflict between categories might exist between languages. add language-specific maps
    val catMainTemplates = Set("Cat_main",
                               "Artigo principal", // for pt.wikipedia.org
                               "AP"                // for es.wikipedia.org
                            )

}

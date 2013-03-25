package org.dbpedia.extraction.config.mappings


object PersondataExtractorConfig
{
    val supportedLanguages = Set("en", "de", "fr") 

    val persondataTemplates = Map("en" -> "persondata", "de" -> "personendaten", "fr" -> "métadonnées personne")
    val name = Map("en" -> "NAME", "de" -> "NAME", "fr" -> "NOM")
    val alternativeNames = Map("en" -> "ALTERNATIVE NAMES", "de" -> "ALTERNATIVNAMEN", "fr" -> "NOMS ALTERNATIFS")
    val description = Map("en" -> "SHORT DESCRIPTION", "de" -> "KURZBESCHREIBUNG", "fr" -> "COURTE DESCRIPTION")
    val birthDate = Map("en" -> "DATE OF BIRTH", "de" -> "GEBURTSDATUM", "fr" -> "DATE DE NAISSANCE")
    val birthPlace = Map("en" -> "PLACE OF BIRTH", "de" -> "GEBURTSORT", "fr" -> "LIEU DE NAISSANCE")
    val deathDate = Map("en" -> "DATE OF DEATH", "de" -> "STERBEDATUM", "fr" -> "DATE DE DÉCÈS")
    val deathPlace = Map("en" -> "PLACE OF DEATH", "de" -> "STERBEORT", "fr" -> "LIEU DE DÉCÈS")
}
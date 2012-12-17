package org.dbpedia.extraction.config.mappings


object PersondataExtractorConfig
{
  // The French template is not used anymore. See http://fr.wikipedia.org/wiki/Wikipédia:Sondage/Limitation_du_modèle_Métadonnées_personne
  
  val supportedLanguages = Set("en", "de") 
  val persondataTemplates = Map("en" -> "persondata", "de" -> "personendaten")
  val name = Map("en" -> "NAME", "de" -> "NAME")
  val alternativeNames = Map("en" -> "ALTERNATIVE NAMES", "de" -> "ALTERNATIVNAMEN")
  val description = Map("en" -> "SHORT DESCRIPTION", "de" -> "KURZBESCHREIBUNG")
  val birthDate = Map("en" -> "DATE OF BIRTH", "de" -> "GEBURTSDATUM")
  val birthPlace = Map("en" -> "PLACE OF BIRTH", "de" -> "GEBURTSORT")
  val deathDate = Map("en" -> "DATE OF DEATH", "de" -> "STERBEDATUM")
  val deathPlace = Map("en" -> "PLACE OF DEATH", "de" -> "STERBEORT")
}
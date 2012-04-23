package org.dbpedia.extraction.ontology

import org.dbpedia.extraction.util.Language

/**
 * This is the base class of OntologyClass and Datatype.
 *
 * @param name The name of this type
 * @param labels The labels of this type. Map: LanguageCode -> Label
 * @param comments Comments describing this type. Map: LanguageCode -> Comment
 */
abstract class OntologyType(name : String, labels : Map[Language, String], comments : Map[Language, String]) extends OntologyEntity(name, labels, comments)

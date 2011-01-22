package org.dbpedia.extraction.mappings

import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.sources.Source
import org.dbpedia.extraction.util.Language

// TODO: find a better solution to configure the extraction.
// Many users of this class only need one part of it - for example,
// the data parsers need the language but not the ontology or the
// redirects.
class ExtractionContext(val ontology : Ontology, val language : Language, val redirects : Redirects,
                        val mappingsSource : Source,  val commonsSource : Source, val articlesSource : Source)

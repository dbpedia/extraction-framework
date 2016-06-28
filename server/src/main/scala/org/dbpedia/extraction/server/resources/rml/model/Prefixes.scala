package org.dbpedia.extraction.server.resources.rml.model

/**
  * Contains the RML prefixes
  */
object Prefixes {

  val map = collection.immutable.HashMap(
    "rdf" -> "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    "rr" -> "http://www.w3.org/ns/r2rml#",
    "rml" -> "http://semweb.mmlab.be/ns/rml#",
    "ql" -> "http://semweb.mmlab.be/ns/ql#",
    "rdfs" -> "http://www.w3.org/2000/01/rdf-schema#",
    "skos" -> "http://www.w3.org/2004/02/skos/core#",
    "dbo" -> "http://dbpedia.org/ontology/",
    "foaf" -> "http://xmlns.com/foaf/0.1/",
    "fnml" -> "http://semweb.mmlab.be/ns/fnml#",
    "fno" -> "http://w3id.org/function/ontology/",
    "ex" -> "http://example.org/stuff/1.0/",
    "dbf" -> "http://dbpedia.org/function/",
    "dcterms" -> "http://purl.org/dc/terms/"
  )

  def apply(prefix: String) : String=
  {
    map(prefix)
  }

}

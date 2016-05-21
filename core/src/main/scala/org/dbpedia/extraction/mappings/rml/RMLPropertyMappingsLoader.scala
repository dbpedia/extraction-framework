package org.dbpedia.extraction.mappings.rml

import be.ugent.mmlab.rml.model.{PredicateObjectMap, TriplesMap}
import org.dbpedia.extraction.mappings.PropertyMapping
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import collection.JavaConverters._


/**
  * Loads property mappings of a single mapping (triples map)
  */
object RMLPropertyMappingsLoader {

  def loadPropertyMappings(triplesMap: TriplesMap, context:{def ontology: Ontology
                                                            def language: Language }) : List[PropertyMapping] =

  {

      var propertyMappings = List[PropertyMapping]()
      val predicateObjectMaps = triplesMap.getPredicateObjectMaps.asScala

      for (predicateObjectMap : PredicateObjectMap <- predicateObjectMaps) {
          propertyMappings ::= loadPropertyMapping(predicateObjectMap, context)
      }

      propertyMappings

  }

  def loadPropertyMapping(predicateObjectMap: PredicateObjectMap, context: {def ontology: Ontology
                                                                            def language: Language}) : PropertyMapping =
  {

    //TODO: implement
    null
  }
}

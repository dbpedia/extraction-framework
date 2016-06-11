package org.dbpedia.extraction.mappings.rml

import be.ugent.mmlab.rml.model.RDFTerm.PredicateMap
import be.ugent.mmlab.rml.model.{PredicateObjectMap, TriplesMap}
import org.dbpedia.extraction.mappings.{PropertyMapping, Redirects, SimplePropertyMapping}
import org.dbpedia.extraction.ontology.Ontology
import org.dbpedia.extraction.util.Language
import scala.language.reflectiveCalls
import collection.JavaConverters._


/**
  * Loads property mappings of a single mapping (triples map)
  */
object RMLPropertyMappingsLoader {

  /**
    * Loads all property mappings from a triples map
    */
  def loadPropertyMappings(triplesMap: TriplesMap, context:{def ontology: Ontology
                                                            def language: Language
                                                            def redirects: Redirects}) : List[PropertyMapping] =

  {

      var propertyMappings = List[PropertyMapping]()
      val predicateObjectMaps = triplesMap.getPredicateObjectMaps.asScala

      for (predicateObjectMap : PredicateObjectMap <- predicateObjectMaps) {
          propertyMappings ::= loadPropertyMapping(predicateObjectMap, context)
      }

      propertyMappings

  }

  /**
    * Loads property mapping
    */
  def loadPropertyMapping(predicateObjectMap: PredicateObjectMap, context: {def ontology: Ontology
                                                                            def language: Language
                                                                            def redirects: Redirects}) : PropertyMapping =
  {

    //TODO: Making a distinction between the type of mappings

    val predicateMap = predicateObjectMap.getPredicateMaps.asScala.head
    val objectMap = predicateObjectMap.getObjectMaps.asScala.head

    val templateProperty = objectMap.getReferenceMap.getReference
    val ontologyProperty = RMLOntologyUtil.loadOntologyPropertyFromIRI(predicateMap.getConstantValue.stringValue(), context)
    val dataType = RMLOntologyUtil.loadOntologyDataTypeFromIRI(ontologyProperty.range.name, context)

    new SimplePropertyMapping(templateProperty, ontologyProperty, null, null, null, null, dataType, context.language, 1, context)
  }

}

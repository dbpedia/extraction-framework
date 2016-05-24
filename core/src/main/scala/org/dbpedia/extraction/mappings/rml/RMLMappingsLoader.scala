package org.dbpedia.extraction.mappings.rml

import be.ugent.mmlab.rml.model.{RMLMapping, TriplesMap}
import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.ontology.{Ontology, OntologyClass, OntologyProperty}
import org.dbpedia.extraction.util.Language
import org.dbpedia.extraction.wikiparser.{TableNode, TemplateNode}

import collection.JavaConverters._
import scala.collection.immutable.List
import scala.collection.mutable.HashMap
import scala.collection.mutable.ArrayBuffer
import scala.language.reflectiveCalls

/**
  * Responsible for loading mappings from parsed RML documents
  */
object RMLMappingsLoader {

  /**
    * Loads mappings from an RMLMapping context
    */
  def load(context: {
              def ontology : Ontology
              def language : Language
              def redirects: Redirects
              def mappingDoc : RMLMapping }) : Mappings =
  {

      val templateMappings = new HashMap[String, Extractor[TemplateNode]]
      val tableMappings = new ArrayBuffer[Extractor[TableNode]] //now still empty

      val triplesMapCollection = context.mappingDoc.getTriplesMaps.asScala //get triplesmaps from mapping document
      for (triplesMap: TriplesMap <- triplesMapCollection) {
        templateMappings.put(triplesMap.getName().replaceAll(".*/", ""), loadTemplateMapping(triplesMap, context))
      }

      new Mappings(templateMappings.toMap,tableMappings.toList)

  }

  /**
    * Loads a template mapping from a triples map
    */
  private def loadTemplateMapping(triplesMap: TriplesMap, context : {
                                                            def ontology: Ontology
                                                            def language: Language
                                                            def redirects: Redirects}) : TemplateMapping =
  {

      val mapToClass: OntologyClass = RMLOntologyUtil.loadMapToClassOntology(triplesMap, context)
      val correspondingClass: OntologyClass = RMLOntologyUtil.loadCorrespondingClassOntology(triplesMap, context)
      val correspondingProperty: OntologyProperty = RMLOntologyUtil.loadCorrespondingPropertyOntology(triplesMap, context)
      val propertyMappingList: List[PropertyMapping] = RMLPropertyMappingsLoader.loadPropertyMappings(triplesMap, context)

      new TemplateMapping(mapToClass, correspondingClass, correspondingProperty, propertyMappingList, context)

  }

}

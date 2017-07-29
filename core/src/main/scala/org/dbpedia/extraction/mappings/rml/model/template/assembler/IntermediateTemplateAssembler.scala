package org.dbpedia.extraction.mappings.rml.model.template.assembler

import org.dbpedia.extraction.mappings.PropertyMapping
import org.dbpedia.extraction.mappings.rml.model.RMLModel
import org.dbpedia.extraction.mappings.rml.model.resource.{RMLLiteral, RMLPredicateObjectMap, RMLTriplesMap, RMLUri}
import org.dbpedia.extraction.mappings.rml.model.template.{GeocoordinateTemplate, IntermediateTemplate, Template}
import org.dbpedia.extraction.mappings.rml.model.template.assembler.TemplateAssembler.Counter
import org.dbpedia.extraction.mappings.rml.translate.mapper.RMLModelMapper

/**
  * Created by wmaroy on 29.07.17.
  */
class IntermediateTemplateAssembler(rmlModel: RMLModel, baseUri : String, language: String, template : IntermediateTemplate,  counter : Counter) {

  def assemble() : List[RMLPredicateObjectMap] = {
    addIntermediateNodeMapping()
  }

  def addIntermediateNodeMapping() : List[RMLPredicateObjectMap] =
  {

    val uri = RMLUri(baseUri +
      "/IntermediateNodeMapping/" +
      counter.intermediates)

    val intermediateNodePom = rmlModel.triplesMap.addPredicateObjectMap(uri)

    addIntermediateNodeMappingToPredicateObjectMap(intermediateNodePom)

    List(intermediateNodePom)

  }

  private def addIntermediateNodeMappingToPredicateObjectMap(intermediateNodePom: RMLPredicateObjectMap) =
  {


    intermediateNodePom.addPredicate(RMLUri(template.property.uri))

    val intermediateNodeObjectMapUri = intermediateNodePom.uri.extend("/ObjectMap")
    val interMediateNodeObjectMap = intermediateNodePom.addObjectMap(intermediateNodeObjectMapUri)

    val parentTriplesMapUri = intermediateNodeObjectMapUri.extend("/ParentTriplesMap")
    val parentTriplesMap = interMediateNodeObjectMap.addParentTriplesMap(parentTriplesMapUri)
    parentTriplesMap.addLogicalSource(rmlModel.logicalSource)

    val parentSubjectMap = parentTriplesMap.addSubjectMap(parentTriplesMapUri.extend("/SubjectMap"))
    parentSubjectMap.addClass(RMLUri(template.ontologyClass.uri))
    parentSubjectMap.addIRITermType()
    parentSubjectMap.addTemplate(new RMLLiteral("http://en.dbpedia.org/resource/{wikititle}__" + counter.intermediates))

    // ??? if(mapping.correspondingProperty != null) parentSubjectMap.addRMLReference(new RMLLiteral(mapping.correspondingProperty.uri))

    //create the intermediate mappings
    val pomList = template.templates.foldLeft((counter, List[RMLPredicateObjectMap]()))((bundle, template) => {
      val newBundle = addPropertyMapping(template, parentTriplesMap, bundle._1)
      (newBundle._1, bundle._2 ++ newBundle._2)
    })._2

    // add intermediate poms to parent triples map
    pomList.foreach(pom => {
      parentTriplesMap.addPredicateObjectMap(pom)
    })
  }

  private def addPropertyMapping(template: Template, triplesMap: RMLTriplesMap, counter : Counter) : (Counter, List[RMLPredicateObjectMap]) =
  {
    val bundle = TemplateAssembler.assembleTemplate(rmlModel, baseUri + "/IntermediateNodeMapping/" + counter.intermediates + "", template, language, counter, independent = true)
    bundle
  }



}

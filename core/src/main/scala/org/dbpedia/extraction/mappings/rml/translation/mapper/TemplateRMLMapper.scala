package org.dbpedia.extraction.mappings.rml.translation.mapper

import org.dbpedia.extraction.mappings._
import org.dbpedia.extraction.mappings.rml.translation.model.RMLModel
import org.dbpedia.extraction.mappings.rml.translation.model.rmlresources.{RMLLiteral, RMLPredicateObjectMap, RMLSubjectMap, RMLUri}

/**
  * Creates an RML Template Mapping
  */
class TemplateRMLMapper(rmlModel: RMLModel, templateMapping: TemplateMapping) {

  private val triplesMapLabel = rmlModel.wikiTitle.decodedWithNamespace + " (Triples Map)"
  private val triplesMapComment = "Main Triples Map of " + rmlModel.wikiTitle.decodedWithNamespace +
                                  ". This defines the generation of triples that are extracted from " +
                                  rmlModel.wikiTitle.decoded + "."

  private val subjectMapLabel = rmlModel.wikiTitle.decodedWithNamespace + " (Subject Map)"
  private val subjectMapComment = "Main Subject Map of " + rmlModel.wikiTitle.decodedWithNamespace +
                                  ". This defines the generation of the subject for the triples that are extracted from " +
                                  rmlModel.wikiTitle.decoded + "."

  def mapToModel() = {
    TemplateRMLMapper.resetState()
    defineTriplesMap() //sets details of the triples map
    addPropertyMappings()
  }

  private def defineTriplesMap() =
  {
    rmlModel.triplesMap.addDCTermsType(new RMLLiteral("templateMapping"))
    //rmlModel.triplesMap.addRdfsLabel(triplesMapLabel)
    //rmlModel.triplesMap.addRdfsComment(triplesMapComment)
    defineSubjectMap()
    defineLogicalSource()
  }

  private def defineSubjectMap() =
  {
    rmlModel.subjectMap.addTemplate(rmlModel.rmlFactory.createRMLLiteral("http://en.dbpedia.org/resource/{wikititle}"))
    rmlModel.subjectMap.addClass(rmlModel.rmlFactory.createRMLUri(templateMapping.mapToClass.uri))
    //rmlModel.subjectMap.addRdfsComment(subjectMapComment)
    //rmlModel.subjectMap.addRdfsLabel(subjectMapLabel)
    addExtraClassesToSubjectMap(rmlModel.subjectMap)
    rmlModel.subjectMap.addIRITermType()
    addCorrespondingPropertyAndClassToSubjectMap()
  }

  private def defineLogicalSource() =
  {
    rmlModel.logicalSource.addSource(rmlModel.rmlFactory.createRMLUri(rmlModel.sourceUri))
  }

  private def addPropertyMappings() =
  {
    val state = new MappingState
    val mapper = new RMLModelMapper(rmlModel)
    for(mapping <- templateMapping.mappings) {
      addPropertyMapping(mapper, mapping, state)
    }
  }

  private def addCorrespondingPropertyAndClassToSubjectMap() =
  {
    if(templateMapping.correspondingProperty != null) {
      val predicateObjectMap = rmlModel.triplesMap.addPredicateObjectMap(new RMLUri(rmlModel.wikiTitle.resourceIri + "/CorrespondingProperty"))
      predicateObjectMap.addPredicate(new RMLUri(templateMapping.correspondingProperty.uri))
      addCorrespondingClassToPredicateObjectMap(predicateObjectMap: RMLPredicateObjectMap)
    }
  }

  private def addCorrespondingClassToPredicateObjectMap(predicateObjectMap: RMLPredicateObjectMap) =
  {
    if(templateMapping.correspondingClass != null) {
      val objectMap = predicateObjectMap.addObjectMap(predicateObjectMap.uri.extend("/ObjectMap"))
      val parentTriplesMap = objectMap.addParentTriplesMap(objectMap.uri.extend("/ParentTriplesMap"))
      val subjectMap = parentTriplesMap.addSubjectMap(parentTriplesMap.uri.extend("/SubjectMap"))
      subjectMap.addClass(new RMLUri(templateMapping.correspondingClass.uri))
      parentTriplesMap.addLogicalSource(rmlModel.logicalSource)
    }
  }

  /**
    * Add related classes to the subject map
    *
    * @param subjectMap
    */
  private def addExtraClassesToSubjectMap(subjectMap: RMLSubjectMap) =
  {
    val relatedClasses = templateMapping.mapToClass.relatedClasses
    for(cls <- relatedClasses) {
      if(!cls.uri.contains("%3E")) {
        subjectMap.addClass(new RMLUri(cls.uri))
      }
    }
  }


  private def addPropertyMapping(mapper: RMLModelMapper, mapping: PropertyMapping, state: MappingState) =
  {
    mapper.addMapping(mapping, state)
  }

}

object TemplateRMLMapper {

  private var _simplePropertyCount = 0
  private var _startDateCount = 0
  private var _endDateCount = 0
  private var _latitudeCount = 0
  private var _longitudeCount = 0
  private var _constantCount = 0

  def simplePropertyCount : Int = {
    _simplePropertyCount
  }

  def startDateCount : Int = {
    _startDateCount
  }

  def endDateCount : Int = {
    _endDateCount
  }

  def latitudeCount : Int = {
    _latitudeCount
  }

  def longitudeCount : Int = {
    _longitudeCount
  }

  def constantCount : Int = {
    _constantCount
  }

  def resetState(): Unit = {
    _simplePropertyCount = 0
    _startDateCount = 0
    _endDateCount = 0
    _latitudeCount = 0
    _longitudeCount = 0
    _constantCount = 0
  }

  def increaseSimplePropertyCount(): Unit = {
    _simplePropertyCount += 1
  }

  def increaseStartDateCount() : Unit = {
    _startDateCount += 1
  }

  def increaseEndDateCount() : Unit = {
    _endDateCount += 1
  }

  def increaseLatitudeCount() : Unit = {
    _latitudeCount += 1
  }

  def increaseLongitudeCount() : Unit = {
    _longitudeCount += 1
  }

  def increaseConstantCount() : Unit = {
    _constantCount += 1
  }

}

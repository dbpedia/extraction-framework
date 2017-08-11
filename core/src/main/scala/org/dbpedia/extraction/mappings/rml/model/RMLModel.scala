package org.dbpedia.extraction.mappings.rml.model
import java.io.StringReader

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.dbpedia.extraction.mappings.rml.model.resource._
import org.dbpedia.extraction.mappings.rml.model.voc.Property
import org.dbpedia.extraction.ontology.RdfNamespace
import org.dbpedia.extraction.util.WikiUtil

import scala.collection.JavaConverters._



/**
  * Created by wmaroy on 21.07.17.
  */
class RMLModel(private val mapping : Model,
               val name : String,
               val base : String,
               val language : String) extends AbstractRMLModel {

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Initialization
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////


  // add the mapping to the core model, if null: this will be a fresh mapping
  if(mapping != null) {
    model.add(mapping)
  } else {
    val triplesMapResourceIRI = RMLUri(base.substring(0, base.lastIndexOf('/')))
    rmlFactory.createRMLTriplesMap(triplesMapResourceIRI)
  }

  override protected val _triplesMap: RMLTriplesMap = getMainTriplesMap
  override protected val _subjectMap: RMLSubjectMap = _triplesMap.addSubjectMap(RMLUri(base + "SubjectMap"))
  override protected val _logicalSource: RMLLogicalSource = _triplesMap.addLogicalSource(RMLUri(base + "LogicalSource"))
  override protected val _functionSubjectMap: RMLSubjectMap = rmlFactory.createRMLSubjectMap(RMLUri(base + "SubjectMap/Function"))
                                                                        .addClass(RMLUri(RdfNamespace.FNO.namespace + "Execution"))
                                                                        .addBlankNodeTermType()

  _subjectMap.addTemplate(rmlFactory.createRMLLiteral("http://"+ language +".dbpedia.org/resource/{wikititle}"))
  _subjectMap.addIRITermType()
  if(!_logicalSource.hasIterator) {
    _logicalSource.addIterator(RMLLiteral("Infobox:" + name))
  }
  _logicalSource.addReferenceFormulation(RMLUri(RdfNamespace.QL.namespace + "wikitext"))


  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Public instance methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////


  def addClass(classURI : String) = {
    _subjectMap.addClass(RMLUri(classURI))
  }

  def getMappedProperties : List[String] = {
    val references = model.listObjectsOfProperty(model.createProperty(Property.REFERENCE)).toList.asScala
    references.map(reference => reference.asLiteral().getString).toList
  }

  override def toString : String = {
    "RML Mapping:\n" +
      "Name: " + name + "\n" +
      "Language: " + language + "\n"
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Private methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  private def getMainTriplesMap : RMLTriplesMap = {
    val triplesMapResourceIRI = base.substring(0, base.lastIndexOf('/'))
    val triplesMap = model.getResource(triplesMapResourceIRI)
    rmlFactory.createRMLTriplesMap(RMLUri(triplesMap.getURI))
  }


}


object RMLModel {

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Apply method (factory method)
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
    *
    * @param language iso code of the language
    * @param templateName name of the infobox template
    * @param dump turtle dump of the RML Mapping
    * @return
    */
  def apply(language : String, templateName : String, dump: String) : RMLModel = {
    val mappingBase = createBase(templateName, language)
    val mappingName = createName(templateName, language)
    val mappingModel = ModelFactory.createDefaultModel().read(new StringReader(dump), mappingBase, "TURTLE")

    new RMLModel(mappingModel, mappingName, mappingBase, language)
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  //  Public static methods
  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  def createBase(templateTitle : String, language : String) : String = {
    "http://" + language + ".dbpedia.org/resource/" + createName(templateTitle, language) + "/"
  }

  /**
    * Creates a mapping name from a template title.
    * If this is already a valid mapping name (prefix = Mapping_{language}:{Template Name}, then this mapping will
    * just be returned.
    * @param templateTitle
    * @param language
    * @return
    */
  def createName(templateTitle : String, language : String) : String = {
    if(!templateTitle.contains("Mapping_")) {
      "Mapping_" + language + ":" + templateTitle
    } else templateTitle // this means a valid title is already given
  }

  def normalize(templateTitle : String, language : String) : String = {
    if(!templateTitle.contains("Mapping_")) {
      "Mapping_" + language + ":" + WikiUtil.wikiEncode(templateTitle)
    } else templateTitle // this means a valid title is already given
  }

}

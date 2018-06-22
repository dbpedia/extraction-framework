package org.dbpedia.extraction.mappings.rml.model.template.analyzing

import org.dbpedia.extraction.mappings.rml.util.{ContextCreator, RMLOntologyUtil}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology.{Ontology, OntologyClass, OntologyProperty}

/**
  * Created by wmaroy on 11.08.17.
  */
abstract class AbstractTemplateAnalyzer(ontology: Ontology) extends TemplateAnalyzer {

  protected def loadProperty(property: String): OntologyProperty = {
    if (isIri(property)) {
      RMLOntologyUtil.loadOntologyPropertyFromIRI(property, ContextCreator.createOntologyContext(ontology))
    } else RMLOntologyUtil.loadOntologyProperty(property, ContextCreator.createOntologyContext(ontology))
  }

  protected def loadDatatype(datatype: String): Datatype = {
    if (isIri(datatype)) {
      RMLOntologyUtil.loadOntologyDataTypeFromIRI(datatype, ContextCreator.createOntologyContext(ontology))
    } else RMLOntologyUtil.loadOntologyDataType(datatype, ContextCreator.createOntologyContext(ontology))
  }

  protected def loadClass(_class: String): OntologyClass = {
    if (isIri(_class)) {
      RMLOntologyUtil.loadOntologyClassFromIRI(_class, ContextCreator.createOntologyContext(ontology))
    } else RMLOntologyUtil.loadOntologyClass(_class, ContextCreator.createOntologyContext(ontology))
  }

  // quick check if given uri is a uri
  private def isIri(s: String): Boolean = {
    val pattern = "(.*[/#])([^/#]+)".r
    pattern.findFirstIn(s).isDefined
  }

}

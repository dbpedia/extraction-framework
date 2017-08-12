package org.dbpedia.extraction.mappings.rml.model.template.analyzing

import org.dbpedia.extraction.mappings.rml.util.{ContextCreator, RMLOntologyUtil}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology.{Ontology, OntologyClass, OntologyProperty}

/**
  * Created by wmaroy on 11.08.17.
  */
abstract class AbstractTemplateAnalyzer(ontology: Ontology) extends TemplateAnalyzer {

  protected def loadProperty(property : String) : OntologyProperty = {
    RMLOntologyUtil.loadOntologyPropertyFromIRI(property, ContextCreator.createOntologyContext(ontology))
  }

  protected def loadDatatype(datatype : String) : Datatype = {
    RMLOntologyUtil.loadOntologyDataTypeFromIRI(datatype, ContextCreator.createOntologyContext(ontology))
  }

  protected def loadClass(_class : String) : OntologyClass = {
    RMLOntologyUtil.loadOntologyClassFromIRI(_class, ContextCreator.createOntologyContext(ontology))
  }

}

package org.dbpedia.extraction.mappings.rml.model.factory
import com.fasterxml.jackson.databind.JsonNode
import org.dbpedia.extraction.mappings.rml.model.template.SimplePropertyTemplate
import org.dbpedia.extraction.mappings.rml.util.{ContextCreator, JSONFactoryUtil, RMLOntologyUtil}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology.{Ontology, OntologyProperty, RdfNamespace}

/**
  * Created by wmaroy on 24.07.17.
  */
class SimplePropertyTemplateJSONFactory(val templateNode: JsonNode, ontology: Ontology) extends TemplateFactory {

  private lazy val context = ContextCreator.createOntologyContext(ontology)

  private lazy val property = JSONFactoryUtil.parameters("property", templateNode)
  private lazy val ontologyProperty = JSONFactoryUtil.getOntologyProperty(templateNode, context)
  private lazy val select = JSONFactoryUtil.parameters("select", templateNode)
  private lazy val prefix = JSONFactoryUtil.parameters("prefix", templateNode)
  private lazy val suffix = JSONFactoryUtil.parameters("suffix", templateNode)
  private lazy val transform = JSONFactoryUtil.parameters("transform", templateNode)
  private lazy val unit = JSONFactoryUtil.getUnit(templateNode, ontology)
  private lazy val factor = JSONFactoryUtil.parameters("factor", templateNode)

  override def createTemplate: SimplePropertyTemplate = {
    val doubleFactor = if(factor == null || factor.equals("null")) 1.0 else factor.toDouble
    new SimplePropertyTemplate(property, ontologyProperty, select, prefix, suffix, transform, unit, doubleFactor)
  }






}

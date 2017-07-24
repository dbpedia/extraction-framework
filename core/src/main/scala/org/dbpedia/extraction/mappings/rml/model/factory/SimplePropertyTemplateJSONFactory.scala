package org.dbpedia.extraction.mappings.rml.model.factory
import com.fasterxml.jackson.databind.JsonNode
import org.dbpedia.extraction.mappings.rml.model.template.SimplePropertyTemplate
import org.dbpedia.extraction.mappings.rml.util.{ContextCreator, RMLOntologyUtil}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology.{Ontology, OntologyProperty, RdfNamespace}

/**
  * Created by wmaroy on 24.07.17.
  */
class SimplePropertyTemplateJSONFactory(val templateNode: JsonNode, ontology: Ontology) extends SimplePropertyTemplateFactory {

  private lazy val property = parameters("property")
  private lazy val ontologyProperty = getOntologyProperty
  private lazy val select = parameters("select")
  private lazy val prefix = parameters("prefix")
  private lazy val suffix = parameters("suffix")
  private lazy val transform = parameters("transform")
  private lazy val unit = getUnit
  private lazy val factor = parameters("factor")

  private lazy val context = ContextCreator.createOntologyContext(ontology)

  override def createTemplate: SimplePropertyTemplate = {
    val doubleFactor = if(factor == null || factor.equals("null")) 1.0 else factor.toDouble
    new SimplePropertyTemplate(property, ontologyProperty, select, prefix, suffix, transform, unit, doubleFactor)
  }

  private def parameters(key : String) : String = {
    val text = templateNode.get("parameters").get(key).asText()
    if(text.equals("null")) null else text
  }

  private def getOntologyProperty : OntologyProperty = {
    val ontologyPropertyParameter = parameters("ontologyProperty")
    val prefix = extractPrefix(ontologyPropertyParameter)
    val localName = extractLocalName(ontologyPropertyParameter)

    if(RdfNamespace.prefixMap.contains(prefix)) {
      val ontologyPropertyIRI = RdfNamespace.prefixMap(prefix).namespace + localName
      RMLOntologyUtil.loadOntologyPropertyFromIRI(ontologyPropertyIRI, context)
    } else {
      RMLOntologyUtil.loadOntologyProperty(ontologyPropertyParameter, context)
    }

  }

  private def getUnit : Datatype = {
    val unitName = parameters("unit")
    RMLOntologyUtil.loadOntologyDataType(unitName, context)
  }

  private def extractPrefix(s: String) : String = {
    val prefixPattern = "^[^:]*".r
    prefixPattern.findFirstIn(s).orNull
  }

  private def extractLocalName(s: String) : String = {
    val localNamePattern = "[^:]*$".r
    localNamePattern.findFirstIn(s).orNull
  }


}

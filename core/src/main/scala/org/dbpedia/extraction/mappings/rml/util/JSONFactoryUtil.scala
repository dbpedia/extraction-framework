package org.dbpedia.extraction.mappings.rml.util

import com.fasterxml.jackson.databind.JsonNode
import org.dbpedia.extraction.mappings.rml.exception.{OntologyClassException, OntologyPropertyException, TemplateFactoryBundleException}
import org.dbpedia.extraction.mappings.rml.model.factory.{JSONBundle, TemplateFactoryBundle}
import org.dbpedia.extraction.ontology.datatypes.Datatype
import org.dbpedia.extraction.ontology.{Ontology, OntologyClass, OntologyProperty, RdfNamespace}

import scala.collection.JavaConverters._

/**
  * Created by wmaroy on 25.07.17.
  */
object JSONFactoryUtil {

  def get(key : String, node: JsonNode) : String = {
    val text = node.get(key).asText()
    if(text.equals("null")) null else text
  }

  def parameters(key : String, templateNode: JsonNode) : String = {
    val text = templateNode.get("parameters").get(key).asText()
    if(text.equals("null")) null else text
  }

  def parametersNode(key : String, templateNode: JsonNode) : JsonNode = {
    templateNode.get("parameters").get(key)
  }

  def hasParameter(key : String, templateNode : JsonNode) : Boolean = {
    val parameterNode = templateNode.get("parameters")
    parameterNode.has(key) && parameterNode.hasNonNull(key)
  }

  def jsonNodeToSeq(listNode : JsonNode) : Seq[JsonNode] = {
    if(listNode.isArray) listNode.iterator().asScala.toSeq
    else throw new IllegalArgumentException("Json Node is not an array.")
  }

  def getOntologyProperty(templateNode: JsonNode, ontology: Ontology) : OntologyProperty = {
    val context = ContextCreator.createOntologyContext(ontology)
    getOntologyProperty(templateNode, context)
  }

  def getUnit(templateNode: JsonNode, ontology: Ontology) : Datatype = {
    val context = ContextCreator.createOntologyContext(ontology)
    getUnit(templateNode, context)
  }

  def getUnit(templateNode: JsonNode, context : {def ontology: Ontology}) : Datatype = {
    val unitName = JSONFactoryUtil.parameters("unit", templateNode)
    RMLOntologyUtil.loadOntologyDataType(unitName, context)
  }

  def getOntologyProperty(templateNode: JsonNode, context : {def ontology: Ontology}) : OntologyProperty = {

    val ontologyPropertyParameter = JSONFactoryUtil.parameters("ontologyProperty", templateNode)
    val prefix = extractPrefix(ontologyPropertyParameter)
    val localName = extractLocalName(ontologyPropertyParameter)

    // load the property
    val result = if(RdfNamespace.prefixMap.contains(prefix)) {
      val ontologyPropertyIRI = RdfNamespace.prefixMap(prefix).namespace + localName
      RMLOntologyUtil.loadOntologyPropertyFromIRI(ontologyPropertyIRI, context)
    } else {
      try {
        RMLOntologyUtil.loadOntologyProperty(ontologyPropertyParameter, context)
      } catch {
        case e : Exception => null
      }
    }

    // throw exception if not found
    if(result == null) {
      throw new OntologyPropertyException("Ontology Property cannot be found in current ontology: " + ontologyPropertyParameter)
    } else {
      result
    }

  }

  def getOntologyClass(ontologyClass : String, ontology: Ontology) : OntologyClass = {
    val context = ContextCreator.createOntologyContext(ontology)

    val prefix = extractPrefix(ontologyClass)
    val localName = extractLocalName(ontologyClass)

    // load the property
    val result = if(RdfNamespace.prefixMap.contains(prefix)) {
      val ontologyPropertyIRI = RdfNamespace.prefixMap(prefix).namespace + localName
      RMLOntologyUtil.loadOntologyClassFromIRI(ontologyPropertyIRI, context)
    } else {
      try {
        RMLOntologyUtil.loadOntologyClass(ontologyClass, context)
      } catch {
        case e : Exception => null
      }
    }

    // throw exception if not found
    if(result == null) {
      throw new OntologyClassException("Ontology Class cannot be found in current ontology: " + ontologyClass)
    } else {
      result
    }
  }

  /**
    * Returns the right instance of the bundle. If not correct, throw an exception
    *
    * @param bundle
    * @return
    */
  def getBundle(bundle : TemplateFactoryBundle) : JSONBundle = {
    if(!bundle.isInstanceOf[JSONBundle]) {
      throw new TemplateFactoryBundleException(TemplateFactoryBundleException.WRONG_BUNDLE_MSG)
    } else {
      bundle.asInstanceOf[JSONBundle]
    }
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

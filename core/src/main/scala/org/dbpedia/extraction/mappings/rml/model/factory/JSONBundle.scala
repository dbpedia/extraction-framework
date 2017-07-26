package org.dbpedia.extraction.mappings.rml.model.factory

import com.fasterxml.jackson.databind.JsonNode
import org.dbpedia.extraction.ontology.Ontology

/**
  * Created by wmaroy on 26.07.17.
  */
case class JSONBundle(templateNode : JsonNode, ontology: Ontology) extends TemplateFactoryBundle

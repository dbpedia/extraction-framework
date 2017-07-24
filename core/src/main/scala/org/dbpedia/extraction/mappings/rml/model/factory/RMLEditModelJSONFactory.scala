package org.dbpedia.extraction.mappings.rml.model.factory

import java.io.StringReader
import java.net.URLEncoder

import com.fasterxml.jackson.databind.JsonNode
import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.dbpedia.extraction.mappings.rml.model.RMLEditModel

/**
  * Created by wmaroy on 21.07.17.
  */
class RMLEditModelJSONFactory(mappingNode: JsonNode) {

  private lazy val dump = mappingNode.get("dump").asText()
  private lazy val name = mappingNode.get("name").asText()
  private lazy val language = mappingNode.get("language").asText()
  private lazy val base = "http://" + language + ".dbpedia.org/resource/Mapping_" + language + ":" + name
  private lazy val model = ModelFactory.createDefaultModel().read(new StringReader(dump), base, "TURTLE")

  def create : RMLEditModel = {
    new RMLEditModel(model, name, base, language)
  }


}

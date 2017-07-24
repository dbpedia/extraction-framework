package org.dbpedia.extraction.mappings.rml.model.factory

import org.apache.jena.rdf.model.{Model, ModelFactory}
import org.dbpedia.extraction.mappings.rml.model.RMLEditModel

/**
  * Created by wmaroy on 21.07.17.
  */
class RMLEditModelFactory {

  def create(mapping : String) : RMLEditModel = {
    val model = ModelFactory.createDefaultModel().read(mapping)
    create(model)
  }

  private def create(model : Model) : RMLEditModel = {
    new RMLEditModel(model)
  }

}
